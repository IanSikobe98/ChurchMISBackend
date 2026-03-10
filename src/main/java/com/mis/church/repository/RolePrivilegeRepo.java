package com.mis.church.repository;

import com.mis.church.entity.RolePrivilege;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolePrivilegeRepo extends JpaRepository<RolePrivilege, Integer> {
    List<RolePrivilege> findByRole_RoleId(Integer roleId);
}