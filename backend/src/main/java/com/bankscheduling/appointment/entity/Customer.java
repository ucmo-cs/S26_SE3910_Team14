package com.bankscheduling.appointment.entity;

import com.bankscheduling.appointment.entity.converter.PiiEncryptionConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Maps to {@code customers} (V1__init_schema.sql). PII columns use {@link PiiEncryptionConverter}.
 */
@Entity
@Table(name = "customers")
public class Customer extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "external_reference", length = 64)
    private String externalReference;

    @Convert(converter = PiiEncryptionConverter.class)
    @Column(name = "full_name_cipher", nullable = false, columnDefinition = "TEXT")
    private String fullName;

    @Convert(converter = PiiEncryptionConverter.class)
    @Column(name = "email_cipher", nullable = false, columnDefinition = "TEXT")
    private String email;

    @Convert(converter = PiiEncryptionConverter.class)
    @Column(name = "phone_cipher", columnDefinition = "TEXT")
    private String phone;

    @Convert(converter = PiiEncryptionConverter.class)
    @Column(name = "address_line1_cipher", columnDefinition = "TEXT")
    private String addressLine1;

    @Convert(converter = PiiEncryptionConverter.class)
    @Column(name = "address_line2_cipher", columnDefinition = "TEXT")
    private String addressLine2;

    @Convert(converter = PiiEncryptionConverter.class)
    @Column(name = "city_cipher", columnDefinition = "TEXT")
    private String city;

    @Convert(converter = PiiEncryptionConverter.class)
    @Column(name = "state_province_cipher", columnDefinition = "TEXT")
    private String stateProvince;

    @Convert(converter = PiiEncryptionConverter.class)
    @Column(name = "postal_code_cipher", columnDefinition = "TEXT")
    private String postalCode;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStateProvince() {
        return stateProvince;
    }

    public void setStateProvince(String stateProvince) {
        this.stateProvince = stateProvince;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
}
