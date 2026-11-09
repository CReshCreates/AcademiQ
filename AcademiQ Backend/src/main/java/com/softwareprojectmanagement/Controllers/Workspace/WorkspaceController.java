
package com.softwareprojectmanagement.Controllers.Workspace;

import com.softwareprojectmanagement.DTO.Request.Workspace.WorkspaceCreationRequest;
import com.softwareprojectmanagement.DTO.Request.Workspace.WorkspaceRegistrationRequest;
import com.softwareprojectmanagement.DTO.Response.Workspace.MyWorkspaceResponse;
import com.softwareprojectmanagement.DTO.Response.Workspace.WorkspaceSelectionResponse;
import com.softwareprojectmanagement.Services.Workspace.WorkspaceRegistrationService;

import com.softwareprojectmanagement.Services.Workspace.WorkspaceSelectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceRegistrationService workspaceRegistrationService;
    private final WorkspaceSelectionService workspaceSelectionService;

    // ==========================================
    // REGISTER A NEW WORKSPACE AND FOUNDER ADMIN
    // ==========================================

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerWorkspace(
            @Valid @RequestBody WorkspaceRegistrationRequest request
    ) {

        Integer workspaceId =
                workspaceRegistrationService.registerFounder(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "message",
                        "Workspace and founder account created successfully.",
                        "workspaceId",
                        workspaceId
                ));
    }


    // ==========================================
    // GET LOGGED-IN USER'S WORKSPACES
    // ==========================================

    @GetMapping("/my")
    public ResponseEntity<List<MyWorkspaceResponse>>
    getMyWorkspaces() {

        List<MyWorkspaceResponse> workspaces =
                workspaceRegistrationService.getMyWorkspaces();

        return ResponseEntity.ok(workspaces);
    }


    // ==========================================
    // VALIDATE A USER'S WORKSPACE SELECTION
    // ==========================================

    @GetMapping("/{workspaceId}/selection")
    public ResponseEntity<WorkspaceSelectionResponse>
    selectWorkspace(
            @PathVariable Integer workspaceId
    ) {

        WorkspaceSelectionResponse response =
                workspaceSelectionService.selectWorkspace(
                        workspaceId
                );

        return ResponseEntity.ok(response);
    }


    // ==========================================
    // EXISTING ACCOUNT CREATES A NEW WORKSPACE
    // ==========================================

    @PostMapping
    public ResponseEntity<Map<String, Object>>
    createWorkspace(
            @Valid @RequestBody
            WorkspaceCreationRequest request
    ) {

        Integer workspaceId =
                workspaceRegistrationService
                        .createWorkspaceForExistingUser(
                                request
                        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "message",
                        "Workspace created successfully.",
                        "workspaceId",
                        workspaceId
                ));
    }
}
