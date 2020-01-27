package com.lsd.eshouse.service.impl;

import com.lsd.eshouse.common.constant.HouseStatus;
import com.lsd.eshouse.common.form.RentSearchForm;
import com.lsd.eshouse.common.utils.HouseSortUtil;
import com.lsd.eshouse.config.qiniu.QiNiuProperties;
import com.lsd.eshouse.common.dto.HouseDTO;
import com.lsd.eshouse.common.dto.HouseDetailDTO;
import com.lsd.eshouse.common.dto.HousePictureDTO;
import com.lsd.eshouse.entity.*;
import com.lsd.eshouse.common.form.DatatableSearchForm;
import com.lsd.eshouse.common.form.HouseForm;
import com.lsd.eshouse.common.form.PhotoForm;
import com.lsd.eshouse.repository.*;
import com.lsd.eshouse.service.HouseService;
import com.lsd.eshouse.common.utils.LoginUserUtil;
import com.lsd.eshouse.common.vo.MultiResultVo;
import com.lsd.eshouse.common.vo.ResultVo;
import com.lsd.eshouse.service.QiNiuService;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by lsd
 * 2020-01-26 14:11
 */
@Service
public class HouseServiceImpl implements HouseService {

    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private HouseRepository houseRepository;
    @Autowired
    private HouseDetailRepository houseDetailRepository;
    @Autowired
    private SubwayStationRepository subwayStationRepository;
    @Autowired
    private SubwayRepository subwayRepository;
    @Autowired
    private HousePictureRepository housePictureRepository;
    @Autowired
    private HouseTagRepository houseTagRepository;
    @Autowired
    private QiNiuProperties qiNiuProperties;
    @Autowired
    private QiNiuService qiNiuService;

    @Override
    public ResultVo<HouseDTO> save(HouseForm houseForm) {
        HouseDetail detail = new HouseDetail();
        ResultVo<HouseDTO> subwayValidtionResult = wrapperDetailInfo(detail, houseForm);
        if (subwayValidtionResult != null) {
            return subwayValidtionResult;
        }
        // house
        House house = modelMapper.map(houseForm, House.class);
        LocalDateTime now = LocalDateTime.now();
        house.setCreateTime(now)
                .setLastUpdateTime(now)
                .setAdminId(LoginUserUtil.getLoginUserId());
        house = houseRepository.save(house);
        // houseDetail
        detail.setHouseId(house.getId());
        detail = houseDetailRepository.save(detail);
        // 图片
        List<HousePicture> pictures = wrapperPicturesInfo(houseForm, house.getId());
        Iterable<HousePicture> housePictures = housePictureRepository.saveAll(pictures);
        // 构造返回的dto对象
        HouseDTO houseDTO = modelMapper.map(house, HouseDTO.class);
        HouseDetailDTO houseDetailDTO = modelMapper.map(detail, HouseDetailDTO.class);
        houseDTO.setHouseDetail(houseDetailDTO);
        List<HousePictureDTO> pictureDTOS = new ArrayList<>();
        housePictures.forEach(housePicture -> pictureDTOS.add(modelMapper.map(housePicture, HousePictureDTO.class)));
        houseDTO.setPictures(pictureDTOS);
        houseDTO.setCover(qiNiuProperties.getCdnPrefix() + houseDTO.getCover());
        // houseTag
        List<String> tags = houseForm.getTags();
        if (tags != null && !tags.isEmpty()) {
            List<HouseTag> houseTags = new ArrayList<>();
            for (String tag : tags) {
                houseTags.add(new HouseTag(house.getId(), tag));
            }
            houseTagRepository.saveAll(houseTags);
            houseDTO.setTags(tags);
        }
        return new ResultVo<>(true, null, houseDTO);
    }

    @Override
    public MultiResultVo<HouseDTO> adminQuery(DatatableSearchForm searchBody) {
        // 分页条件
        Sort sort = new Sort(Sort.Direction.fromString(searchBody.getDirection()), searchBody.getOrderBy());
        int page = searchBody.getStart() / searchBody.getLength();
        Pageable pageable = PageRequest.of(page, searchBody.getLength(), sort);
        // 查询条件
        Specification<House> specification = (root, criteriaQuery, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.equal(root.get("adminId"), LoginUserUtil.getLoginUserId());
            predicate = criteriaBuilder.and(predicate, criteriaBuilder.notEqual(root.get("status"), HouseStatus.DELETED.getValue()));

            if (searchBody.getCity() != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("cityEnName"), searchBody.getCity()));
            }

            if (searchBody.getStatus() != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("status"), searchBody.getStatus()));
            }

            if (searchBody.getCreateTimeMin() != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.greaterThanOrEqualTo(root.get("createTime"), searchBody.getCreateTimeMin()));
            }

            if (searchBody.getCreateTimeMax() != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.lessThanOrEqualTo(root.get("createTime"), searchBody.getCreateTimeMax()));
            }

            if (searchBody.getTitle() != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.like(root.get("title"), "%" + searchBody.getTitle() + "%"));
            }

            return predicate;
        };
        // 带条件分页查询
        final Page<House> housePage = houseRepository.findAll(specification, pageable);
        // 转为DTO对象
        final List<HouseDTO> houseDTOS = housePage.stream().map(house -> {
            HouseDTO houseDTO = modelMapper.map(house, HouseDTO.class);
            houseDTO.setCover(qiNiuProperties.getCdnPrefix() + house.getCover());
            return houseDTO;
        }).collect(Collectors.toList());
        return new MultiResultVo<>(housePage.getTotalElements(), houseDTOS);
    }

    @Override
    public ResultVo<HouseDTO> findCompleteOne(Integer id) {
        House house = houseRepository.findById(id).get();
        if (house == null) {
            return ResultVo.notFound();
        }
        HouseDetail detail = houseDetailRepository.findByHouseId(id);
        List<HousePicture> pictures = housePictureRepository.findAllByHouseId(id);

        HouseDetailDTO detailDTO = modelMapper.map(detail, HouseDetailDTO.class);
        List<HousePictureDTO> pictureDTOS = new ArrayList<>();
        for (HousePicture picture : pictures) {
            HousePictureDTO pictureDTO = modelMapper.map(picture, HousePictureDTO.class);
            pictureDTOS.add(pictureDTO);
        }


        List<HouseTag> tags = houseTagRepository.findAllByHouseId(id);
        List<String> tagList = new ArrayList<>();
        for (HouseTag tag : tags) {
            tagList.add(tag.getName());
        }

        HouseDTO result = modelMapper.map(house, HouseDTO.class);
        result.setHouseDetail(detailDTO);
        result.setPictures(pictureDTOS);
        result.setTags(tagList);

        return ResultVo.of(result);
    }

    @Transactional
    @Override
    public ResultVo update(HouseForm houseForm) {
        House house = this.houseRepository.findById(houseForm.getId()).get();
        if (house == null) {
            return ResultVo.notFound();
        }
        HouseDetail detail = this.houseDetailRepository.findByHouseId(house.getId());
        if (detail == null) {
            return ResultVo.notFound();
        }
        ResultVo wrapperResult = wrapperDetailInfo(detail, houseForm);
        if (wrapperResult != null) {
            return wrapperResult;
        }
        houseDetailRepository.save(detail);

        List<HousePicture> pictures = wrapperPicturesInfo(houseForm, houseForm.getId());
        housePictureRepository.saveAll(pictures);

        if (houseForm.getCover() == null) {
            houseForm.setCover(house.getCover());
        }

        modelMapper.map(houseForm, house);
        house.setLastUpdateTime(LocalDateTime.now());
        houseRepository.save(house);

        return ResultVo.success();
    }

    @Override
    public ResultVo removePhoto(Integer id) {
        HousePicture picture = housePictureRepository.findById(id).get();
        if (picture == null) {
            return ResultVo.notFound();
        }

        try {
            Response response = qiNiuService.delete(picture.getPath());
            if (response.isOK()) {
                housePictureRepository.deleteById(id);
                return ResultVo.success();
            } else {
                return new ResultVo(false, response.error);
            }
        } catch (QiniuException e) {
            e.printStackTrace();
            return new ResultVo(false, e.getMessage());
        }
    }

    @Transactional
    @Override
    public ResultVo updateCover(Integer coverId, Integer targetId) {
        HousePicture cover = housePictureRepository.findById(coverId).get();
        if (cover == null) {
            return ResultVo.notFound();
        }
        houseRepository.updateCover(targetId, cover.getPath());
        return ResultVo.success();
    }

    @Transactional
    @Override
    public ResultVo updateStatus(Integer id, int status) {
        House house = houseRepository.findById(id).get();
        if (house == null) {
            return ResultVo.notFound();
        }
        if (house.getStatus() == status) {
            return new ResultVo(false, "状态没有发生变化");
        }
        if (house.getStatus() == HouseStatus.RENTED.getValue()) {
            return new ResultVo(false, "已出租的房源不允许修改状态");
        }
        if (house.getStatus() == HouseStatus.DELETED.getValue()) {
            return new ResultVo(false, "已删除的资源不允许操作");
        }
        houseRepository.updateStatus(id, status);
        return ResultVo.success();
    }

    @Override
    public MultiResultVo<HouseDTO> query(RentSearchForm searchForm) {
        // 条件分页查询house
        var sort = HouseSortUtil.getSort(searchForm.getOrderBy(), searchForm.getOrderDirection());
        int page = searchForm.getStart() / searchForm.getSize();
        var pageRequest = PageRequest.of(page, searchForm.getSize(), sort);
        Specification<House> specification = (root, criteriaQuery, criteriaBuilder) -> {
            var statusPredicate = criteriaBuilder.equal(root.get("status"), HouseStatus.PASSES.getValue());
            var cityEnNamePredicate = criteriaBuilder.equal(root.get("cityEnName"), searchForm.getCityEnName());
            Predicate predicate = criteriaBuilder.and(statusPredicate, cityEnNamePredicate);
            if (HouseSortUtil.DISTANCE_TO_SUBWAY_KEY.equals(searchForm.getOrderBy())) {
                var distanceToSubwayPredicate = criteriaBuilder.gt(root.get(HouseSortUtil.DISTANCE_TO_SUBWAY_KEY), -1);
                predicate = criteriaBuilder.and(predicate,distanceToSubwayPredicate);
            }
            return predicate;
        };
        var housePage = houseRepository.findAll(specification, pageRequest);

        // houseDetail信息，空间换时间
        Map<Integer, HouseDTO> houseDTOMap = new HashMap<>();
        // 转换DTO对象
        final List<HouseDTO> houseDTOList = housePage.stream().map(house -> {
            var houseDTO = modelMapper.map(house, HouseDTO.class);
            houseDTO.setCover(qiNiuProperties.getCdnPrefix() + house.getCover());
            houseDTOMap.put(houseDTO.getId(), new HouseDTO());
            return houseDTO;
        }).collect(Collectors.toList());

        // 查询并填充houseDetail信息
        wrapperHouseDTOListInfo(houseDTOMap);

        return new MultiResultVo<>(housePage.getTotalElements(), houseDTOList);
    }

    /**
     * 查询并填充houseDetail信息
     */
    private void wrapperHouseDTOListInfo(Map<Integer, HouseDTO> houseDTOMap) {
        houseDetailRepository.findAllByHouseIdIn(houseDTOMap.keySet())
                .forEach(houseDetail -> {
                    HouseDTO houseDTO = houseDTOMap.get(houseDetail.getHouseId());
                    HouseDetailDTO houseDetailDTO = modelMapper.map(houseDetail, HouseDetailDTO.class);
                    houseDTO.setHouseDetail(houseDetailDTO);
                });
        houseTagRepository.findAllByHouseIdIn(houseDTOMap.keySet())
                .forEach(tag -> {
                    HouseDTO houseDTO = houseDTOMap.get(tag.getHouseId());
                    houseDTO.getTags().add(tag.getName());
                });
    }

    /**
     * 图片对象列表信息填充
     */
    private List<HousePicture> wrapperPicturesInfo(HouseForm form, Integer houseId) {
        List<HousePicture> pictures = new ArrayList<>();
        if (form.getPhotos() == null || form.getPhotos().isEmpty()) {
            return pictures;
        }
        for (PhotoForm photoForm : form.getPhotos()) {
            HousePicture picture = new HousePicture();
            picture.setHouseId(houseId);
            picture.setCdnPrefix(qiNiuProperties.getCdnPrefix());
            picture.setPath(photoForm.getPath());
            picture.setWidth(photoForm.getWidth());
            picture.setHeight(photoForm.getHeight());
            pictures.add(picture);
        }
        return pictures;
    }

    /**
     * 房源详细信息对象填充
     */
    private ResultVo<HouseDTO> wrapperDetailInfo(HouseDetail houseDetail, HouseForm houseForm) {
        Subway subway = subwayRepository.findById(houseForm.getSubwayLineId()).get();
        if (subway == null) {
            return new ResultVo<>(false, "Not valid subway line!");
        }
        SubwayStation subwayStation = subwayStationRepository.findById(houseForm.getSubwayStationId()).get();
        if (subwayStation == null || subway.getId() != subwayStation.getSubwayId()) {
            return new ResultVo<>(false, "Not valid subway station!");
        }
        houseDetail.setSubwayLineId(subway.getId())
                .setSubwayLineName(subway.getName())
                .setSubwayStationId(subwayStation.getId())
                .setSubwayStationName(subwayStation.getName())
                .setDescription(houseForm.getDescription())
                .setAddress(houseForm.getDetailAddress())
                .setLayoutDesc(houseForm.getLayoutDesc())
                .setRentWay(houseForm.getRentWay())
                .setRoundService(houseForm.getRoundService())
                .setTraffic(houseForm.getTraffic());
        return null;

    }

}
