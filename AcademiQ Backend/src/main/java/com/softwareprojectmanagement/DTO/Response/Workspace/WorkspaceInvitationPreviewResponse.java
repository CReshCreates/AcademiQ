
package com.softwareprojectmanagement.DTO.Response.Workspace;

import com.softwareprojectmanagement.Models.Workspace;
import com.softwareprojectmanagement.Models.WorkspaceMembership;

import java.time.LocalDateTime;

public record WorkspaceInvitationPreviewResponse(

        Integer workspaceId,

        String workspaceName,

        Workspace.WorkspaceType workspaceType,

        String invitedEmail,

        WorkspaceMembership.WorkspaceRole role,

        Integer programmeId,

        String programmeName,

        Integer sectionId,

        String sectionName,

        String batchName,

        LocalDateTime expiresAt

) {
}
