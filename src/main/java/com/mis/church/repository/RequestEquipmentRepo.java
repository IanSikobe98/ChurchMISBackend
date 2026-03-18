package com.mis.church.repository;

import com.mis.church.entity.RequestApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestEquipmentRepo extends JpaRepository<RequestApproval,Integer> {
}
