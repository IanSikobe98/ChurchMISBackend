package com.mis.church.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;


@Entity
@Table(
    name = "request_approvals",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_request_approver_level",
        columnNames = {"request_id", "approver_id", "step_level"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Request being approved
     */
    @ManyToOne
    @JoinColumn(name = "request_id", nullable = false)
    private Request request;

    /**
     * Role assigned to this step
     */
    @ManyToOne
    @JoinColumn(name = "approver_role_id", nullable = false)
    private Role approverRole;

    /**
     * Step level in the workflow
     */
    @Column(name = "step_level", nullable = false)
    private Integer stepLevel;

    /**
     * Status of this approval: PENDING, APPROVED, REJECTED
     */
    @ManyToOne
    @JoinColumn(name = "status", referencedColumnName = "status_id")
    private Status status;

    @Column(name = "is_allocater")
    private Boolean isAllocater = false;

    /**
     * Comments from approver
     */
    @Column(name = "comments", length = 2000)
    private String comments;

    /**
     * Who actually performed the action (can differ from assigned approver)
     */
    @ManyToOne
    @JoinColumn(name = "action_by", referencedColumnName = "user_id")
    private User actionBy;

    /**
     * When the approval action was performed
     */
    @Column(name = "action_at")
    private Date actionAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    /**
     * Auto-manage timestamps
     */
    @PrePersist
    public void prePersist() {
        this.createdAt = new Date();
    }
}