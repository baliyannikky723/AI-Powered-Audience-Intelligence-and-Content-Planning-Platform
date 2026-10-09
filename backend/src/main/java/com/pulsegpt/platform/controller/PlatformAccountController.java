package com.pulsegpt.platform.controller;

import com.pulsegpt.common.ApiResponse;
import com.pulsegpt.platform.dto.PlatformAccountResponse;
import com.pulsegpt.platform.dto.UpdatePlatformAccountStatusRequest;
import com.pulsegpt.platform.service.PlatformAccountService;
import com.pulsegpt.security.CurrentUserService;
import com.pulsegpt.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/platform-accounts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Platform Accounts", description = "Endpoints for managing connected social and video platform accounts")
public class PlatformAccountController {

    private final PlatformAccountService platformAccountService;
    private final CurrentUserService currentUserService;

    @GetMapping
    @Operation(summary = "Get all platform accounts for authenticated user")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Accounts retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<List<PlatformAccountResponse>>> getAllAccounts() {
        User currentUser = currentUserService.requireUser();
        List<PlatformAccountResponse> accounts = platformAccountService.getAllAccounts(currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Platform accounts retrieved successfully", accounts));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a platform account by ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Account not found or belongs to another user")
    })
    public ResponseEntity<ApiResponse<PlatformAccountResponse>> getAccountById(@PathVariable UUID id) {
        User currentUser = currentUserService.requireUser();
        PlatformAccountResponse account = platformAccountService.getAccountById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Platform account retrieved successfully", account));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update platform account connection status")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account status updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<ApiResponse<PlatformAccountResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePlatformAccountStatusRequest request
    ) {
        User currentUser = currentUserService.requireUser();
        PlatformAccountResponse updated = platformAccountService.updateStatus(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Platform account status updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Disconnect platform account")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account disconnected"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@PathVariable UUID id) {
        User currentUser = currentUserService.requireUser();
        platformAccountService.deleteAccount(id, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Platform account disconnected successfully", null));
    }
}
