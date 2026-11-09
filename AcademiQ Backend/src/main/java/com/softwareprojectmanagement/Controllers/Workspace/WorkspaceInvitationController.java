
package com.softwareprojectmanagement.Controllers.Workspace;

import com.softwareprojectmanagement.DTO.Request.Workspace.InvitedUserRegistrationRequest;
import com.softwareprojectmanagement.DTO.Request.Workspace.WorkspaceInvitationAcceptanceRequest;
import com.softwareprojectmanagement.DTO.Request.Workspace.WorkspaceInvitationRequest;
import com.softwareprojectmanagement.DTO.Response.Workspace.WorkspaceInvitationPreviewResponse;
import com.softwareprojectmanagement.Services.InvitedUserRegistrationService;
import com.softwareprojectmanagement.Services.Workspace.WorkspaceInvitationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/workspace-invitations")
@RequiredArgsConstructor
public class WorkspaceInvitationController {

    private final WorkspaceInvitationService invitationService;
    private final InvitedUserRegistrationService invitedUserRegistrationService;

    // ADMIN: INVITE A MEMBER INTO A WORKSPACE
    @PostMapping("/workspaces/{workspaceId}")
    public ResponseEntity<Map<String, Object>> createInvitation(
            @PathVariable Integer workspaceId,
            @Valid @RequestBody WorkspaceInvitationRequest request
    ) {

        WorkspaceInvitationService.IssuedInvitation result =
                invitationService.createInvitation(
                        workspaceId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Invitation created successfully.",
                        "invitationId", result.invitationId(),
                        "invitationToken", result.token(),
                        "expiresAt", result.expiresAt().toString()
                ));
    }

    // ADMIN: REVOKE A PENDING INVITATION
    @DeleteMapping("/workspaces/{workspaceId}/{invitationId}")
    public ResponseEntity<Map<String, String>> revokeInvitation(
            @PathVariable Integer workspaceId,
            @PathVariable Integer invitationId
    ) {

        invitationService.revokeInvitation(
                workspaceId,
                invitationId
        );

        return ResponseEntity.ok(
                Map.of("message", "Invitation revoked successfully.")
        );
    }

    // EXISTING USER: ACCEPT AN INVITATION
    @PostMapping("/accept")
    public ResponseEntity<Map<String, Object>> acceptInvitation(
            @Valid @RequestBody WorkspaceInvitationAcceptanceRequest request
    ) {

        Integer workspaceId =
                invitationService.acceptInvitation(
                        request.token()
                );

        return ResponseEntity.ok(
                Map.of(
                        "message", "Invitation accepted successfully.",
                        "workspaceId", workspaceId
                )
        );
    }


    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerInvitedUser(
            @Valid @RequestBody InvitedUserRegistrationRequest request
    ) {
        Integer workspaceId =
                invitedUserRegistrationService.registerInvitedUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "message",
                        "Account created and workspace invitation accepted.",
                        "workspaceId",
                        workspaceId
                ));
    }


    // ==========================================
    // PUBLIC INVITATION PREVIEW
    // ==========================================

    @PostMapping("/preview")
    public ResponseEntity<WorkspaceInvitationPreviewResponse>
    previewInvitation(
            @Valid @RequestBody
            WorkspaceInvitationAcceptanceRequest request
    ) {

        WorkspaceInvitationPreviewResponse response =
                invitationService.previewInvitation(
                        request.token()
                );

        return ResponseEntity.ok()
                .cacheControl(
                        org.springframework.http.CacheControl.noStore()
                )
                .body(response);
    }

}
