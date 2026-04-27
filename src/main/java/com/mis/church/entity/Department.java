package com.mis.church.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;


@Entity
@Table(name = "department")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "department_name", nullable = false, length = 50)
    private String departmentName;

    @Column(name = "department_description", length = 255)
    private String departmentDescription;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "date_added")
    private Date dateAdded;

    @Column(name = "date_approved")
    private Date dateApproved;

    @Column(name = "date_updated")
    private Date dateUpdated;

    // 🔗 Foreign Key: status_id → status.status_id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id")
    private Status status;

    // 🔗 Foreign Key: department_head → roles.role_id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_head")
    private Role departmentHead;


    /**
     * Auto-manage timestamps
     */
    @PrePersist
    public void prePersist() {
        this.dateAdded = new Date();
        this.dateUpdated = new Date();
    }

    @PreUpdate
    public void preUpdate() {
        this.dateUpdated = new Date();
    }
}