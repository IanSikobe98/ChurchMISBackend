package com.mis.church.repository;

import com.mis.church.entity.RequestApproval;
import com.mis.church.entity.RequestEquipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestEquipmentRepo extends JpaRepository<RequestEquipment,Integer> {
}
