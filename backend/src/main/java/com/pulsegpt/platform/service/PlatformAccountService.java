package com.pulsegpt.platform.service;

import com.pulsegpt.audit.AuditService;
import com.pulsegpt.common.exception.ResourceNotFoundException;
import com.pulsegpt.platform.PlatformAccount;
import com.pulsegpt.platform.PlatformAccountRepository;
import com.pulsegpt.platform.PlatformAccountStatus;
import com.pulsegpt.platform.dto.PlatformAccountResponse;
import com.pulsegpt.platform.dto.UpdatePlatformAccountStatusRequest;
import com.pulsegpt.platform.mapper.PlatformAccountMapper;
import com.pulsegpt.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformAccountService {

    private final PlatformAccountRepository platformAccountRepository;
    private final PlatformAccountMapper platformAccountMapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<PlatformAccountResponse> getAllAccounts(User currentUser) {
        List<PlatformAccount> accounts = platformAccountRepository.findByUserId(currentUser.getId());
        return platformAccountMapper.toResponseList(accounts);
    }

    @Transactional(readOnly = true)
    public PlatformAccountResponse getAccountById(UUID accountId, User currentUser) {
        PlatformAccount account = findUserAccountOrThrow(accountId, currentUser.getId());
        auditService.logAuthEvent(currentUser, "PLATFORM_ACCOUNT_VIEW", accountId.toString(),
                Map.of("platform", account.getPlatform().name()));
        return platformAccountMapper.toResponse(account);
    }

    @Transactional
    public PlatformAccountResponse updateStatus(UUID accountId, UpdatePlatformAccountStatusRequest request, User currentUser) {
        PlatformAccount account = findUserAccountOrThrow(accountId, currentUser.getId());
        PlatformAccountStatus oldStatus = account.getStatus();
        PlatformAccountStatus newStatus = request.status();

        account.setStatus(newStatus);
        if (newStatus == PlatformAccountStatus.DISCONNECTED) {
            account.setDisconnectedAt(Instant.now());
        } else if (newStatus == PlatformAccountStatus.CONNECTED) {
            account.setDisconnectedAt(null);
        }

        PlatformAccount updated = platformAccountRepository.save(account);
        auditService.logAuthEvent(currentUser, "PLATFORM_ACCOUNT_STATUS_CHANGED", accountId.toString(),
                Map.of("oldStatus", oldStatus.name(), "newStatus", newStatus.name()));

        return platformAccountMapper.toResponse(updated);
    }

    @Transactional
    public void deleteAccount(UUID accountId, User currentUser) {
        PlatformAccount account = findUserAccountOrThrow(accountId, currentUser.getId());

        // Soft disconnect transition preferred for historical analytics retention
        account.setStatus(PlatformAccountStatus.DISCONNECTED);
        account.setDisconnectedAt(Instant.now());
        account.setAccessTokenEncrypted(null);
        account.setRefreshTokenEncrypted(null);
        platformAccountRepository.save(account);

        auditService.logAuthEvent(currentUser, "PLATFORM_ACCOUNT_DISCONNECTED", accountId.toString(),
                Map.of("platform", account.getPlatform().name()));
    }

    public PlatformAccount findUserAccountOrThrow(UUID accountId, UUID userId) {
        return platformAccountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Platform account not found with id: " + accountId));
    }
}
