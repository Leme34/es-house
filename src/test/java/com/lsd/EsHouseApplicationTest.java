package com.lsd;

import com.lsd.eshouse.EsHouseApplication;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Created by lsd
 * 2020-01-22 17:02
 */
@ActiveProfiles("test") //指定使用测试环境配置
@RunWith(SpringRunner.class)
@SpringBootTest(classes = EsHouseApplication.class)
public class EsHouseApplicationTest {
}
