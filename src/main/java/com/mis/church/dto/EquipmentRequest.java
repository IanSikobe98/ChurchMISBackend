package com.mis.church.dto;

import lombok.Data;

@Data
public class EquipmentRequest {
    private Long equipmentId;
    private int quantity;
    private String purpose;
    private String venue;
    private String event;

}
