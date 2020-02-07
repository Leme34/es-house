package com.lsd.eshouse.service.impl;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lsd.eshouse.common.constant.RentValueRangeBlock;
import com.lsd.eshouse.common.dto.HouseSuggest;
import com.lsd.eshouse.common.form.RentSearchForm;
import com.lsd.eshouse.common.index.HouseIndex;
import com.lsd.eshouse.common.index.HouseIndexKey;
import com.lsd.eshouse.common.index.HouseIndexMessage;
import com.lsd.eshouse.common.utils.HouseSortUtil;
import com.lsd.eshouse.common.vo.MultiResultVo;
import com.lsd.eshouse.common.vo.ResultVo;
import com.lsd.eshouse.entity.House;
import com.lsd.eshouse.entity.HouseTag;
import com.lsd.eshouse.msg.MessageListener;
import com.lsd.eshouse.repository.HouseDetailRepository;
import com.lsd.eshouse.repository.HouseRepository;
import com.lsd.eshouse.repository.HouseTagRepository;
import com.lsd.eshouse.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
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
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.rest.RestStatus;
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
    private ModelMapper modelMapper;
    @Autowired
    private RestClient restClient;
    @Autowired
    private RestHighLevelClient rhlClient;
    @Autowired
    private Gson gson;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
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
        // 查询house detail
        final var houseDetail = houseDetailRepository.findByHouseId(houseId);
        if (houseDetail == null) {
            // 发消息进行重试，消息重试次数+1
            retryIndex(houseId, retry + 1, "房源详细信息不存在");
            return;
        }
        // 查询house tags
        List<HouseTag> houseTags = houseTagRepository.findAllByHouseId(houseId);
        // 构建索引对象
        HouseIndex houseIndex = modelMapper.map(houseOpt.get(), HouseIndex.class);
        modelMapper.map(houseDetail, houseIndex);
        if (!CollectionUtils.isEmpty(houseTags)) {
            houseIndex.setTags(houseTags.stream().map(HouseTag::getName).collect(Collectors.toList()));
        }
        houseIndex.setHouseId(houseId);

        // 索引前先term查询索引是否存在
        var sourceBuilder = new SearchSourceBuilder().query(QueryBuilders.termQuery(HouseIndexKey.HOUSE_ID, houseId));
        var searchRequest = new SearchRequest(INDEX_NAME).source(sourceBuilder);
        boolean success = false;
        try {
            final var response = rhlClient.search(searchRequest, RequestOptions.DEFAULT);
            final var totalHits = response.getHits().getTotalHits().value;
            // 索引不存在，则创建索引
            if (totalHits == 0) {
                success = this.createIndex(houseIndex);
            } else if (totalHits == 1) {  //已存在则更新索引
                var documentId = response.getHits().getAt(0).getId();
                success = this.createUpdateIndex(documentId, houseIndex);
            } else {  //其他情况先删除再新增索引
                success = deleteAndCreateIndex(houseIndex);
            }
        } catch (IOException e) {
            log.error("房源索引失败，houseId = " + houseId, e);
        }
        if (success) {
            log.debug("房源索引成功，houseId = " + houseId);
            return;
        }
        // 发消息进行重试，消息重试次数+1
        retryIndex(houseId, retry + 1, "索引操作失败");
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
        } catch (IOException e) {
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
            request.setJsonEntity(gson.toJson(index));
            log.debug(gson.toJson(index));
            Response response = restClient.performRequest(request);
            // 是否创建成功
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_CREATED) {
                return true;
            }
            // 记录失败信息
            log.error("创建房源索引失败，houseId = " + index.getHouseId() + ",返回体：{}", EntityUtils.toString(response.getEntity()));
        } catch (IOException e) {
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
        final Map<String, Object> map = gson.fromJson(gson.toJson(index), new TypeToken<Map<String, Object>>() {
        }.getType());
        final var request = new IndexRequest(INDEX_NAME)
                .id(documentId)
                .source(map)
                .opType(opType);
        log.debug(gson.toJson(map));
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
            }
        } catch (IOException e) {
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
            }
        } catch (IOException e) {
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
            log.debug("删除索引成功，houseId = " + houseId);
        } catch (IOException e) {
            log.error("删除索引出错，houseId = " + houseId, e);
        }
    }

    @Override
    public void remove(Integer houseId) {
        this.removeAsync(houseId, 0);
    }

    @Override
    public MultiResultVo<Integer> search(RentSearchForm searchForm) {
        // 城市、地区等筛选栏使用filterQuery
        final var boolQB = new BoolQueryBuilder().filter(new TermQueryBuilder(HouseIndexKey.CITY_EN_NAME, searchForm.getCityEnName()));
        final String regionEnName = searchForm.getRegionEnName();
        if (StringUtils.isNotBlank(regionEnName) && !StringUtils.equals(regionEnName, "*")) {
            boolQB.filter(new TermQueryBuilder(HouseIndexKey.REGION_EN_NAME, regionEnName));
        }
        // keywords使用multiQuery
        boolQB.must(
                new MultiMatchQueryBuilder(searchForm.getKeywords(),
                        HouseIndexKey.TITLE,
                        HouseIndexKey.TRAFFIC,
                        HouseIndexKey.DISTRICT,
                        HouseIndexKey.ROUND_SERVICE,
                        HouseIndexKey.SUBWAY_LINE_NAME,
                        HouseIndexKey.SUBWAY_STATION_NAME)
        );
        // 面积、租金使用rangeQuery
        final var areaRange = RentValueRangeBlock.matchArea(searchForm.getAreaBlock());
        final var priceRange = RentValueRangeBlock.matchPrice(searchForm.getPriceBlock());
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
        if (searchForm.getDirection() > 0) {
            boolQB.filter(QueryBuilders.termQuery(HouseIndexKey.DIRECTION, searchForm.getDirection()));
        }
        //租赁方式
        if (searchForm.getRentWay() > -1) {
            boolQB.filter(QueryBuilders.termQuery(HouseIndexKey.RENT_WAY, searchForm.getRentWay()));
        }
        //搜索
        final var sourceBuilder = new SearchSourceBuilder()
                .query(boolQB)
                .sort(HouseSortUtil.getSortKey(searchForm.getOrderBy()), SortOrder.fromString(searchForm.getOrderDirection()))
                .from(searchForm.getStart())
                .size(searchForm.getSize());
        final var searchRequest = new SearchRequest(INDEX_NAME).source(sourceBuilder);
        log.debug(sourceBuilder.toString());
        try {
            final var response = rhlClient.search(searchRequest, RequestOptions.DEFAULT);
            if (response.status() != RestStatus.OK) {
                log.warn("查询失败，searchRequest = {}", searchRequest.toString());
                return new MultiResultVo<>(0, List.of());
            }
            final var houseIds = Arrays.stream(response.getHits().getHits()).map(hit ->
                    Integer.parseInt(String.valueOf(hit.getSourceAsMap().get(HouseIndexKey.HOUSE_ID)))
            ).collect(Collectors.toList());
            return new MultiResultVo<>(response.getHits().getTotalHits().value, houseIds);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new MultiResultVo<>(0, List.of());
    }

    @Override
    public ResultVo<List<String>> suggest(String prefix) {
        //参照:https://www.elastic.co/guide/en/elasticsearch/reference/6.6/search-suggesters-completion.html
        var completionSuggestion = SuggestBuilders.completionSuggestion(HouseIndexKey.suggestion)
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
                    .filter(entry -> {
                        if (entry instanceof CompletionSuggestion.Entry) {
                            final CompletionSuggestion.Entry item = (CompletionSuggestion.Entry) entry;
                            return !item.getOptions().isEmpty();
                        }
                        return false;
                    }).map(entry ->
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
        } catch (IOException e) {
            log.error("获取补全建议关键词失败", e);
            return ResultVo.of(List.of());
        }
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
        } catch (IOException e) {
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
