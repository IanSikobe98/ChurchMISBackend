package com.mis.church.dto;

import lombok.Data;

import java.util.List;

@Data
public class EquipmentRequest {
    private List<EquipmentRequestDTO> equipmentRequestDTOList;
    private String purpose;
    private String venue;
    private String event;
    private String returnDate;

}
