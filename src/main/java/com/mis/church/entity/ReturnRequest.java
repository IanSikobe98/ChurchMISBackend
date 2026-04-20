package com.mis.church.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;


@Entity
@Table(name = "return_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trx_id", nullable = false, length = 200)
    private String trxId;

    /**
     * Workflow for return approval
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private ApprovalWorkflow workflow;

    /**
     * Status (PENDING, APPROVED, REJECTED)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status", referencedColumnName = "status_id")
    private Status status;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "current_approval_level")
    private Integer currentApprovalLevel;

    @Column(name = "created_at", updatable = false)
    private Date createdAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_at")
    private Date updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    /**
     * Auto timestamps
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