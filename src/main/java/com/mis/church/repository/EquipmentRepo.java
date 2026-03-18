package com.mis.church.repository;

import com.mis.church.entity.Equipment;
import com.mis.church.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EquipmentRepo extends JpaRepository<Equipment,Integer> {
}
