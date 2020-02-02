package com.lsd.eshouse.msg;

import com.google.gson.Gson;
import com.lsd.eshouse.common.index.HouseIndexMessage;
import com.lsd.eshouse.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static com.lsd.eshouse.common.index.HouseIndexMessage.IndexOperation;

/**
 * Created by lsd
 * 2020-01-31 10:49
 */
@Slf4j
@Component
public class MessageListener {

    public final static String INDEX_TOPIC = "house_build";

    @Autowired
    private Gson gson;
    @Autowired
    private SearchService searchService;

    @KafkaListener(topics = INDEX_TOPIC)
    public void handlerMessage(String content) {
        // parse message
        var houseIndexMessage = gson.fromJson(content, HouseIndexMessage.class);
        // avoid NPE
        if (houseIndexMessage == null) {
            log.warn("消息内容解析失败，消息体：{}", content);
            return;
        }
        // handler index message
        final var houseId = houseIndexMessage.getHouseId();
        final int retry = houseIndexMessage.getRetry();
        IndexOperation.getByName(houseIndexMessage.getOperation()).ifPresentOrElse(operation -> {
                    switch (operation) {
                        case INDEX:
                            searchService.doIndex(houseId, retry);
                            break;
                        case REMOVE:
                            searchService.doRemove(houseId, retry);
                            break;
                        default:
                            log.warn("不支持的消息内容，消息体：{}", content);
                    }
                },
                () -> log.warn("不支持的消息内容，消息体：{}", content)
        );
    }

}
