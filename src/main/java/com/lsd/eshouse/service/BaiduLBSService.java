package com.lsd.eshouse.service;

import com.lsd.eshouse.common.dto.BaiduMapLocation;
import com.lsd.eshouse.common.vo.ResultVo;

/**
 * 百度地图LBS.云服务
 *
 * 介绍：http://lbsyun.baidu.com/index.php?title=lbscloud/guide
 * API文档：http://lbsyun.baidu.com/index.php?title=lbscloud/api
 *
 * Created by lsd
 * 2020-02-14 11:35
 */
public interface BaiduLBSService {

    /**
     * 上传百度LBS数据
     */
    ResultVo upload(BaiduMapLocation location, String title, String address,
                       Integer houseId, int price, int area);

    /**
     * 移除百度LBS数据
     */
    ResultVo remove(Integer houseId);

}
