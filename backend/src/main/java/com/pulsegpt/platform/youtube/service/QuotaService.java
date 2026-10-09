package com.pulsegpt.platform.youtube.service;

import com.pulsegpt.admin.ApiQuotaUsage;
import com.pulsegpt.admin.ApiQuotaUsageRepository;
import com.pulsegpt.audit.AuditService;
import com.pulsegpt.common.exception.ApiException;
import com.pulsegpt.config.AppProperties;
import com.pulsegpt.platform.PlatformAccount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaService {

    private final ApiQuotaUsageRepository quotaUsageRepository;
    private final AppProperties appProperties;
    private final AuditService auditService;

    public static final int COST_CHANNEL_LIST = 1;
    public static final int COST_PLAYLIST_ITEMS_LIST = 1;
    public static final int COST_VIDEOS_LIST = 1;
    public static final int COST_COMMENT_THREADS_LIST = 1;
    public static final int COST_SEARCH_LIST = 100;

    @Transactional(readOnly = true)
    public void checkQuotaAvailable(PlatformAccount account, int estimatedUnits) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        long limit = appProperties.getYoutube().getQuotaLimit();
        int stopThreshold = appProperties.getYoutube().getQuotaStopThreshold();

        ApiQuotaUsage usage = quotaUsageRepository.findByPlatformAccountIdAndUsageDate(account.getId(), today)
                .orElse(null);

        long currentUsed = usage != null ? usage.getQuotaUsed() : 0L;
        long stopLimit = (limit * stopThreshold) / 100;

        if (currentUsed + estimatedUnits > stopLimit) {
            log.warn("YouTube API quota safety stop reached for account: {}. Used: {}/{}, threshold: {}%",
                    account.getId(), currentUsed, limit, stopThreshold);
            auditService.logSecurityEvent(account.getUser(), "YOUTUBE_QUOTA_LIMIT_REACHED", "PLATFORM_ACCOUNT",
                    account.getId().toString(), "Quota limit reached (" + currentUsed + "/" + limit + ")");
            throw new ApiException("Daily YouTube API quota threshold reached. Ingestion halted for today.",
                    HttpStatus.TOO_MANY_REQUESTS, "YOUTUBE_QUOTA_EXCEEDED");
        }
    }

    @Transactional
    public void recordQuotaUsage(PlatformAccount account, String operation, int unitsUsed) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        long limit = appProperties.getYoutube().getQuotaLimit();
        int warningThreshold = appProperties.getYoutube().getQuotaWarningThreshold();

        ApiQuotaUsage usage = quotaUsageRepository.findByPlatformAccountIdAndUsageDate(account.getId(), today)
                .orElseGet(() -> ApiQuotaUsage.builder()
                        .platformAccount(account)
                        .usageDate(today)
                        .quotaLimit(limit)
                        .quotaUsed(0L)
                        .warningThreshold(warningThreshold)
                        .build());

        long oldUsed = usage.getQuotaUsed();
        long newUsed = oldUsed + unitsUsed;
        usage.setQuotaUsed(newUsed);
        usage.setQuotaLimit(limit);
        usage.setWarningThreshold(warningThreshold);
        quotaUsageRepository.save(usage);

        long warningLimit = (limit * warningThreshold) / 100;
        if (oldUsed < warningLimit && newUsed >= warningLimit) {
            log.warn("YouTube API quota warning threshold ({}%) reached for account: {}. Used: {}/{}",
                    warningThreshold, account.getId(), newUsed, limit);
            auditService.logAuthEvent(account.getUser(), "YOUTUBE_QUOTA_WARNING", account.getId().toString(),
                    Map.of("quotaUsed", newUsed, "quotaLimit", limit, "thresholdPercent", warningThreshold));
        }
    }

    @Transactional(readOnly = true)
    public long getQuotaUsedToday(PlatformAccount account) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return quotaUsageRepository.findByPlatformAccountIdAndUsageDate(account.getId(), today)
                .map(ApiQuotaUsage::getQuotaUsed)
                .orElse(0L);
    }
}
