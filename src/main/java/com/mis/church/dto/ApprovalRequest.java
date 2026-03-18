package com.mis.church.dto;



import com.mis.church.enums.ApprovalType;
import lombok.Data;

import java.util.List;

@Data
public class ApprovalRequest {
    private String action;
    private String description;
    private List<String> ids;
    private ApprovalType approvalType;
    private List<ApprovalEquipment> approvalEquipment;
}