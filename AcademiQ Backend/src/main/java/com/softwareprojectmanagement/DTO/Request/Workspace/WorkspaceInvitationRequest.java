
package com.softwareprojectmanagement.DTO.Request.Workspace;

import com.softwareprojectmanagement.Models.WorkspaceMembership;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record WorkspaceInvitationRequest(

        @NotBlank(message = "Invitee email is required.")
        @Email(message = "A valid email address is required.")
        @Size(max = 255)
        String email,

        @NotNull(message = "Invitation role is required.")
        WorkspaceMembership.WorkspaceRole role,

        @Positive(message = "Programme ID must be positive.")
        Integer programmeId,

        @Positive(message = "Section ID must be positive.")
        Integer sectionId

) {
}
