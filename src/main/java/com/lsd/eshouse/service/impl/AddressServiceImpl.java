package com.lsd.eshouse.service.impl;

import com.lsd.eshouse.common.dto.SubwayDTO;
import com.lsd.eshouse.common.dto.SubwayStationDTO;
import com.lsd.eshouse.common.dto.SupportAddressDTO;
import com.lsd.eshouse.entity.Subway;
import com.lsd.eshouse.entity.SubwayStation;
import com.lsd.eshouse.entity.SupportAddress;
import com.lsd.eshouse.repository.SubwayRepository;
import com.lsd.eshouse.repository.SubwayStationRepository;
import com.lsd.eshouse.repository.SupportAddressRepository;
import com.lsd.eshouse.service.AddressService;
import com.lsd.eshouse.common.vo.MultiResultVo;
import com.lsd.eshouse.common.vo.ResultVo;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by lsd
 * 2020-01-26 09:32
 */
@Service
public class AddressServiceImpl implements AddressService {

    @Autowired
    private SupportAddressRepository supportAddressRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private SubwayStationRepository subwayStationRepository;
    @Autowired
    private SubwayRepository subwayRepository;

    @Override
    public MultiResultVo<SupportAddressDTO> findAllCities() {
        // 查出所有市
        var addresses = supportAddressRepository.findAllByLevel(SupportAddress.Level.CITY.getValue());
        // 转为DTO对象
        var addressDTOList = addresses.stream()
                .map(addr -> modelMapper.map(addr, SupportAddressDTO.class))
                .collect(Collectors.toList());
        return new MultiResultVo<>(addressDTOList.size(),addressDTOList);
    }


    @Override
    public Map<SupportAddress.Level, SupportAddressDTO> findCityAndRegion(String cityEnName, String regionEnName) {
        Map<SupportAddress.Level, SupportAddressDTO> result = new HashMap<>();

        SupportAddress city = supportAddressRepository.findByEnNameAndLevel(cityEnName, SupportAddress.Level.CITY
                .getValue());
        SupportAddress region = supportAddressRepository.findByEnNameAndBelongTo(regionEnName, city.getEnName());

        result.put(SupportAddress.Level.CITY, modelMapper.map(city, SupportAddressDTO.class));
        result.put(SupportAddress.Level.REGION, modelMapper.map(region, SupportAddressDTO.class));
        return result;
    }

    @Override
    public MultiResultVo<SupportAddressDTO> findAllRegionsByCityName(String cityName) {
        if (cityName == null) {
            return new MultiResultVo<>(0, null);
        }

        List<SupportAddressDTO> result = new ArrayList<>();

        List<SupportAddress> regions = supportAddressRepository.findAllByLevelAndBelongTo(SupportAddress.Level.REGION
                .getValue(), cityName);
        for (SupportAddress region : regions) {
            result.add(modelMapper.map(region, SupportAddressDTO.class));
        }
        return new MultiResultVo<>(regions.size(), result);
    }

    @Override
    public List<SubwayDTO> findAllSubwayByCity(String cityEnName) {
        List<SubwayDTO> result = new ArrayList<>();
        List<Subway> subways = subwayRepository.findAllByCityEnName(cityEnName);
        if (subways.isEmpty()) {
            return result;
        }

        subways.forEach(subway -> result.add(modelMapper.map(subway, SubwayDTO.class)));
        return result;
    }

    @Override
    public List<SubwayStationDTO> findAllStationBySubway(Integer subwayId) {
        List<SubwayStationDTO> result = new ArrayList<>();
        List<SubwayStation> stations = subwayStationRepository.findAllBySubwayId(subwayId);
        if (stations.isEmpty()) {
            return result;
        }

        stations.forEach(station -> result.add(modelMapper.map(station, SubwayStationDTO.class)));
        return result;
    }

    @Override
    public ResultVo<SubwayDTO> findSubway(Integer subwayId) {
        if (subwayId == null) {
            return ResultVo.notFound();
        }
        Subway subway = subwayRepository.findById(subwayId).get();
        if (subway == null) {
            return ResultVo.notFound();
        }
        return ResultVo.of(modelMapper.map(subway, SubwayDTO.class));
    }

    @Override
    public ResultVo<SubwayStationDTO> findSubwayStation(Integer stationId) {
        if (stationId == null) {
            return ResultVo.notFound();
        }
        SubwayStation station = subwayStationRepository.findById(stationId).get();
        if (station == null) {
            return ResultVo.notFound();
        }
        return ResultVo.of(modelMapper.map(station, SubwayStationDTO.class));
    }

    @Override
    public ResultVo<SupportAddressDTO> findCity(String cityEnName) {
        if (cityEnName == null) {
            return ResultVo.notFound();
        }

        SupportAddress supportAddress = supportAddressRepository.findByEnNameAndLevel(cityEnName, SupportAddress.Level.CITY.getValue());
        if (supportAddress == null) {
            return ResultVo.notFound();
        }

        SupportAddressDTO addressDTO = modelMapper.map(supportAddress, SupportAddressDTO.class);
        return ResultVo.of(addressDTO);
    }

}
