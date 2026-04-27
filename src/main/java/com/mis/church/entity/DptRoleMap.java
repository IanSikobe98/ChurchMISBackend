package com.mis.church.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;


@Entity
@Table(name = "dpt_role_map")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DptRoleMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 FK: department_id → department.id
    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    // 🔗 FK: role_id → roles.role_id
    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;

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

    // 🔗 FK: status_id → status.status_id
    @ManyToOne
    @JoinColumn(name = "status_id")
    private Status status;


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