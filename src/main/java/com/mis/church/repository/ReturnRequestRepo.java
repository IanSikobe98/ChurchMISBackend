package com.mis.church.repository;

import com.mis.church.entity.ReturnRequest;
import com.mis.church.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnRequestRepo extends JpaRepository<ReturnRequest,Integer> {
    List<ReturnRequest> findByCreatedByAndStatus(String username, Status status);
}
