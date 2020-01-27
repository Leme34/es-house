package com.lsd.eshouse.repository;

import com.lsd.eshouse.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface RoleRepository extends JpaRepository<Role, Integer>, JpaSpecificationExecutor<Role> {

    List<Role> findRolesByUserId(Integer userId);

}
