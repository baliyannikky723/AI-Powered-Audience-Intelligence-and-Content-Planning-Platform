package com.pulsegpt.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record RegisterRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 10, message = "Password must be at least 10 characters long")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&^#()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{10,}$",
                message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"
        )
        String password,

        @NotBlank(message = "Password confirmation is required")
        String confirmPassword
) {
    public boolean isPasswordConfirmed() {
        return password != null && password.equals(confirmPassword);
    }
}
