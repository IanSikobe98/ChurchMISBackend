package com.mis.church.repository;

import com.mis.church.entity.EquipmentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EquipmentReturnRepo extends JpaRepository<EquipmentAllocation,Integer> {
}
