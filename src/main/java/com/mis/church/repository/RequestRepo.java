package com.mis.church.repository;

import com.mis.church.entity.Equipment;
import com.mis.church.entity.Request;
import com.mis.church.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequestRepo  extends JpaRepository<Request,Integer> {
    List<Request> findByStatus_StatusIdInOrderByCreatedAtDesc(List<Integer> statusId);
    List<Request> findByCreatedByAndStatus(String createdBy, Status status);
}
