package com.mis.church.repository;

import com.mis.church.entity.EquipmentItem;
import com.mis.church.entity.Status;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentItemRepo extends JpaRepository<EquipmentItem,Integer> {

    List<EquipmentItem> findByEquipment_IdAndAvailabilityStatus(Long equipmentId, Status status);
    List<EquipmentItem> findByEquipment_IdAndAvailabilityStatusAndConditionStatus_StatusId(Long equipment_id,
                                                                                           Status status, Integer statusId,Pageable pageable);

}
