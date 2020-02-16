package com.lsd.eshouse.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lsd.eshouse.common.constant.RentValueRangeBlock;
import com.lsd.eshouse.common.dto.BaiduMapLocation;
import com.lsd.eshouse.common.dto.HouseBucketDTO;
import com.lsd.eshouse.common.dto.HouseSuggest;
import com.lsd.eshouse.common.form.MapSearchForm;
import com.lsd.eshouse.common.form.RentSearchForm;
import com.lsd.eshouse.common.index.HouseIndex;
import com.lsd.eshouse.common.index.HouseIndexKey;
import com.lsd.eshouse.entity.SupportAddress;
import com.lsd.eshouse.msg.dto.HouseIndexMessage;
import com.lsd.eshouse.common.utils.HouseSortUtil;
import com.lsd.eshouse.common.vo.MultiResultVo;
import com.lsd.eshouse.common.vo.ResultVo;
import com.lsd.eshouse.entity.House;
import com.lsd.eshouse.entity.HouseTag;
import com.lsd.eshouse.msg.listener.MessageListener;
import com.lsd.eshouse.repository.HouseDetailRepository;
import com.lsd.eshouse.repository.HouseRepository;
import com.lsd.eshouse.repository.HouseTagRepository;
import com.lsd.eshouse.repository.SupportAddressRepository;
import com.lsd.eshouse.service.AddressService;
import com.lsd.eshouse.service.BaiduLBSService;
import com.lsd.eshouse.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.replication.ReplicationResponse;
import org.elasticsearch.client.*;
import org.elasticsearch.client.indices.AnalyzeRequest;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ES搜索服务
 * <p>
 * Created by lsd
 * 2020-01-28 22:16
 */
@Slf4j
@Service
public class SearchServiceImpl implements SearchService {
    private static final String INDEX_NAME = "house";
    private static final String IK_SMART = "ik_smart";
    @Autowired
    private HouseRepository houseRepository;
    @Autowired
    private HouseDetailRepository houseDetailRepository;
    @Autowired
    private HouseTagRepository houseTagRepository;
    @Autowired
    private SupportAddressRepository supportAddressRepository;
    @Autowired
    private AddressService addressService;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private RestClient restClient;
    @Autowired
    private RestHighLevelClient rhlClient;
    @Autowired
    private Gson gson;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private BaiduLBSService baiduLBSService;
    @Value("${eshouse.elasticsearch.max-retry:3}")
    private Integer maxReTry;
    @Value("${eshouse.elasticsearch.max-suggest:5}")
    private Integer maxSuggest;


    @Override
    public void doIndex(Integer houseId, int retry) {
        // 查询house
        Optional<House> houseOpt = houseRepository.findById(houseId);
        if (houseOpt.isEmpty()) {
            // 发消息进行重试，消息重试次数+1
            retryIndex(houseId, retry + 1, "房源不存在");
            return;
        }
        final House house = houseOpt.get();
        // 查询house detail
        final var houseDetail = houseDetailRepository.findByHouseId(houseId);
        if (houseDetail == null) {
            // 发消息进行重试，消息重试次数+1
            retryIndex(houseId, retry + 1, "房源详细信息不存在");
            return;
        }
        // 查询house地址信息，并调用百度地图API获取地址的经纬度
        SupportAddress city = supportAddressRepository.findByEnNameAndLevel(house.getCityEnName(), SupportAddress.Level.CITY.getValue());
        SupportAddress region = supportAddressRepository.findByEnNameAndLevel(house.getRegionEnName(), SupportAddress.Level.REGION.getValue());
        String address = city.getCnName() + region.getCnName() + house.getStreet() + house.getDistrict() + houseDetail.getAddress();
        ResultVo<BaiduMapLocation> locationResultVo = addressService.getBaiduMapLocation(city.getCnName(), address);
        if (!locationResultVo.isSuccess()) {
            retryIndex(houseId, retry + 1, "查询house地址信息失败");
            return;
        }
        // 查询house tags
        List<HouseTag> houseTags = houseTagRepository.findAllByHouseId(houseId);
        // 构建索引对象
        HouseIndex houseIndex = modelMapper.map(house, HouseIndex.class);
        modelMapper.map(houseDetail, houseIndex);
        if (!CollectionUtils.isEmpty(houseTags)) {
            houseIndex.setTags(houseTags.stream().map(HouseTag::getName).collect(Collectors.toList()));
        }
        houseIndex.setLocation(locationResultVo.getResult());
        houseIndex.setHouseId(houseId);

        // 索引前先term查询索引是否存在
        var sourceBuilder = new SearchSourceBuilder().query(QueryBuilders.termQuery(HouseIndexKey.HOUSE_ID, houseId));
        var searchRequest = new SearchRequest(INDEX_NAME).source(sourceBuilder);
        boolean indexSuccess = false;
        try {
            final var response = rhlClient.search(searchRequest, RequestOptions.DEFAULT);
            final var totalHits = response.getHits().getTotalHits().value;
            // 索引不存在，则创建索引
            if (totalHits == 0) {
                indexSuccess = this.createIndex(houseIndex);
            } else if (totalHits == 1) {  //已存在则更新索引
                var documentId = response.getHits().getAt(0).getId();
                indexSuccess = this.createUpdateIndex(documentId, houseIndex);
            } else {  //其他情况先删除再新增索引
                indexSuccess = deleteAndCreateIndex(houseIndex);
            }
        } catch (Exception e) {
            log.error("房源索引失败，houseId = " + houseId, e);
        }
        // 上传百度地图LBS.云的POI数据
        var uploadLBSResult = baiduLBSService.upload(locationResultVo.getResult(), house.getStreet() + house.getDistrict(), city.getCnName() + region.getCnName() + house.getStreet() + house.getDistrict(), houseId, house.getPrice(), house.getArea());
        // 两者都成功才算成功
        if (indexSuccess && uploadLBSResult.isSuccess()) {
            log.debug("房源索引成功 && POI数据上传成功，houseId = " + houseId);
            return;
        }
        // 发消息进行重试，消息重试次数+1
        retryIndex(houseId, retry + 1, "索引操作失败或POI数据上传失败");
    }

    /**
     * 先删除再索引
     */
    private boolean deleteAndCreateIndex(HouseIndex houseIndex) {
        //根据houseId精确filter查询
        final var houseId = houseIndex.getHouseId();
        BoolQueryBuilder qb = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termQuery(HouseIndexKey.HOUSE_ID, houseId));
        var request = new DeleteByQueryRequest(INDEX_NAME).setQuery(qb);
        try {
            var response = rhlClient.deleteByQuery(request, RequestOptions.DEFAULT);
            final long totalHits = response.getTotal();      // 查到的文档数
            final long deletedDocs = response.getDeleted();  // 删除的文档数
            if (deletedDocs != totalHits) {
                log.warn("已删除的数目 < 需要删除的数目，需要删除的数目：{}，已删除的数目：{}", totalHits, deletedDocs);
                return false;
            }
        } catch (Exception e) {
            log.error("删除索引出错，houseId = " + houseId, e);
            return false;
        }
        return this.createIndex(houseIndex);
    }

    @Override
    public void index(Integer houseId) {
        this.indexAsync(houseId, 0);
    }

    /**
     * 发消息到 kafka 进行索引构建
     *
     * @param retry 将要进行第几次重试
     */
    private void indexAsync(Integer houseId, int retry) {
        final var houseIndexMessage = new HouseIndexMessage(houseId, HouseIndexMessage.IndexOperation.INDEX.getName(), retry);
        kafkaTemplate.send(MessageListener.INDEX_TOPIC, gson.toJson(houseIndexMessage));
    }

    /**
     * 发消息到 kafka 进行重试索引构建
     *
     * @param retry          将要进行第几次重试
     * @param reasonIfFailed 重试失败的提示信息
     */
    private void retryIndex(Integer houseId, int retry, String reasonIfFailed) {
        if (retry > maxReTry) {
            log.error("超过索引最大重试次数，索引失败，原因：" + reasonIfFailed + "，houseId = {}", houseId);
            return;
        }
        this.indexAsync(houseId, retry);
    }

    /**
     * 发消息到 kafka 进行重试索引删除
     *
     * @param retry 将要进行第几次重试
     */
    private void retryRemoveIndex(Integer houseId, int retry) {
        if (retry > maxReTry) {
            log.error("超过索引最大重试次数，索引删除失败，原因：已删除的数目 < 需要删除的数目，houseId = {}", houseId);
            return;
        }
        this.removeAsync(houseId, retry);
    }

    /**
     * 新增索引，使用低级restClient
     */
    private boolean createIndex(HouseIndex index) {
        // 分析索引数据并放入到索引的自动补全关键词列表
        if (!analyzeSuggestion(index)) {
            return false;
        }
        try {
            final Request request = new Request(HttpPost.METHOD_NAME, "/" + INDEX_NAME + "/_doc");
            final String indexJson = objectMapper.writeValueAsString(index);
            request.setJsonEntity(indexJson);
            log.debug(indexJson);
            Response response = restClient.performRequest(request);
            // 是否创建成功
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_CREATED) {
                return true;
            }
            // 记录失败信息
            log.error("houseId = " + index.getHouseId() + ",返回体：{}", EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            log.error("创建房源索引失败，houseId = " + index.getHouseId(), e);
        }
        return false;
    }

    /**
     * 新增或更新索引，使用高级restClient
     *
     * @param documentId 新增传null或者空字符串，更新传索引文档id
     */
    private boolean createUpdateIndex(String documentId, HouseIndex index) {
        // 分析索引数据并放入到索引的自动补全关键词列表
        if (!analyzeSuggestion(index)) {
            return false;
        }
        // 构造索引请求，source不能是json串了应该传Map<String,Object>类型，而且通过opType限定操作类型
        DocWriteRequest.OpType opType = StringUtils.isBlank(documentId) ?
                DocWriteRequest.OpType.CREATE : DocWriteRequest.OpType.INDEX;

        Map<String, Object> map;
        try {
            map = objectMapper.readValue(objectMapper.writeValueAsString(index), new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.error("更新房源索引出错，序列化错误，houseId = " + index.getHouseId(), e);
            return false;
        }
        final var request = new IndexRequest(INDEX_NAME)
                .id(documentId)
                .source(map)
                .opType(opType);
        log.debug(request.toString());
        try {
            IndexResponse response = rhlClient.index(request, RequestOptions.DEFAULT);
            //处理首次创建文档的情况
            if (response.getResult() == DocWriteResponse.Result.CREATED) {
                log.debug("创建房源索引成功，houseId = " + index.getHouseId());
                return true;
            } else if (response.getResult() == DocWriteResponse.Result.UPDATED) { //处理文档已经存在时被覆盖的情况
                log.debug("更新房源索引成功，houseId = " + index.getHouseId());
                return true;
            }
            // 失败处理
            return dealWithReplicationResponse(index.getHouseId(), response);
        } catch (ElasticsearchException e) {
            //表示是由于返回了版本冲突错误引发的异常
            if (e.status() == RestStatus.CONFLICT) {
                log.error("更新房源索引出错，houseId = " + index.getHouseId() + "，原因：版本冲突错误，请检查是否opType设置错误", e);
            } else {
                log.error("更新房源索引出错，houseId = " + index.getHouseId(), e);
            }
        } catch (Exception e) {
            log.error("更新房源索引出错，houseId = " + index.getHouseId(), e);
        }
        return false;
    }

    /**
     * 根据文档id删除文档
     */
    private boolean deleteIndexByDocumentId(String documentId) {
        DeleteRequest request = new DeleteRequest(INDEX_NAME).id(documentId);
        try {
            final DeleteResponse response = rhlClient.delete(request, RequestOptions.DEFAULT);
            if (response.getResult() == DocWriteResponse.Result.DELETED) {
                return true;
            }
            // 错误处理
            ReplicationResponse.ShardInfo shardInfo = response.getShardInfo();
            //处理成功的分片数少于总分片的情况
            if (shardInfo.getTotal() != shardInfo.getSuccessful()) {
                log.warn("删除索引成功的分片数少于总分片，documentId = " + documentId);
                return false;
            }
            //处理潜在的故障
            if (shardInfo.getFailed() > 0) {
                String reasonStr = Arrays.stream(shardInfo.getFailures())
                        .map(ReplicationResponse.ShardInfo.Failure::reason)
                        .collect(Collectors.joining(","));
                log.warn("删除索引出错，documentId = " + documentId + "，原因：" + reasonStr);
            }
        } catch (ElasticsearchException e) {
            if (e.status() == RestStatus.NOT_FOUND) {       //文档不存在
                return true;
            } else if (e.status() == RestStatus.CONFLICT) { //表示是由于返回了版本冲突错误引发的异常
                log.error("删除索引出错，documentId = " + documentId + "，原因：版本冲突错误", e);
            } else {
                log.error("删除索引出错，documentId = " + documentId, e);
            }
        } catch (Exception e) {
            log.error("删除索引出错，documentId = " + documentId, e);
        }
        return false;
    }


    public void doRemove(Integer houseId, int retry) {
        //根据houseId精确filter查询
        BoolQueryBuilder qb = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termQuery(HouseIndexKey.HOUSE_ID, houseId));
        var request = new DeleteByQueryRequest(INDEX_NAME).setQuery(qb);
        log.debug(qb.toString());
        try {
            var response = rhlClient.deleteByQuery(request, RequestOptions.DEFAULT);
            final long totalHits = response.getTotal();      // 查到的文档数
            final long deletedDocs = response.getDeleted();  // 删除的文档数
            if (deletedDocs != totalHits) {
                log.warn("已删除的数目 < 需要删除的数目，需要删除的数目：{}，已删除的数目：{}", totalHits, deletedDocs);
                retryRemoveIndex(houseId, retry + 1);
            }
            // 删除百度地图LBS.云的POI数据
            final ResultVo removeLBSResult = baiduLBSService.remove(houseId);
            if (!removeLBSResult.isSuccess()) {
                log.warn("删除百度地图LBS.云的POI数据失败");
                retryRemoveIndex(houseId, retry + 1);
            }
            log.debug("删除索引成功  && POI数据删除成功 ，houseId = " + houseId);
        } catch (Exception e) {
            log.error("删除索引出错，houseId = " + houseId, e);
        }
    }

    @Override
    public void remove(Integer houseId) {
        this.removeAsync(houseId, 0);
    }

    @Override
    public MultiResultVo<Integer> search(RentSearchForm form) {
        // 城市、地区等筛选栏使用filterQuery
        final var boolQB = new BoolQueryBuilder().filter(new TermQueryBuilder(HouseIndexKey.CITY_EN_NAME, form.getCityEnName()));
        final String regionEnName = form.getRegionEnName();
        if (StringUtils.isNotBlank(regionEnName) && !StringUtils.equals(regionEnName, "*")) {
            boolQB.filter(new TermQueryBuilder(HouseIndexKey.REGION_EN_NAME, regionEnName));
        }
        // keywords使用multiQuery
        // 设置每个域的评分权重
        boolQB.should(
                QueryBuilders.matchQuery(HouseIndexKey.TITLE, form.getKeywords())
                        .boost(2.0f)
        ).should(
                QueryBuilders.multiMatchQuery(form.getKeywords(),
                        HouseIndexKey.TITLE,
                        HouseIndexKey.TRAFFIC,
                        HouseIndexKey.DISTRICT,
                        HouseIndexKey.ROUND_SERVICE,
                        HouseIndexKey.SUBWAY_LINE_NAME,
                        HouseIndexKey.SUBWAY_STATION_NAME)
        );
        // 面积、租金使用rangeQuery
        final var areaRange = RentValueRangeBlock.matchArea(form.getAreaBlock());
        final var priceRange = RentValueRangeBlock.matchPrice(form.getPriceBlock());
        if (!RentValueRangeBlock.ALL.equals(areaRange)) {
            final var rangeQB = QueryBuilders.rangeQuery(HouseIndexKey.AREA);
            if (areaRange.getMin() > 0) {
                rangeQB.gte(areaRange.getMin());
            }
            if (areaRange.getMax() > 0) {
                rangeQB.lte(areaRange.getMax());
            }
            boolQB.filter(rangeQB);
        }
        if (!RentValueRangeBlock.ALL.equals(priceRange)) {
            final var rangeQB = QueryBuilders.rangeQuery(HouseIndexKey.PRICE);
            if (priceRange.getMin() > 0) {
                rangeQB.gte(priceRange.getMin());
            }
            if (priceRange.getMax() > 0) {
                rangeQB.lte(priceRange.getMax());
            }
            boolQB.filter(rangeQB);
        }
        //朝向
        if (form.getDirection() > 0) {
            boolQB.filter(QueryBuilders.termQuery(HouseIndexKey.DIRECTION, form.getDirection()));
        }
        //租赁方式
        if (form.getRentWay() > -1) {
            boolQB.filter(QueryBuilders.termQuery(HouseIndexKey.RENT_WAY, form.getRentWay()));
        }
        //执行搜索
        return this.searchForHouseIds(form.getStart(), form.getSize(), form.getOrderBy(), form.getOrderDirection(), boolQB);
    }

    /**
     * 搜索房源ids
     *
     * @param boolQB 构造好的 BoolQueryBuilder
     * @return 条件匹配的房源ids
     */
    private MultiResultVo<Integer> searchForHouseIds(int start, int size, String orderBy, String orderDirection, BoolQueryBuilder boolQB) {
        final var sourceBuilder = new SearchSourceBuilder()
                .query(boolQB)
                .fetchSource(HouseIndexKey.HOUSE_ID, null)  //只查出houseId，避免其他无用数据浪费性能
                .sort(HouseSortUtil.getSortKey(orderBy), SortOrder.fromString(orderDirection))
                .from(start)
                .size(size);
        final var searchRequest = new SearchRequest(INDEX_NAME).source(sourceBuilder);
        log.debug(searchRequest.toString());
        try {
            final var response = rhlClient.search(searchRequest, RequestOptions.DEFAULT);
            if (response.status() != RestStatus.OK) {
                log.error("搜索查询失败，searchRequest = {}", searchRequest.toString());
                return new MultiResultVo<>(0, List.of());
            }
            final var houseIds = Arrays.stream(response.getHits().getHits()).map(hit ->
                    Integer.parseInt(String.valueOf(hit.getSourceAsMap().get(HouseIndexKey.HOUSE_ID)))
            ).collect(Collectors.toList());
            return new MultiResultVo<>(response.getHits().getTotalHits().value, houseIds);
        } catch (Exception e) {
            log.error("搜索查询失败", e);
        }
        return new MultiResultVo<>(0, List.of());
    }

    @Override
    public ResultVo<List<String>> suggest(String prefix) {
        //参照:https://www.elastic.co/guide/en/elasticsearch/reference/6.6/search-suggesters-completion.html
        var completionSuggestion = SuggestBuilders.completionSuggestion(HouseIndexKey.SUGGESTION)
                .prefix(prefix)         //提供给suggest analyzer分析的需要补全的前缀
                .size(5)                //每个建议文本项最多可返回的建议词个数，默认值是5
                .skipDuplicates(false); //是否从结果中过滤掉来自不同文档的重复建议词，开启后会减慢搜索速度，因为需要遍历更多的建议词选出topN，下边已使用set去重
        final var sourceBuilder = new SearchSourceBuilder().suggest(
                new SuggestBuilder().addSuggestion("autocomplete", completionSuggestion)
        );
        var searchRequest = new SearchRequest(INDEX_NAME).source(sourceBuilder);
        log.debug(searchRequest.toString());
        try {
            final SearchResponse response = rhlClient.search(searchRequest, RequestOptions.DEFAULT);
            // 最终获取5个补全建议关键字结果（做去重处理）
            final Set<String> suggestionSet = new HashSet<>();
            response.getSuggest().getSuggestion("autocomplete")
                    .getEntries()
                    .stream()
                    .filter(entry ->
                            entry instanceof CompletionSuggestion.Entry && !entry.getOptions().isEmpty()
                    ).map(entry ->
                    ((CompletionSuggestion.Entry) entry).getOptions()
            ).forEach(options -> {
                if (suggestionSet.size() > maxSuggest) {
                    return;
                }
                for (CompletionSuggestion.Entry.Option option : options) {
                    if (suggestionSet.size() > maxSuggest) {
                        break;
                    }
                    suggestionSet.add(option.getText().string());
                }
            });
            List<String> suggestionList = Lists.newArrayList(suggestionSet.toArray(new String[0]));
            return ResultVo.of(suggestionList);
        } catch (Exception e) {
            log.error("获取补全建议关键词失败", e);
            return ResultVo.of(List.of());
        }
    }

    @Override
    public ResultVo<Long> aggregateDistrictHouse(String cityEnName, String regionEnName, String district) {
        var boolQuery = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termQuery(HouseIndexKey.CITY_EN_NAME, cityEnName))
                .filter(QueryBuilders.termQuery(HouseIndexKey.REGION_EN_NAME, regionEnName))
                .filter(QueryBuilders.termQuery(HouseIndexKey.DISTRICT, district));
        // 先boolQuery再根据小区名进行aggregation
        // the terms aggregation will return the buckets for the top ten terms ordered by the doc_count.
        // One can change this default behaviour by setting the size parameter.
        var sourceBuilder = new SearchSourceBuilder()
                .query(boolQuery)
                .aggregation(
                        AggregationBuilders.terms(HouseIndexKey.AGG_DISTRICT)
                                .field(HouseIndexKey.DISTRICT)
                );
        SearchRequest searchReq = new SearchRequest().source(sourceBuilder);
        log.debug(searchReq.toString());
        try {
            final var response = rhlClient.search(searchReq, RequestOptions.DEFAULT);
            if (response.status() != RestStatus.OK) {
                log.error("聚合特定小区的房源数查询失败，searchRequest = {}", searchReq.toString());
                return ResultVo.of(0L);
            }
            Aggregations aggregations = response.getAggregations();
            // the list of the top buckets, the meaning of top being defined by the order
            Terms buckets = aggregations.get(HouseIndexKey.AGG_DISTRICT);
            // 获取当前小区的桶的文档数量
            Long docCount = Optional.ofNullable(buckets.getBucketByKey(district))
                    .map(Terms.Bucket::getDocCount)
                    .orElse(0L);
            return ResultVo.of(docCount);
        } catch (Exception e) {
            log.error("聚合特定小区的房源数查询失败", e);
        }
        return ResultVo.of(0L);
    }

    @Override
    public MultiResultVo<HouseBucketDTO> mapAggregateByCity(String cityEnName) {
        // 先根据城市filter
        var queryBuilder = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termQuery(HouseIndexKey.CITY_EN_NAME, cityEnName));
        var sourceBuilder = new SearchSourceBuilder()
                .query(queryBuilder)
                .aggregation(
                        AggregationBuilders.terms(HouseIndexKey.AGG_REGION)     //聚合名称
                                .field(HouseIndexKey.REGION_EN_NAME)            //根据地区聚合
                );
        var searchReq = new SearchRequest(INDEX_NAME).source(sourceBuilder);
        log.debug(searchReq.toString());
        try {
            final var response = rhlClient.search(searchReq, RequestOptions.DEFAULT);
            if (response.status() != RestStatus.OK) {
                log.error("地图页面城市信息聚合失败,searchRequest = {}", searchReq.toString());
                return new MultiResultVo<>(0, List.of());
            }
            // 聚合信息转为DTO返回
            Terms terms = response.getAggregations().get(HouseIndexKey.AGG_REGION);
            List<HouseBucketDTO> bucketDTOList = terms.getBuckets().stream().map(buck ->
                    new HouseBucketDTO(buck.getKeyAsString(), buck.getDocCount())
            ).collect(Collectors.toList());
            return new MultiResultVo<>(response.getHits().getTotalHits().value, bucketDTOList);
        } catch (Exception e) {
            log.error("地图页面城市信息聚合失败", e);
            return new MultiResultVo<>(0, List.of());
        }
    }

    @Override
    public MultiResultVo<Integer> mapSearchByCity(MapSearchForm form) {
        var boolQB = QueryBuilders.boolQuery().filter(QueryBuilders.termQuery(HouseIndexKey.CITY_EN_NAME, form.getCityEnName()));
        return this.searchForHouseIds(form.getStart(), form.getSize(), form.getOrderBy(), form.getOrderDirection(), boolQB);
    }

    @Override
    public MultiResultVo<Integer> mapSearchByBound(MapSearchForm form) {
        // 根据精确范围经纬度搜索
        var boundingBoxQB = QueryBuilders.geoBoundingBoxQuery(HouseIndexKey.LOCATION)
                .setCorners(
                        new GeoPoint(form.getLeftLatitude(), form.getLeftLongitude()),
                        new GeoPoint(form.getRightLatitude(), form.getRightLongitude())
                );
        var boolQB = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termQuery(HouseIndexKey.CITY_EN_NAME, form.getCityEnName()))
                .filter(boundingBoxQB);
        return this.searchForHouseIds(form.getStart(), form.getSize(), form.getOrderBy(), form.getOrderDirection(), boolQB);
    }

    /**
     * 使用 ik_smart Tokenizer + TokenFilter 对索引数据进行词条分析(分词)，并把全部词条(term)加入到索引的 补全建议关键词 列表
     * <p>
     * 过滤器构造参照:https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/java-rest-high-analyze.html
     * 过滤器类型参照:https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-tokenfilters.html
     */
    private boolean analyzeSuggestion(HouseIndex houseIndex) {
        //构造数字类型term的过滤器，使分析返回的token不包含数字类型的term
        final Map<String, Object> numericFilter = Map.of(
                "type", "keep_types",
                "types", new String[]{"<NUM>"},
                "mode", "exclude"
        );
        //构造term长度过滤器，使分析返回的token长度>=2，这样补全提示才有意义
        final Map<String, Object> lengthFilter = Map.of(
                "type", "length",
                "min", 2
        );
        // 用户输入的搜索词与这些域（底层存储的倒排索引词条）匹配
        AnalyzeRequest analyzeReq = AnalyzeRequest
                .buildCustomAnalyzer(IK_SMART)                  //使用的分词器
                .addTokenFilter(numericFilter)                  //过滤器，we can change term or add/remove term
                .addTokenFilter(lengthFilter)
                .build(                                         //被分析的内容
                        houseIndex.getTitle(),
                        houseIndex.getLayoutDesc(),
                        houseIndex.getRoundService(),
                        houseIndex.getDescription(),
                        houseIndex.getSubwayLineName(),
                        houseIndex.getSubwayStationName()
                );
        log.debug(gson.toJson(analyzeReq));
        try {
            final var response = rhlClient.indices().analyze(analyzeReq, RequestOptions.DEFAULT);
            // 获取词条分析结果 The token is the actual term that will be stored in the index
            final var analyzeTokenList = response.getTokens();
            if (CollectionUtils.isEmpty(analyzeTokenList)) {
                log.warn("词条分析结果解析失败: houseId = " + houseIndex.getHouseId());
                return false;
            }
            final var suggestList = analyzeTokenList.stream()
                    .map(token -> new HouseSuggest(token.getTerm()))
                    .collect(Collectors.toList());

            // 非analyze字段的直接加入补全建议关键词列表，小区名...等
            suggestList.add(new HouseSuggest(houseIndex.getDistrict()));

            // 把补全建议关键词列表放入索引
            houseIndex.setSuggest(suggestList);
            return true;
        } catch (Exception e) {
            log.error("词条分析失败: houseId = " + houseIndex.getHouseId(), e);
            return false;
        }
    }

    /**
     * 发消息到 kafka 进行索引删除
     *
     * @param retry 将要进行第几次重试
     */
    private void removeAsync(Integer houseId, int retry) {
        final var houseIndexMessage = new HouseIndexMessage(houseId, HouseIndexMessage.IndexOperation.REMOVE.getName(), retry);
        kafkaTemplate.send(MessageListener.INDEX_TOPIC, gson.toJson(houseIndexMessage));
    }

    /**
     * 处理返回的 ReplicationResponse 中的信息
     *
     * @return 索引操作是否成功
     */
    private boolean dealWithReplicationResponse(Integer houseId, ReplicationResponse response) {
        ReplicationResponse.ShardInfo shardInfo = response.getShardInfo();
        //处理成功的分片数少于总分片的情况
        if (shardInfo.getTotal() != shardInfo.getSuccessful()) {
            log.warn("索引操作成功的分片数少于总分片，houseId = " + houseId);
            return false;
        }
        //处理潜在的故障
        if (shardInfo.getFailed() > 0) {
            String reasonStr = Arrays.stream(shardInfo.getFailures())
                    .map(ReplicationResponse.ShardInfo.Failure::reason)
                    .collect(Collectors.joining(","));
            log.warn("索引操作出错，houseId = " + houseId + "，原因：" + reasonStr);
        }
        return false;
    }
}
