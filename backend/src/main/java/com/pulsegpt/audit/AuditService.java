package com.pulsegpt.audit;

import com.pulsegpt.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAuthEvent(User user, String action, String resourceId, Map<String, ?> metadata) {
        logAuditEvent(user, action, "AUTH", resourceId, metadata);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAuditEvent(User user, String action, String resourceId, Map<String, ?> metadata) {
        logAuditEvent(user, action, "GENERAL", resourceId, metadata);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAuditEvent(User user, String action, String resourceType, String resourceId, Map<String, ?> metadata) {
        try {
            String correlationId = MDC.get("correlationId");
            Map<String, Object> safeMetadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();

            AuditLog auditLog = AuditLog.builder()
                    .user(user)
                    .action(action)
                    .resourceType(resourceType != null ? resourceType : "GENERAL")
                    .resourceId(resourceId)
                    .metadata(safeMetadata)
                    .correlationId(correlationId)
                    .build();

            auditLogRepository.save(auditLog);
            log.info("Audit: action={}, userId={}, resourceId={}", action, user != null ? user.getId() : "anonymous", resourceId);
        } catch (Exception e) {
            log.error("Failed to record audit log: action={}, error={}", action, e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSecurityEvent(User user, String action, String reason) {
        logSecurityEvent(user, action, "SECURITY", user != null ? user.getId().toString() : "SYSTEM", reason);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSecurityEvent(User user, String action, String resourceType, String resourceId, String reason) {
        try {
            String correlationId = MDC.get("correlationId");
            Map<String, Object> metadata = Map.of("reason", reason);

            AuditLog auditLog = AuditLog.builder()
                    .user(user)
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .metadata(metadata)
                    .correlationId(correlationId)
                    .build();

            auditLogRepository.save(auditLog);
            log.warn("Security Event: action={}, userId={}, reason={}", action, user != null ? user.getId() : "anonymous", reason);
        } catch (Exception e) {
            log.error("Failed to record security audit log: action={}, error={}", action, e.getMessage());
        }
    }
}
