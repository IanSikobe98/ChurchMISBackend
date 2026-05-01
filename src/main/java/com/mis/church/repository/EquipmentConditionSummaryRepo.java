package com.mis.church.repository;

import com.mis.church.entity.views.EquipmentConditionSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentConditionSummaryRepo extends JpaRepository<EquipmentConditionSummary,Long> {
    List<EquipmentConditionSummary> findByEquipmentIdIn(List<Long> id);
    List<EquipmentConditionSummary> findByEquipmentIdInAndStatusIdIn(List<Long> id,List<Integer> statusIds);
}
