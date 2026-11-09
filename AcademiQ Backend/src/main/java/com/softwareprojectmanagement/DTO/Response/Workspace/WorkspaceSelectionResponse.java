
package com.softwareprojectmanagement.DTO.Response.Workspace;

import com.softwareprojectmanagement.Models.Workspace;
import com.softwareprojectmanagement.Models.WorkspaceMembership;

public record WorkspaceSelectionResponse(

        Integer workspaceId,

        String workspaceName,

        String workspaceSlug,

        Workspace.WorkspaceType workspaceType,

        WorkspaceMembership.WorkspaceRole role

) {
}
