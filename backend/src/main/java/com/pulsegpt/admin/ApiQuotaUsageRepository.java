package com.pulsegpt.admin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiQuotaUsageRepository extends JpaRepository<ApiQuotaUsage, UUID> {

    Optional<ApiQuotaUsage> findByPlatformAccountIdAndUsageDate(UUID platformAccountId, LocalDate usageDate);

    List<ApiQuotaUsage> findByPlatformAccountIdOrderByUsageDateDesc(UUID platformAccountId);

    List<ApiQuotaUsage> findByPlatformAccountIdAndUsageDateBetween(UUID platformAccountId, LocalDate start, LocalDate end);
}
