package com.bankscheduling.appointment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Maps to {@code roles} (V1__init_schema.sql).
 */
@Entity
@Table(name = "roles")
public class Role extends AuditableEntity {

    @Column(name = "name", nullable = false, unique = true, length = 64)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
