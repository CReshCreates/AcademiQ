
package com.softwareprojectmanagement.Services.Workspace;

import com.softwareprojectmanagement.Models.User;
import com.softwareprojectmanagement.Models.Workspace;
import com.softwareprojectmanagement.Models.WorkspaceMembership;
import com.softwareprojectmanagement.Repository.UserRepository;
import com.softwareprojectmanagement.Repository.WorkspaceMembershipRepository;
import com.softwareprojectmanagement.Repository.WorkspaceRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class WorkspaceAccessService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository membershipRepository;

    // ==========================================
    // GET AUTHENTICATED USER
    // ==========================================

    @Transactional(readOnly = true)
    public User getAuthenticatedUser() {

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(
                authentication.getPrincipal()
        )) {

            throw new AccessDeniedException(
                    "Authentication is required."
            );
        }

        User user = userRepository.findByEmail(
                authentication.getName()
        );

        if (user == null) {
            throw new AccessDeniedException(
                    "Authenticated user not found."
            );
        }

        return user;
    }

    // ==========================================
    // REQUIRE ACTIVE WORKSPACE
    // ==========================================

    @Transactional(readOnly = true)
    public Workspace requireActiveWorkspace(
            Integer workspaceId
    ) {

        if (workspaceId == null || workspaceId <= 0) {
            throw new IllegalArgumentException(
                    "Valid workspace ID is required."
            );
        }

        Workspace workspace = workspaceRepository
                .findById(workspaceId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Workspace not found."
                        )
                );

        if (workspace.getStatus()
                != Workspace.WorkspaceStatus.ACTIVE) {

            throw new AccessDeniedException(
                    "Workspace is not active."
            );
        }

        return workspace;
    }

    // ==========================================
    // REQUIRE ACTIVE WORKSPACE MEMBERSHIP
    // ==========================================

    @Transactional(readOnly = true)
    public WorkspaceMembership requireActiveMembership(
            Integer userId,
            Integer workspaceId
    ) {

        requireActiveWorkspace(workspaceId);

        if (userId == null || userId <= 0) {
            throw new AccessDeniedException(
                    "Valid user identity is required."
            );
        }

        return membershipRepository
                .findUserMembershipsByStatus(
                        userId,
                        WorkspaceMembership.MembershipStatus.ACTIVE
                )
                .stream()
                .filter(membership ->
                        workspaceId.equals(
                                membership.getWorkspace()
                                        .getWorkspaceId()
                        )
                )
                .findFirst()
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Active workspace membership is required."
                        )
                );
    }

    // ==========================================
    // REQUIRE SPECIFIC WORKSPACE ROLE
    // ==========================================

    @Transactional(readOnly = true)
    public WorkspaceMembership requireWorkspaceRole(
            Integer userId,
            Integer workspaceId,
            WorkspaceMembership.WorkspaceRole... allowedRoles
    ) {

        WorkspaceMembership membership =
                requireActiveMembership(
                        userId,
                        workspaceId
                );

        if (allowedRoles == null
                || allowedRoles.length == 0
                || Arrays.stream(allowedRoles)
                .noneMatch(role ->
                        role == membership.getRole()
                )) {

            throw new AccessDeniedException(
                    "You do not have the required workspace role."
            );
        }

        return membership;
    }

    // ==========================================
    // REQUIRE CURRENT USER'S WORKSPACE ROLE
    // ==========================================

    @Transactional(readOnly = true)
    public WorkspaceMembership requireCurrentUserRole(
            Integer workspaceId,
            WorkspaceMembership.WorkspaceRole... allowedRoles
    ) {

        User currentUser = getAuthenticatedUser();

        return requireWorkspaceRole(
                currentUser.getUserId(),
                workspaceId,
                allowedRoles
        );
    }
}
