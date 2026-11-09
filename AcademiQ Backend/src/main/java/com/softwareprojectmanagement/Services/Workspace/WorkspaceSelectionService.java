
package com.softwareprojectmanagement.Services.Workspace;

import com.softwareprojectmanagement.DTO.Response.Workspace.WorkspaceSelectionResponse;
import com.softwareprojectmanagement.Models.User;
import com.softwareprojectmanagement.Models.Workspace;
import com.softwareprojectmanagement.Models.WorkspaceMembership;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkspaceSelectionService {

    private final WorkspaceAccessService workspaceAccessService;

    // ==========================================
    // VALIDATE WORKSPACE SELECTION
    // ==========================================

    @Transactional(readOnly = true)
    public WorkspaceSelectionResponse selectWorkspace(
            Integer workspaceId
    ) {

        // STEP 1: Identify the authenticated user.
        User currentUser =
                workspaceAccessService.getAuthenticatedUser();

        // STEP 2: Verify active membership.
        // This also checks that the workspace is active.
        WorkspaceMembership membership =
                workspaceAccessService.requireActiveMembership(
                        currentUser.getUserId(),
                        workspaceId
                );

        // STEP 3: Retrieve the authorized workspace.
        Workspace workspace = membership.getWorkspace();

        // STEP 4: Return workspace information.
        return new WorkspaceSelectionResponse(
                workspace.getWorkspaceId(),
                workspace.getName(),
                workspace.getSlug(),
                workspace.getWorkspaceType(),
                membership.getRole()
        );
    }
}
