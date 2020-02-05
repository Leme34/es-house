package com.lsd.eshouse.service;

import com.lsd.EsHouseApplicationTest;
import com.lsd.eshouse.common.form.RentSearchForm;
import com.lsd.eshouse.common.vo.MultiResultVo;
import org.junit.Assert;
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
        searchService.doIndex(15,0);
    }

    @Test
    public void search() {
        RentSearchForm searchForm = new RentSearchForm();
        searchForm.setCityEnName("bj");
        searchForm.setStart(0);
        searchForm.setSize(10);
        final MultiResultVo<Integer> result = searchService.search(searchForm);
        Assert.assertEquals(9, result.getTotal());
    }

    @Test
    public void remove() {
        searchService.doRemove(15,0);
    }

}
