package com.mis.church.dto;


import com.mis.church.entity.Equipment;
import lombok.Data;

@Data
public class EquipmentInfo {
    private Equipment equipment;
    private Integer quantity;
}
