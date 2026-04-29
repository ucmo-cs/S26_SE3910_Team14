package com.bankscheduling.appointment.config;

import com.bankscheduling.appointment.entity.Branch;
import com.bankscheduling.appointment.entity.EmployeeServiceLink;
import com.bankscheduling.appointment.entity.Role;
import com.bankscheduling.appointment.entity.User;
import com.bankscheduling.appointment.repository.BranchRepository;
import com.bankscheduling.appointment.repository.EmployeeServiceLinkRepository;
import com.bankscheduling.appointment.repository.RoleRepository;
import com.bankscheduling.appointment.repository.ServiceTypeRepository;
import com.bankscheduling.appointment.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Keeps demo identities deterministic for role-based demos.
 */
@Component
@Order(100)
public class DemoIdentityBootstrap implements ApplicationRunner {
    private static final String DEMO_PASSWORD = "password";

    private final BranchRepository branchRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final EmployeeServiceLinkRepository employeeServiceLinkRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    public DemoIdentityBootstrap(
            BranchRepository branchRepository,
            RoleRepository roleRepository,
            UserRepository userRepository,
            EmployeeServiceLinkRepository employeeServiceLinkRepository,
            ServiceTypeRepository serviceTypeRepository,
            PasswordEncoder passwordEncoder,
            EntityManager entityManager
    ) {
        this.branchRepository = branchRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.employeeServiceLinkRepository = employeeServiceLinkRepository;
        this.serviceTypeRepository = serviceTypeRepository;
        this.passwordEncoder = passwordEncoder;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        alignIdentitySequence("users");

        Branch branch = branchRepository.findByActiveTrueOrderByDisplayNameAsc().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No active branch found for demo identity setup"));

        Role customerRole = findRole("ROLE_CUSTOMER");
        Role employeeRole = findRole("ROLE_EMPLOYEE");
        Role adminRole = findRole("ROLE_ADMIN");

        upsertUser(
                branch,
                customerRole,
                "sally.jane",
                "SALLY",
                "JANE",
                "jej74840@ucmo.edu"
        );
        User employee = upsertUser(
                branch,
                employeeRole,
                "bob.billy",
                "Bob",
                "Billy",
                "jej64114@gmail.com"
        );
        User admin = upsertUser(
                branch,
                adminRole,
                "justin.jenkins",
                "Justin",
                "jenkins",
                "jenkinsj2703@gmail.com"
        );

        employeeServiceLinkRepository.deleteAllInBatch();
        var activeServices = serviceTypeRepository.findByActiveTrueOrderByDisplayNameAsc();
        for (var serviceType : activeServices) {
            employeeServiceLinkRepository.save(createLink(employee, serviceType.getId()));
            employeeServiceLinkRepository.save(createLink(admin, serviceType.getId()));
        }
    }

    private Role findRole(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Required role missing: " + roleName));
    }

    private User upsertUser(
            Branch branch,
            Role role,
            String username,
            String firstName,
            String lastName,
            String email
    ) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        User employee = userRepository.findByEmailNormalized(normalizedEmail)
                .orElseGet(User::new);
        employee.setBranch(branch);
        employee.setRole(role);
        employee.setUsername(username);
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setEmailNormalized(normalizedEmail);
        employee.setFullName(firstName + " " + lastName);
        employee.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        employee.setActive(true);
        employee.setAccountLocked(false);
        employee.setFailedLoginAttempts(0);
        return userRepository.save(employee);
    }

    private EmployeeServiceLink createLink(User employee, Long serviceTypeId) {
        EmployeeServiceLink link = new EmployeeServiceLink();
        link.setEmployee(employee);
        var serviceRef = entityManager.getReference(com.bankscheduling.appointment.entity.ServiceType.class, serviceTypeId);
        link.setServiceType(serviceRef);
        return link;
    }

    private void alignIdentitySequence(String tableName) {
        entityManager.createNativeQuery("""
                        SELECT setval(
                            pg_get_serial_sequence(:tableName, 'id'),
                            COALESCE((SELECT MAX(id) FROM %s), 0),
                            true
                        )
                        """.formatted(tableName))
                .setParameter("tableName", tableName)
                .getSingleResult();
    }
}
