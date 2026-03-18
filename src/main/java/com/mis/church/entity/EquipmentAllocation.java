package com.mis.church.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "equipment_allocations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Request this allocation belongs to
     */
    @ManyToOne
    @JoinColumn(name = "request_id", nullable = false)
    private Request request;

    /**
     * Actual equipment item allocated
     */
    @ManyToOne
    @JoinColumn(name = "equipment_item_id", nullable = false)
    private EquipmentItem equipmentItem;

    /**
     * Condition before allocation
     */
    @ManyToOne
    @JoinColumn(name = "condition_before", referencedColumnName = "status_id")
    private Status conditionBefore;

    /**
     * Condition after return
     */
    @ManyToOne
    @JoinColumn(name = "condition_after", referencedColumnName = "status_id")
    private Status conditionAfter;

    /**
     * Allocation status (ALLOCATED, RETURNED, LOST, etc.)
     */
    @ManyToOne
    @JoinColumn(name = "status", referencedColumnName = "status_id")
    private Status status;

    @Column(name = "allocated_at", nullable = false, updatable = false)
    private Date allocatedAt;

    @Column(name = "returned_at")
    private Date returnedAt;

    @Column(name = "returned_by", length = 100)
    private String returnedBy;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

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

        if (this.allocatedAt == null) {
            this.allocatedAt = new Date();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = new Date();
    }
}