package com.lsd.eshouse.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class HouseDTO implements Serializable {
    private static final long serialVersionUID = 8918735582286008182L;
    private Integer id;

    private String title;

    private int price;

    private int area;

    private int direction;

    private int room;

    private int parlour;

    private int bathroom;

    private int floor;

    private Integer adminId;

    private String district;

    private int totalFloor;

    private int watchTimes;

    private int buildYear;

    private int status;

    private Date createTime;

    private Date lastUpdateTime;

    private String cityEnName;

    private String regionEnName;

    private String street;

    private String cover;

    private int distanceToSubway;

    private HouseDetailDTO houseDetail;

    private List<String> tags;

    public List<String> getTags() {
        if (this.tags == null) {
            tags = new ArrayList<>();
        }
        return tags;
    }

    private List<HousePictureDTO> pictures;

    private int subscribeStatus;

}
