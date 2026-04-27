package com.mis.church.repository;

import com.mis.church.entity.DptRoleMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DptRoleMapRepo extends JpaRepository<DptRoleMap, Long> {
    Optional<DptRoleMap> findByRole_RoleId(Integer roleRoleId);
}
