package com.mis.church.repository;

import com.mis.church.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequestApprovalRepo extends JpaRepository<RequestApproval,Integer> {
    List<RequestApproval> findByRequestAndStatusAndIdNot(Request request, Status status,Long id);
    Optional<RequestApproval> findByIdAndStatus(Long id,Status status);
    List<RequestApproval> findByApproverRoleAndStatus_statusIdInOrderByCreatedAtDesc(Role approverRole, List<Integer> status);
    List<RequestApproval> findByApproverRoleOrderByCreatedAtDesc(Role approverRole);
    List<RequestApproval> findByStatus_statusIdInOrderByCreatedAtDesc(List<Integer> status);

    List<RequestApproval> findByRequest_IdOrderByStepLevelAsc(Long requestId);

}
