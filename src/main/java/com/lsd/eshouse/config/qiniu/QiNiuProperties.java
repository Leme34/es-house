package com.lsd.eshouse.config.qiniu;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 读取七牛云自定义配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "qiniu")
public class QiNiuProperties {

    private String accessKey;
    private String secretKey;
    private String bucket;
    private String cdnPrefix;
    // 外链默认域名
    private String hostPrefix;
    // 允许上传的文件类型
    private List<String> allowTypes;

}
