package com.mis.church.repository;

import com.mis.church.entity.ApprovalWorkflow;
import com.mis.church.entity.WorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowStepRepo extends JpaRepository<WorkflowStep, Integer> {

    List<WorkflowStep> findByWorkflow(ApprovalWorkflow workflow);
}
