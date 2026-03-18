package com.mis.church.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;


@Entity
@Table(name = "equipment_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Many items belong to one equipment type
     */
    @ManyToOne
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    /**
     * Condition of the item (EXCELLENT, GOOD, FAIR, DAMAGED)
     */
    @ManyToOne
    @JoinColumn(name = "condition_status", nullable = false, referencedColumnName = "status_id")
    private Status conditionStatus;

    /**
     * Availability (AVAILABLE, ALLOCATED, MAINTENANCE)
     */
    @ManyToOne
    @JoinColumn(name = "availability_status", referencedColumnName = "status_id")
    private Status availabilityStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private Date updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    /**
     * Auto-manage timestamps
     */
    @PrePersist
    public void prePersist() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = new Date();
    }
}