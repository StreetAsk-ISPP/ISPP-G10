package com.streetask.app.user;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.streetask.app.business.BusinessAccount;
import com.streetask.app.business.BusinessAccountRepository;
import com.streetask.app.exceptions.AccessDeniedException;
import com.streetask.app.exceptions.ResourceNotFoundException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class UserTypeChangeService {

    private static final float DEFAULT_VISIBILITY_RADIUS_KM = 10.0f;

    @PersistenceContext
    private EntityManager entityManager;

    private final UserRepository userRepository;
    private final RegularUserRepository regularUserRepository;
    private final BusinessAccountRepository businessAccountRepository;
    private final AuthoritiesService authoritiesService;
    private final UserRoleChangeLogRepository roleChangeLogRepository;
    private final UserService userService;

    @Autowired
    public UserTypeChangeService(UserRepository userRepository,
            RegularUserRepository regularUserRepository,
            BusinessAccountRepository businessAccountRepository,
            AuthoritiesService authoritiesService,
            UserRoleChangeLogRepository roleChangeLogRepository,
            UserService userService) {
        this.userRepository = userRepository;
        this.regularUserRepository = regularUserRepository;
        this.businessAccountRepository = businessAccountRepository;
        this.authoritiesService = authoritiesService;
        this.roleChangeLogRepository = roleChangeLogRepository;
        this.userService = userService;
    }

    /**
     * Validates if a role transition is allowed.
     * Allowed transitions:
     * - REGULAR_USER ↔ BUSINESS (only after caller-scope checks)
     * - NOT: REGULAR_USER/BUSINESS → ADMIN (admins cannot self-promote)
     * - NOT: ADMIN → anything (no downgrade from admin)
     */
    public void validateTransition(AccountType fromType, AccountType toType, boolean isAdmin) {
        if (fromType == null) {
            throw new IllegalArgumentException("Current account type cannot be null");
        }

        if (toType == null) {
            throw new IllegalArgumentException("New account type cannot be null");
        }

        if (fromType == toType) {
            throw new IllegalArgumentException("User is already " + toType);
        }

        // ADMIN users cannot change their role
        if (fromType == AccountType.ADMIN) {
            throw new AccessDeniedException("Admin users cannot change their account type.");
        }

        // Users cannot promote themselves to ADMIN
        if (toType == AccountType.ADMIN) {
            throw new AccessDeniedException("Cannot promote user to admin via this endpoint.");
        }

        // Only allowed: REGULAR_USER ↔ BUSINESS
        if ((fromType == AccountType.REGULAR_USER && toType == AccountType.BUSINESS) ||
                (fromType == AccountType.BUSINESS && toType == AccountType.REGULAR_USER)) {
            return; // Valid transition
        }

        throw new IllegalArgumentException("Invalid role transition: " + fromType + " → " + toType);
    }

    /**
     * Changes user's account type and authority.
     * Handles data migration between subclass tables and cleans FK orphans.
     * 
     * SECURITY: This method is transactional and validates transitions.
     * If any step fails, entire change is rolled back.
     */
    @Transactional
    public void changeAccountType(User user, AccountType newAccountType, UUID changedByUserId,
            String reason, String ipAddress) {
        AccountType currentType = user.getAccountType();

        // Validate transition
        User changedByUser = null;
        if (changedByUserId != null) {
            changedByUser = userRepository.findById(changedByUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", changedByUserId));
            if (!changedByUser.hasAuthority("ADMIN")) {
                throw new AccessDeniedException("Only admins can change other users' roles.");
            }

            // SECURITY: Restrict admins: ADMIN cannot change to BUSINESS
            // Only downgrade from BUSINESS to REGULAR can be done by ADMIN for this user
            if (currentType == AccountType.BUSINESS && newAccountType == AccountType.REGULAR_USER) {
                // Admin downgrade is allowed (for cleanup) but always logged
                if (reason == null) {
                    reason = "Admin-initiated downgrade";
                }
            }
        } else {
            // Product rule: account type transitions are not a normal self-service flow.
            if (currentType == AccountType.REGULAR_USER && newAccountType == AccountType.BUSINESS) {
                throw new AccessDeniedException(
                        "Regular users cannot change their account type to Business from this endpoint. Please use business signup.");
            }

            // SECURITY: User self-change: Cannot downgrade from BUSINESS to REGULAR
            if (currentType == AccountType.BUSINESS && newAccountType == AccountType.REGULAR_USER) {
                throw new AccessDeniedException(
                        "Cannot downgrade from Business to Regular User. Please contact support if you need to close your business account.");
            }
        }

        validateTransition(currentType, newAccountType, changedByUser != null && changedByUser.hasAuthority("ADMIN"));

        String previousAuthority = user.getAuthority().getAuthority();
        String newAuthority = null;

        if (newAccountType == AccountType.REGULAR_USER) {
            newAuthority = "USER";
            convertToRegularUser(user);
        } else if (newAccountType == AccountType.BUSINESS) {
            newAuthority = "BUSINESS";
            convertToBusinessUser(user);
        }

        // Log the change
        UserRoleChangeLog log = new UserRoleChangeLog(
                user.getId(),
                currentType != null ? currentType.toString() : "NONE",
                newAccountType.toString(),
                previousAuthority,
                newAuthority,
                changedByUserId != null ? changedByUserId.toString() : "SELF",
                reason,
                ipAddress);
        roleChangeLogRepository.save(log);
    }

    /**
     * Convert user to RegularUser.
     * Handles migration from BusinessAccount to RegularUser.
     * Uses UPDATE strategy instead of DELETE+INSERT to avoid optimistic locking
     * issues.
     */
    private void convertToRegularUser(User user) {
        // SECURITY: Clean up EventAttendance records that reference this user
        int deletedAttendances = entityManager.createQuery(
                "DELETE FROM EventAttendance ea WHERE ea.regularUser.id = :userId").setParameter("userId", user.getId())
                .executeUpdate();

        if (deletedAttendances > 0) {
            System.out.println("Cleaned " + deletedAttendances + " EventAttendance orphans for user " + user.getId());
        }

        // Update the existing user instance
        user.setAccountType(AccountType.REGULAR_USER);

        // Set authority to USER
        Authorities userAuthority = authoritiesService.findByAuthority("USER");
        user.setAuthority(userAuthority);

        // For BusinessAccount specific fields, set them to null/defaults
        if (user instanceof BusinessAccount) {
            BusinessAccount ba = (BusinessAccount) user;
            ba.setCompanyName(null);
            ba.setTaxId(null);
            ba.setAddress(null);
            ba.setWebsite(null);
            ba.setDescription(null);
            ba.setLogo(null);
            ba.setVerified(false);
            ba.setVerifiedAt(null);
            ba.setVerifiedBy(null);
            ba.setRating(0.0f);
            ba.setRequestStatus(null);
            ba.setSubscriptionActive(false);
            ba.setSubscriptionExpiresAt(null);
            ba.setRejectionReason(null);

        }

        // Save updated user
        userRepository.save(user);
    }

    /**
     * Convert user to BusinessAccount.
     * Handles migration from RegularUser to BusinessAccount.
     * Uses UPDATE strategy instead of DELETE+INSERT to avoid optimistic locking
     * issues.
     */
    private void convertToBusinessUser(User user) {
        if (user instanceof BusinessAccount) {
            return; // Already a BusinessAccount
        }

        // Update the existing user instance
        user.setAccountType(AccountType.BUSINESS);

        // Set authority to BUSINESS
        Authorities businessAuthority = authoritiesService.findByAuthority("BUSINESS");
        user.setAuthority(businessAuthority);

        // For RegularUser specific fields, clear them
        if (user instanceof RegularUser) {
            RegularUser ru = (RegularUser) user;
            ru.setPhone(null);
            ru.setProfilePhoto(null);
            ru.setCoinBalance(0);
            ru.setRating(0.0f);
            ru.setTotalLikesReceived(0);
            ru.setTotalDislikesReceived(0);
            ru.setVerified(false);
            ru.setVisibilityRadiusKm(DEFAULT_VISIBILITY_RADIUS_KM);
        }

        // Ensure instance is BusinessAccount after update
        user.setAccountType(AccountType.BUSINESS);

        // Set BusinessAccount defaults
        if (!(user instanceof BusinessAccount)) {
            // Convert to BusinessAccount by clearing RegularUser fields
            user.setAccountType(AccountType.BUSINESS);
        }

        // Save updated user
        userRepository.save(user);
    }
}
