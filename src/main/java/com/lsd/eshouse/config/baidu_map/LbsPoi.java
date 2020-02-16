package com.lsd.eshouse.config.baidu_map;

import lombok.Data;

/**
 * LBS.云服务 - 位置数据（poi）管理相关
 * 参照：http://lbsyun.baidu.com/index.php?title=lbscloud/api
 */
@Data
public class LbsPoi{
    private String geotableId; //虎鲸数据管理平台表id
    private String createUrl;
    private String queryUrl;
    private String updateUrl;
    private String deleteUrl;
}
