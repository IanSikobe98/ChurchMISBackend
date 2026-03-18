package com.mis.church.repository;

import com.mis.church.entity.ApprovalWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApprovalWorkflowRepo extends JpaRepository<ApprovalWorkflow,Integer> {
}
