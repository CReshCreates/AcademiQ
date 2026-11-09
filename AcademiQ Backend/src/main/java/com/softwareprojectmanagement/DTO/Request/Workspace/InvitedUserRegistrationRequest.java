
package com.softwareprojectmanagement.DTO.Request.Workspace;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InvitedUserRegistrationRequest(

        @NotBlank(message = "Invitation token is required.")
        String token,

        @NotBlank(message = "Full name is required.")
        @Size(max = 150)
        String fullName,

        @NotBlank(message = "Password is required.")
        @Size(min = 8)
        String password

) {
}
