package com.lsd.eshouse.common.utils;

import org.springframework.data.domain.Sort;

import java.util.Set;

/**
 * 排序生成器
 */
public class HouseSortUtil {
    public static final String DEFAULT_SORT_KEY = "lastUpdateTime";

    public static final String DISTANCE_TO_SUBWAY_KEY = "distanceToSubway";

    private static final Set<String> SORT_KEYS = Set.of(
            "createTime",
            "price",
            "area",
            DEFAULT_SORT_KEY,
            DISTANCE_TO_SUBWAY_KEY
    );

    public static Sort getSort(String key, String directionKey) {
        String sortKey = getSortKey(key);
        Sort.Direction direction = Sort.Direction.fromOptionalString(directionKey)
                .orElse(Sort.Direction.DESC);
        return new Sort(direction, sortKey);
    }

    public static String getSortKey(String key) {
        if (!SORT_KEYS.contains(key)) {
            key = DEFAULT_SORT_KEY;
        }

        return key;
    }

}
