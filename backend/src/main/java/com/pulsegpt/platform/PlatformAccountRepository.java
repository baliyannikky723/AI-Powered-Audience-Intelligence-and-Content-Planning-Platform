package com.pulsegpt.platform;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlatformAccountRepository extends JpaRepository<PlatformAccount, UUID> {

    List<PlatformAccount> findByUserId(UUID userId);

    List<PlatformAccount> findAllByUserId(UUID userId);

    List<PlatformAccount> findByUserIdAndStatus(UUID userId, PlatformAccountStatus status);

    Optional<PlatformAccount> findByIdAndUserId(UUID id, UUID userId);

    Optional<PlatformAccount> findByUserIdAndPlatformAndExternalAccountId(UUID userId, PlatformType platform, String externalAccountId);

    boolean existsByUserIdAndPlatformAndExternalAccountId(UUID userId, PlatformType platform, String externalAccountId);
}
