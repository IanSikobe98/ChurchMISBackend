package com.mis.church.repository;

import com.mis.church.entity.Request;
import com.mis.church.entity.RequestApproval;
import com.mis.church.entity.RequestEquipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestEquipmentRepo extends JpaRepository<RequestEquipment,Integer> {
    List<RequestEquipment> findByRequest(Request request);
    List<RequestEquipment> findByRequest_Id(Long request_id);
}
