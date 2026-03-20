package com.mis.church.repository;

import com.mis.church.entity.EquipmentAllocation;
import com.mis.church.entity.Request;
import com.mis.church.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentAllocationRepo extends JpaRepository<EquipmentAllocation,Integer> {
    List<EquipmentAllocation> findByRequestAndStatus(Request request, Status status);
    List<EquipmentAllocation> findByRequest_Id(Long request);
}
