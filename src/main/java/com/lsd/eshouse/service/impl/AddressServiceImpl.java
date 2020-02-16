package com.lsd.eshouse.service.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lsd.eshouse.common.dto.BaiduMapLocation;
import com.lsd.eshouse.common.dto.SubwayDTO;
import com.lsd.eshouse.common.dto.SubwayStationDTO;
import com.lsd.eshouse.common.dto.SupportAddressDTO;
import com.lsd.eshouse.config.baidu_map.BaiduMapProperties;
import com.lsd.eshouse.entity.Subway;
import com.lsd.eshouse.entity.SubwayStation;
import com.lsd.eshouse.entity.SupportAddress;
import com.lsd.eshouse.repository.SubwayRepository;
import com.lsd.eshouse.repository.SubwayStationRepository;
import com.lsd.eshouse.repository.SupportAddressRepository;
import com.lsd.eshouse.service.AddressService;
import com.lsd.eshouse.common.vo.MultiResultVo;
import com.lsd.eshouse.common.vo.ResultVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by lsd
 * 2020-01-26 09:32
 */
@Slf4j
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
    @Autowired
    private HttpClient httpClient;
    @Autowired
    private BaiduMapProperties baiduMapProperties;
    @Autowired
    private Gson gson;

    @Override
    public MultiResultVo<SupportAddressDTO> findAllCities() {
        // 查出所有市
        var addresses = supportAddressRepository.findAllByLevel(SupportAddress.Level.CITY.getValue());
        // 转为DTO对象
        var addressDTOList = addresses.stream()
                .map(addr -> modelMapper.map(addr, SupportAddressDTO.class))
                .collect(Collectors.toList());
        return new MultiResultVo<>(addressDTOList.size(), addressDTOList);
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

    @Override
    public ResultVo<BaiduMapLocation> getBaiduMapLocation(String city, String address) {
        // 把要传的请求参数编码
        String encodeCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String encodeAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
        String url = String.format("%s?address=%s&city=%s&output=json&ak=%s",
                baiduMapProperties.getGeocoderApiPrefix(), encodeAddress, encodeCity, baiduMapProperties.getApiKey());
        // 发起Http请求调用百度地图API
        try {
            final HttpResponse response = httpClient.execute(new HttpGet(url));
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                return new ResultVo<>(false, "百度地图地理编码服务Web API接口调用失败");
            }
            String resultStr = EntityUtils.toString(response.getEntity(), "UTF-8");
            // 解析为json结构
            JsonObject resultJson = gson.fromJson(resultStr, JsonObject.class);
            int status = resultJson.get("status").getAsInt();
            if (status != 0) {
                return new ResultVo<>(false, "百度地图地理编码服务地址解析错误，status = " + status);
            }
            JsonObject locationJson = resultJson.getAsJsonObject("result").getAsJsonObject("location");
            BaiduMapLocation location = new BaiduMapLocation(locationJson.get("lng").getAsDouble(),
                    locationJson.get("lat").getAsDouble());
            return ResultVo.of(location);
        } catch (IOException e) {
            log.error("百度地图地理编码服务Web API接口调用失败", e);
            return new ResultVo<>(false, "百度地图地理编码服务Web API接口调用失败");
        }
    }

}
