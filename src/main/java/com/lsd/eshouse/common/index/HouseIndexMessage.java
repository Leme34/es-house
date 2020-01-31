package com.lsd.eshouse.common.index;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * kafka消息结构体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor //默认构造器 防止jackson序列化失败
public class HouseIndexMessage {

    private Integer houseId;
    private String operation;
    // 用于记录改消息已被重试的次数
    private int retry = 0;

    /**
     * operation字段枚举，name对应 SearchService 的接口名称
     *
     * @see com.lsd.eshouse.service.SearchService
     */
    @Getter
    @AllArgsConstructor
    public enum IndexOperation {
        INDEX("index"),
        REMOVE("remove");
        private String name;

        /**
         * 根据属性值查询枚举
         */
        public static Optional<IndexOperation> getByName(String name) {
            if (name == null) {
                return Optional.empty();
            }
            for (IndexOperation operation : IndexOperation.values()) {
                if (operation.getName().equals(name)) {
                    return Optional.of(operation);
                }
            }
            return Optional.empty();
        }
    }

}
