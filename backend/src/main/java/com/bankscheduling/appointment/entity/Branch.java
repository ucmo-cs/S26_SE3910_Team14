package com.bankscheduling.appointment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "branches")
public class Branch extends AuditableEntity {

    @Column(name = "code", nullable = false, unique = true, length = 32)
    private String code;

    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;

    @Column(name = "street_line1")
    private String streetLine1;

    @Column(name = "street_line2")
    private String streetLine2;

    @Column(name = "city", length = 120)
    private String city;

    @Column(name = "state_province", length = 64)
    private String stateProvince;

    @Column(name = "postal_code", length = 32)
    private String postalCode;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "phone_e164", length = 32)
    private String phoneE164;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
