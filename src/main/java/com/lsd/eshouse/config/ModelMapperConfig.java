package com.lsd.eshouse.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 注入第三方ModelMapper工具类
 *
 * Created by lsd
 * 2020-01-26 10:27
 */
@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        final ModelMapper modelMapper = new ModelMapper();
        //完全匹配
        modelMapper.getConfiguration().setFullTypeMatchingRequired(true);
        //匹配策略定义为严格
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        return modelMapper;
    }

}
