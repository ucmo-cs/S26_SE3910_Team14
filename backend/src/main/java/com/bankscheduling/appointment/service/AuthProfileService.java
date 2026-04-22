package com.bankscheduling.appointment.service;

import com.bankscheduling.appointment.dto.auth.AuthProfileResponse;
import com.bankscheduling.appointment.entity.Customer;
import com.bankscheduling.appointment.entity.CustomerAccount;
import com.bankscheduling.appointment.entity.Employee;
import com.bankscheduling.appointment.repository.CustomerAccountRepository;
import com.bankscheduling.appointment.repository.EmployeeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthProfileService {
    private final CustomerAccountRepository customerAccountRepository;
    private final EmployeeRepository employeeRepository;

    public AuthProfileService(
            CustomerAccountRepository customerAccountRepository,
            EmployeeRepository employeeRepository
    ) {
        this.customerAccountRepository = customerAccountRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public AuthProfileResponse currentProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        Long principalId;
        try {
            principalId = Long.parseLong(authentication.getName());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication principal");
        }

        return customerAccountRepository.findByIdWithCustomer(principalId)
                .map(this::toCustomerProfile)
                .orElseGet(() -> employeeRepository.findByIdWithBranchAndRole(principalId)
                        .map(this::toEmployeeProfile)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found")));
    }

    private AuthProfileResponse toCustomerProfile(CustomerAccount account) {
        Customer customer = account.getCustomer();
        String[] split = splitName(customer.getFullName());
        return new AuthProfileResponse(
                customer.getId(),
                split[0],
                split[1],
                customer.getFullName(),
                customer.getEmail(),
                normalizeRole(account.getRole().getName())
        );
    }

    private AuthProfileResponse toEmployeeProfile(Employee employee) {
        String fullName = employee.getFirstName() + " " + employee.getLastName();
        return new AuthProfileResponse(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                fullName,
                employee.getWorkEmail(),
                normalizeRole(employee.getRole().getName())
        );
    }

    private static String normalizeRole(String roleName) {
        if (roleName == null) return "CUSTOMER";
        return roleName.startsWith("ROLE_") ? roleName.substring("ROLE_".length()) : roleName;
    }

    private static String[] splitName(String fullName) {
        String safe = fullName == null ? "" : fullName.trim();
        int idx = safe.indexOf(' ');
        if (idx <= 0) {
            return new String[]{safe, ""};
        }
        return new String[]{safe.substring(0, idx), safe.substring(idx + 1).trim()};
    }
}
