
package com.softwareprojectmanagement.DTO.Request.Workspace;

import com.softwareprojectmanagement.Models.Workspace;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WorkspaceRegistrationRequest(

        @NotBlank(message = "Full name is required.")
        @Size(max = 150)
        String fullName,

        @NotBlank(message = "Email is required.")
        @Email(message = "Invalid email format.")
        String email,

        @NotBlank(message = "Password is required.")
        @Size(min = 8, message = "Password must contain at least 8 characters.")
        String password,

        @NotBlank(message = "Workspace name is required.")
        @Size(max = 150)
        String workspaceName,

        @NotBlank(message = "Workspace slug is required.")
        @Size(max = 160)
        String workspaceSlug,

        @NotNull(message = "Workspace type is required.")
        Workspace.WorkspaceType workspaceType

) {
}
