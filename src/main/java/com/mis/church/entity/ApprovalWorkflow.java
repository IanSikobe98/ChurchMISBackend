package com.mis.church.entity;

import jakarta.persistence.*;
import lombok.*;


import java.util.Date;

@Entity
@Table(name = "approval_workflow")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalWorkflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    @Column(name = "created_by", length = 109)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private Date updatedAt;

    @Column(name = "updated_by", length = 109)
    private String updatedBy;

    /**
     * Many workflows can have one status
     */
    @ManyToOne
    @JoinColumn(name = "status", referencedColumnName = "status_id")
    private Status status;


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