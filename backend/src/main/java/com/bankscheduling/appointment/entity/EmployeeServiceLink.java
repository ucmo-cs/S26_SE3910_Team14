package com.bankscheduling.appointment.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "employee_services")
public class EmployeeServiceLink {

    @EmbeddedId
    private EmployeeServiceLinkId id = new EmployeeServiceLinkId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("employeeId")
    @JoinColumn(name = "employee_id")
    private User employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("serviceTypeId")
    @JoinColumn(name = "service_type_id")
    private ServiceType serviceType;

    public EmployeeServiceLinkId getId() {
        return id;
    }

    public void setId(EmployeeServiceLinkId id) {
        this.id = id;
    }

    public User getEmployee() {
        return employee;
    }

    public void setEmployee(User employee) {
        this.employee = employee;
        if (id != null && employee != null && employee.getId() != null) {
            id.setEmployeeId(employee.getId());
        }
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
        if (id != null && serviceType != null && serviceType.getId() != null) {
            id.setServiceTypeId(serviceType.getId());
        }
    }
}
