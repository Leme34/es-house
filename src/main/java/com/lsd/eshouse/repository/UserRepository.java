package com.lsd.eshouse.repository;

import com.lsd.eshouse.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {

    Optional<User> findByName(String username);

    @Modifying
    @Query("update User as user set user.name = :name where user.id = :id")
    void updateUsername(@Param(value = "id") Integer id, @Param(value = "name") String name);

    @Modifying
    @Query("update User as user set user.email = :email where user.id = :id")
    void updateEmail(@Param(value = "id") Integer id, @Param(value = "email") String email);

    @Modifying
    @Query("update User as user set user.password = :password where user.id = :id")
    void updatePassword(@Param(value = "id") Integer id, @Param(value = "password") String password);

}
