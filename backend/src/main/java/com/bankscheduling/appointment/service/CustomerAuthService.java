package com.bankscheduling.appointment.service;

import com.bankscheduling.appointment.dto.customerauth.CustomerAppointmentDto;
import com.bankscheduling.appointment.dto.customerauth.CustomerAuthRequest;
import com.bankscheduling.appointment.dto.customerauth.CustomerAuthResponse;
import com.bankscheduling.appointment.dto.customerauth.CustomerProfileDto;
import com.bankscheduling.appointment.dto.customerauth.CustomerRegisterRequest;
import com.bankscheduling.appointment.entity.Customer;
import com.bankscheduling.appointment.entity.CustomerAccount;
import com.bankscheduling.appointment.entity.Role;
import com.bankscheduling.appointment.repository.AppointmentRepository;
import com.bankscheduling.appointment.repository.CustomerAccountRepository;
import com.bankscheduling.appointment.repository.CustomerRepository;
import com.bankscheduling.appointment.repository.RoleRepository;
import com.bankscheduling.appointment.security.jwt.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CustomerAuthService {
    private final CustomerAccountRepository customerAccountRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AppointmentRepository appointmentRepository;
    private final RoleRepository roleRepository;

    public CustomerAuthService(
            CustomerAccountRepository customerAccountRepository,
            CustomerRepository customerRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            AppointmentRepository appointmentRepository,
            RoleRepository roleRepository
    ) {
        this.customerAccountRepository = customerAccountRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.appointmentRepository = appointmentRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional
    public CustomerAuthResponse register(CustomerRegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        if (customerAccountRepository.findByEmailNormalized(normalizedEmail).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with that email already exists");
        }

        Customer customer = new Customer();
        customer.setFullName(request.firstName().trim() + " " + request.lastName().trim());
        customer.setEmail(normalizedEmail);
        customerRepository.save(customer);

        CustomerAccount account = new CustomerAccount();
        account.setCustomer(customer);
        account.setEmailNormalized(normalizedEmail);
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.setRole(resolveRoleOrThrow("ROLE_CUSTOMER"));
        customerAccountRepository.save(account);

        String token = jwtTokenProvider.createAccessToken(account.getId(), List.of(account.getRole().getName()));
        return new CustomerAuthResponse(token, toCustomerProfile(account));
    }

    @Transactional(readOnly = true)
    public CustomerAuthResponse login(CustomerAuthRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        CustomerAccount account = customerAccountRepository.findByEmailNormalized(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!account.isActive() || !passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        Customer customer = account.getCustomer();
        String token = jwtTokenProvider.createAccessToken(account.getId(), List.of(account.getRole().getName()));
        return new CustomerAuthResponse(token, toCustomerProfile(account));
    }

    @Transactional(readOnly = true)
    public CustomerProfileDto getCurrentCustomerProfile() {
        CustomerAccount account = getCurrentAccount();
        return toCustomerProfile(account);
    }

    @Transactional(readOnly = true)
    public List<CustomerAppointmentDto> getCurrentCustomerAppointments() {
        CustomerAccount account = getCurrentAccount();
        Long customerId = account.getCustomer().getId();
        return appointmentRepository.findAllByCustomerIdWithAssociationsOrdered(customerId).stream()
                .map(appointment -> new CustomerAppointmentDto(
                        appointment.getId(),
                        appointment.getServiceType().getDisplayName(),
                        appointment.getBranch().getDisplayName(),
                        appointment.getScheduledStart(),
                        appointment.getScheduledEnd(),
                        appointment.getStatus().name()
                ))
                .toList();
    }

    private CustomerAccount getCurrentAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        Long accountId;
        try {
            accountId = Long.parseLong(authentication.getName());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication principal");
        }

        return customerAccountRepository.findByIdWithCustomer(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Customer account not found"));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private CustomerProfileDto toCustomerProfile(CustomerAccount account) {
        Customer customer = account.getCustomer();
        String fullName = customer.getFullName() == null ? "" : customer.getFullName().trim();
        String firstName = fullName;
        String lastName = "";
        int firstSpace = fullName.indexOf(' ');
        if (firstSpace > 0) {
            firstName = fullName.substring(0, firstSpace).trim();
            lastName = fullName.substring(firstSpace + 1).trim();
        }

        String role = account.getRole() == null ? "CUSTOMER" : normalizeRoleName(account.getRole().getName());

        return new CustomerProfileDto(
                customer.getId(),
                firstName,
                lastName,
                fullName,
                customer.getEmail(),
                role
        );
    }

    private Role resolveRoleOrThrow(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Required role is missing from database: " + roleName
                ));
    }

    private static String normalizeRoleName(String roleName) {
        if (roleName == null) {
            return "CUSTOMER";
        }
        return roleName.startsWith("ROLE_") ? roleName.substring("ROLE_".length()) : roleName;
    }
}
