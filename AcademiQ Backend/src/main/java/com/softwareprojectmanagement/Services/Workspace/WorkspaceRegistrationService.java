
package com.softwareprojectmanagement.Services.Workspace;

import com.softwareprojectmanagement.DTO.Request.Workspace.WorkspaceCreationRequest;
import com.softwareprojectmanagement.DTO.Request.Workspace.WorkspaceRegistrationRequest;
import com.softwareprojectmanagement.DTO.Response.Workspace.MyWorkspaceResponse;
import com.softwareprojectmanagement.Models.User;
import com.softwareprojectmanagement.Models.Workspace;
import com.softwareprojectmanagement.Models.WorkspaceMembership;
import com.softwareprojectmanagement.Repository.UserRepository;
import com.softwareprojectmanagement.Repository.WorkspaceMembershipRepository;
import com.softwareprojectmanagement.Repository.WorkspaceRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class WorkspaceRegistrationService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final WorkspaceAccessService workspaceAccessService;

    private static final Pattern SLUG_PATTERN =
            Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder(12);

    // ==================================================
    // REGISTER A FOUNDER AND CREATE THEIR NEW WORKSPACE
    // ==================================================

    @Transactional
    public Integer registerFounder(
            WorkspaceRegistrationRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Registration details are required."
            );
        }

        // STEP 1: Normalize and validate submitted details.

        String fullName = requireNonBlank(
                request.fullName(),
                "Full name"
        );

        String email = requireNonBlank(
                request.email(),
                "Email"
        ).toLowerCase(Locale.ROOT);

        String password = request.password();

        String workspaceName = requireNonBlank(
                request.workspaceName(),
                "Workspace name"
        );

        String slug = requireNonBlank(
                request.workspaceSlug(),
                "Workspace slug"
        ).toLowerCase(Locale.ROOT);

        if (request.workspaceType() == null) {
            throw new IllegalArgumentException(
                    "Workspace type is required."
            );
        }

        if (fullName.length() > 150
                || workspaceName.length() > 150
                || slug.length() > 160) {

            throw new IllegalArgumentException(
                    "One or more fields exceed their maximum length."
            );
        }

        if (email.length() > 255
                || !email.matches(
                "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
        )) {

            throw new IllegalArgumentException(
                    "A valid email address is required."
            );
        }

        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters."
            );
        }

        if (!SLUG_PATTERN.matcher(slug).matches()) {
            throw new IllegalArgumentException(
                    "Workspace slug may contain lowercase letters, numbers, and hyphens only."
            );
        }

        // STEP 2: Reject duplicate user accounts.
        // One email must correspond to one user.

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "An account with this email already exists. Sign in to create or join a workspace."
            );
        }

        // STEP 3: Reject duplicate workspace slugs.

        if (workspaceRepository.existsBySlug(slug)) {
            throw new IllegalArgumentException(
                    "This workspace slug is already taken."
            );
        }

        // STEP 4: Create the founder's user account.

        User founder = new User();

        founder.setFullName(fullName);
        founder.setEmail(email);
        founder.setPasswordHash(
                passwordEncoder.encode(password)
        );

        // Temporary compatibility with existing JWT login.
        // Workspace authorization must use memberships,
        // never this global role.
        founder.setRole("ADMIN");

        founder.setCreatedAt(LocalDateTime.now());

        User savedFounder = userRepository.save(founder);

        // STEP 5: Create the new workspace.

        Workspace workspace = new Workspace();

        workspace.setName(workspaceName);
        workspace.setSlug(slug);
        workspace.setWorkspaceType(
                request.workspaceType()
        );
        workspace.setStatus(
                Workspace.WorkspaceStatus.ACTIVE
        );

        Workspace savedWorkspace =
                workspaceRepository.save(workspace);

        // STEP 6: Make the founder an active Admin
        // of the workspace they just created.

        WorkspaceMembership membership =
                new WorkspaceMembership();

        membership.setUser(savedFounder);
        membership.setWorkspace(savedWorkspace);
        membership.setRole(
                WorkspaceMembership.WorkspaceRole.ADMIN
        );
        membership.setStatus(
                WorkspaceMembership.MembershipStatus.ACTIVE
        );

        workspaceMembershipRepository.save(membership);

        // STEP 7: Return the new workspace ID.
        return savedWorkspace.getWorkspaceId();
    }

    // ==================================================
    // SHARED TEXT VALIDATION
    // ==================================================

    private String requireNonBlank(
            String value,
            String fieldName
    ) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " is required."
            );
        }

        return value.trim();
    }


    // ==========================================
    // GET CURRENT USER'S ACTIVE WORKSPACES
    // ==========================================

    @Transactional(readOnly = true)
    public List<MyWorkspaceResponse> getMyWorkspaces() {

        // STEP 1: Identify the authenticated user.
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

        User currentUser = userRepository.findByEmail(
                authentication.getName()
        );

        if (currentUser == null) {
            throw new AccessDeniedException(
                    "Authenticated user not found."
            );
        }

        // STEP 2: Retrieve active memberships.
        List<WorkspaceMembership> memberships =
                workspaceMembershipRepository
                        .findUserMembershipsByStatus(
                                currentUser.getUserId(),
                                WorkspaceMembership.MembershipStatus.ACTIVE
                        );

        // STEP 3: Exclude inactive workspaces and convert memberships into frontend DTOs.
        return memberships.stream()
                .filter(membership ->
                        membership.getWorkspace().getStatus()
                                == Workspace.WorkspaceStatus.ACTIVE
                )
                .map(membership -> {

                    Workspace workspace =
                            membership.getWorkspace();

                    return new MyWorkspaceResponse(
                            workspace.getWorkspaceId(),
                            workspace.getName(),
                            workspace.getSlug(),
                            workspace.getWorkspaceType(),
                            membership.getRole()
                    );
                })
                .sorted(
                        java.util.Comparator.comparing(
                                MyWorkspaceResponse::workspaceName,
                                String.CASE_INSENSITIVE_ORDER
                        )
                )
                .toList();
    }


    // ==========================================
    // EXISTING USER CREATES ANOTHER WORKSPACE
    // ==========================================

    @Transactional
    public Integer createWorkspaceForExistingUser(
            WorkspaceCreationRequest request
    ) {

        // STEP 1: Require an authenticated account.
        User currentUser =
                workspaceAccessService.getAuthenticatedUser();

        if (request == null) {
            throw new IllegalArgumentException(
                    "Workspace details are required."
            );
        }

        // STEP 2: Validate workspace details.
        String workspaceName = requireNonBlank(
                request.workspaceName(),
                "Workspace name"
        );

        String slug = requireNonBlank(
                request.workspaceSlug(),
                "Workspace slug"
        ).toLowerCase(Locale.ROOT);

        if (workspaceName.length() > 150
                || slug.length() > 160) {
            throw new IllegalArgumentException(
                    "Workspace name or slug is too long."
            );
        }

        if (request.workspaceType() == null) {
            throw new IllegalArgumentException(
                    "Workspace type is required."
            );
        }

        if (!SLUG_PATTERN.matcher(slug).matches()) {
            throw new IllegalArgumentException(
                    "Workspace slug may contain lowercase letters, "
                            + "numbers and hyphens only."
            );
        }

        // STEP 3: Verify slug uniqueness.
        if (workspaceRepository.existsBySlug(slug)) {
            throw new IllegalArgumentException(
                    "This workspace slug is already taken."
            );
        }

        // STEP 4: Create a new workspace.
        Workspace workspace = new Workspace();

        workspace.setName(workspaceName);
        workspace.setSlug(slug);
        workspace.setWorkspaceType(
                request.workspaceType()
        );
        workspace.setStatus(
                Workspace.WorkspaceStatus.ACTIVE
        );

        Workspace savedWorkspace =
                workspaceRepository.save(workspace);

        // STEP 5: Make the existing user the founding Admin
        // of this new workspace only.
        WorkspaceMembership membership =
                new WorkspaceMembership();

        membership.setUser(currentUser);
        membership.setWorkspace(savedWorkspace);
        membership.setRole(
                WorkspaceMembership.WorkspaceRole.ADMIN
        );
        membership.setStatus(
                WorkspaceMembership.MembershipStatus.ACTIVE
        );

        workspaceMembershipRepository.save(membership);


        // STEP 6: Return the created workspace ID.
        return savedWorkspace.getWorkspaceId();
    }


}
