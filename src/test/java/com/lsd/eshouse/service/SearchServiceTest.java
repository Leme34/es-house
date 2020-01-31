package com.lsd.eshouse.service;

import com.lsd.EsHouseApplicationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by lsd
 * 2020-01-29 22:11
 */
public class SearchServiceTest extends EsHouseApplicationTest {

    @Autowired
    private SearchService searchService;

    @Test
    public void index() {
        searchService.index(15)
    }

}
