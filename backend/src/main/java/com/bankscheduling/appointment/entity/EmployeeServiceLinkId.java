package com.bankscheduling.appointment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class EmployeeServiceLinkId implements Serializable {

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "service_type_id", nullable = false)
    private Long serviceTypeId;

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public Long getServiceTypeId() {
        return serviceTypeId;
    }

    public void setServiceTypeId(Long serviceTypeId) {
        this.serviceTypeId = serviceTypeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EmployeeServiceLinkId that)) {
            return false;
        }
        return Objects.equals(employeeId, that.employeeId) && Objects.equals(serviceTypeId, that.serviceTypeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(employeeId, serviceTypeId);
    }
}
