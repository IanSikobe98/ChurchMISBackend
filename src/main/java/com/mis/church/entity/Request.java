package com.mis.church.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
import java.util.List;


@Entity
@Table(name = "requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trx_id", nullable = false, length = 200)
    private String trxId;

    @Column(name = "event", nullable = false, length = 2000)
    private String event;

    @Column(name = "purpose", nullable = false, length = 2000)
    private String purpose;

    @Column(name = "venue", nullable = false, length = 200)
    private String venue;

    /**
     * Workflow linked to this request
     */
    @ManyToOne
    @JoinColumn(name = "workflow_id", nullable = false)
    private ApprovalWorkflow workflow;

    /**
     * Request status (PENDING, APPROVED, REJECTED, etc.)
     */
    @ManyToOne
    @JoinColumn(name = "status", referencedColumnName = "status_id")
    private Status status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private Date updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    /**
     * Optional: link approvals (very useful)
     */
    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RequestApproval> approvals;

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