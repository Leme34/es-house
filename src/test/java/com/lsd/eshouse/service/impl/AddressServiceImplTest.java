package com.lsd.eshouse.service.impl;

import com.lsd.EsHouseApplicationTest;
import com.lsd.eshouse.common.dto.BaiduMapLocation;
import com.lsd.eshouse.common.vo.ResultVo;
import com.lsd.eshouse.service.AddressService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;

/**
 * Created by lsd
 * 2020-02-11 09:44
 */
public class AddressServiceImplTest extends EsHouseApplicationTest {

    @Autowired
    private AddressService addressService;

    @Test
    public void getBaiduMapLocation() {
        ResultVo<BaiduMapLocation> baiduMapLocationResult = addressService.getBaiduMapLocation("北京", "北京昌平区巩华家园1号楼2单元");
        System.out.println(baiduMapLocationResult);
    }

}
