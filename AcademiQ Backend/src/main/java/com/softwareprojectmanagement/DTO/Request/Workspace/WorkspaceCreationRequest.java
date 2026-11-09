
package com.softwareprojectmanagement.DTO.Request.Workspace;

import com.softwareprojectmanagement.Models.Workspace;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WorkspaceCreationRequest(

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
