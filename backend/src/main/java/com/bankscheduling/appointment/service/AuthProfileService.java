package com.bankscheduling.appointment.service;

import com.bankscheduling.appointment.dto.auth.AuthProfileResponse;
import com.bankscheduling.appointment.entity.User;
import com.bankscheduling.appointment.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthProfileService {
    private final UserRepository userRepository;

    public AuthProfileService(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
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

        User user = userRepository.findByIdWithRole(principalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
        String fullName = user.getFullName() == null || user.getFullName().isBlank()
                ? ((user.getFirstName() == null ? "" : user.getFirstName()) + " " + (user.getLastName() == null ? "" : user.getLastName())).trim()
                : user.getFullName();
        String[] split = splitName(fullName);
        return new AuthProfileResponse(
                user.getId(),
                split[0],
                split[1],
                fullName,
                user.getEmailNormalized(),
                normalizeRole(user.getRole().getName())
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
