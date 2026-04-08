package com.streetask.app.moderation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.streetask.app.exceptions.AccessDeniedException;
import com.streetask.app.exceptions.ResourceNotFoundException;
import com.streetask.app.business.BusinessAccount;
import com.streetask.app.business.BusinessAccountRepository;
import com.streetask.app.business.RequestStatus;
import com.streetask.app.functionalities.notifications.realtime.FrontendNotificationGateway;
import com.streetask.app.functionalities.notifications.realtime.FrontendNotificationMessage;
import com.streetask.app.user.Admin;
import com.streetask.app.user.User;
import com.streetask.app.user.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BusinessVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(BusinessVerificationService.class);

    private final UserService userService;
    private final BusinessAccountRepository businessAccountRepository;
    private final FrontendNotificationGateway frontendNotificationGateway;

    @Transactional(readOnly = true)
    public List<BusinessVerificationDTO> listByStatus(RequestStatus status) {
        requireAdmin();
        return businessAccountRepository.findAllByRequestStatus(status)
                .stream()
                .map(BusinessVerificationDTO::new)
                .toList();
    }

    @Transactional
    public BusinessVerificationDTO approve(UUID businessId) {
        Admin admin = requireAdmin();
        BusinessAccount account = findBusiness(businessId);

        if (account.getRequestStatus() == RequestStatus.APPROVED) {
            throw new IllegalStateException("Business account is already approved");
        }

        account.setRequestStatus(RequestStatus.APPROVED);
        account.setVerified(true);
        account.setVerifiedAt(LocalDateTime.now());
        account.setVerifiedBy(admin);
        account.setRejectionReason(null);
        businessAccountRepository.save(account);

        logger.info("[BusinessVerification] Admin {} approved business account {}", admin.getId(), businessId);

        sendVerificationNotification(account, "approved",
                "Your business account has been approved. You now have full access to all business features.");

        return new BusinessVerificationDTO(account);
    }

    @Transactional
    public BusinessVerificationDTO reject(UUID businessId, String reason) {
        Admin admin = requireAdmin();
        BusinessAccount account = findBusiness(businessId);

        if (account.getRequestStatus() == RequestStatus.REJECTED) {
            throw new IllegalStateException("Business account is already rejected");
        }

        account.setRequestStatus(RequestStatus.REJECTED);
        account.setVerified(false);
        account.setVerifiedAt(LocalDateTime.now());
        account.setVerifiedBy(admin);
        account.setRejectionReason(reason);
        businessAccountRepository.save(account);

        logger.info("[BusinessVerification] Admin {} rejected business account {}", admin.getId(), businessId);

        String message = reason != null && !reason.isBlank()
                ? "Your business account registration was rejected. Reason: " + reason
                : "Your business account registration was rejected by an administrator.";
        sendVerificationNotification(account, "rejected", message);

        return new BusinessVerificationDTO(account);
    }

    @Transactional(readOnly = true)
    public BusinessVerificationDTO getVerificationStatus(UUID businessId) {
        User currentUser = userService.findCurrentUser();
        if (!(currentUser instanceof BusinessAccount account)) {
            throw new AccessDeniedException("Only business accounts can view verification status");
        }
        if (!account.getId().equals(businessId) && !currentUser.hasAuthority("ADMIN")) {
            throw new AccessDeniedException("You can only view your own verification status");
        }
        BusinessAccount target = findBusiness(businessId);
        return new BusinessVerificationDTO(target);
    }

    @Transactional(readOnly = true)
    public BusinessVerificationDTO getMyVerificationStatus() {
        User currentUser = userService.findCurrentUser();
        if (!(currentUser instanceof BusinessAccount account)) {
            throw new AccessDeniedException("Only business accounts can view verification status");
        }
        return new BusinessVerificationDTO(account);
    }

    private Admin requireAdmin() {
        User currentUser = userService.findCurrentUser();
        if (!currentUser.hasAuthority("ADMIN") || !(currentUser instanceof Admin admin)) {
            throw new AccessDeniedException("Only admins can perform this action");
        }
        return admin;
    }

    private BusinessAccount findBusiness(UUID businessId) {
        return businessAccountRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessAccount", "id", businessId));
    }

    private void sendVerificationNotification(BusinessAccount account, String status, String message) {
        FrontendNotificationMessage wsMessage = FrontendNotificationMessage.builder()
                .type("BUSINESS_VERIFICATION")
                .title("Business Account " + (status.equals("approved") ? "Approved" : "Rejected"))
                .message(message)
                .referenceId(account.getId())
                .referenceType("BUSINESS_VERIFICATION")
                .timestamp(LocalDateTime.now())
                .build();
        frontendNotificationGateway.sendToUser(account.getEmail(), wsMessage);
    }
}
