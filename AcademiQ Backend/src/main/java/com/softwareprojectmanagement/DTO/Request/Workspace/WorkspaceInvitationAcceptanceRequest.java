
package com.softwareprojectmanagement.DTO.Request.Workspace;

import jakarta.validation.constraints.NotBlank;

public record WorkspaceInvitationAcceptanceRequest(

        @NotBlank(message = "Invitation token is required.")
        String token

) {
}
