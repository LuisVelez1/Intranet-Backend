package com.backendintranet.entity;

import jakarta.persistence.*;

import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "absence_types",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_absence_type_name",
                        columnNames = "name"),
        indexes = {
            @Index(
                    name = "idx_absence_type_active_name",
                    columnList = "active,name")
        })
@Getter
@Setter
@NoArgsConstructor
public class AbsenceType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            nullable = false,
            length = 100)
    private String name;

    @Column(
            length = 500)
    private String description;

    @Column(
            nullable = false)
    private Boolean active = true;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false)
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void create() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;

        if (active == null) {
            active = true;
        }
    }

    @PreUpdate
    void update() {
        updatedAt = LocalDateTime.now();
    }
}
