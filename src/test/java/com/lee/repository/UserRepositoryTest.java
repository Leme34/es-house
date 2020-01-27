package com.lee.repository;

import com.lee.EsHouseApplicationTest;
import com.lsd.eshouse.entity.User;
import com.lsd.eshouse.repository.UserRepository;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

/**
 * Created by lsd
 * 2020-01-22 17:04
 */
public class UserRepositoryTest extends EsHouseApplicationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    public void test(){
        final Optional<User> user = userRepository.findById(1);
        user.ifPresent(System.out::println);
    }

}
