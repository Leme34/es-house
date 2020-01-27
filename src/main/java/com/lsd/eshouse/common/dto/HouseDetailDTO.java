package com.lsd.eshouse.common.dto;

import lombok.Data;

@Data
public class HouseDetailDTO {

    private String description;

    private String layoutDesc;

    private String traffic;

    private String roundService;

    private int rentWay;

    private Integer adminId;

    private String address;

    private Integer subwayLineId;

    private Integer subwayStationId;

    private String subwayLineName;

    private String subwayStationName;

}
