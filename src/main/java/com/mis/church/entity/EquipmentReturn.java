package com.mis.church.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;


@Entity
@Table(name = "equipment_returns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Allocation being returned
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_id", nullable = false)
    private EquipmentAllocation allocation;

    /**
     * Parent return request
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_requests_id")
    private ReturnRequest returnRequest;

    /**
     * Who requested the return
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @Column(name = "request_date")
    private Date requestDate;

    /**
     * Status of this return (PENDING, APPROVED, REJECTED)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status", referencedColumnName = "status_id")
    private Status status;

    @Column(name = "comments", length = 2000)
    private String comments;

    @Column(name = "created_at", updatable = false)
    private Date createdAt;

    /**
     * Auto timestamps
     */
    @PrePersist
    public void prePersist() {
        this.createdAt = new Date();

        if (this.requestDate == null) {
            this.requestDate = new Date();
        }
    }
}