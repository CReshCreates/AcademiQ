
package com.softwareprojectmanagement.Services.Workspace;

import com.softwareprojectmanagement.DTO.Request.Workspace.InvitedUserRegistrationRequest;
import com.softwareprojectmanagement.DTO.Request.Workspace.WorkspaceInvitationRequest;
import com.softwareprojectmanagement.DTO.Response.Workspace.WorkspaceInvitationPreviewResponse;
import com.softwareprojectmanagement.Models.*;
import com.softwareprojectmanagement.Repository.*;

import com.softwareprojectmanagement.Services.InvitedUserRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class WorkspaceInvitationService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceInvitationRepository invitationRepository;
    private final WorkspaceMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final ProgrammeRepository programmeRepository;
    private final SectionRepository sectionRepository;
    private final InvitedUserRegistrationService invitedUserRegistrationService;
    private final WorkspaceAccessService workspaceAccessService;


    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    private static final int INVITATION_VALIDITY_DAYS = 7;

    public record IssuedInvitation(
            Integer invitationId,
            String token,
            LocalDateTime expiresAt
    ) {}

    // ==========================================
    // CREATE AN INVITATION
    // ==========================================

    @Transactional
    public IssuedInvitation createInvitation(
            Integer workspaceId,
            WorkspaceInvitationRequest request
    ) {

        User admin = workspaceAccessService.getAuthenticatedUser();

        Workspace workspace =
                workspaceAccessService.requireActiveWorkspace(workspaceId);

        workspaceAccessService.requireWorkspaceRole(
                admin.getUserId(),
                workspaceId,
                WorkspaceMembership.WorkspaceRole.ADMIN
        );


        if (request == null
                || request.email() == null
                || request.email().isBlank()
                || request.role() == null) {
            throw new IllegalArgumentException(
                    "Invitation email and role are required."
            );
        }

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (email.length() > 255
                || !email.matches(
                "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
        )) {
            throw new IllegalArgumentException(
                    "Invalid invitation email."
            );
        }

        validateInvitationRole(workspace, request.role());

        Section section = validateAcademicAssignment(
                workspace,
                request
        );

        // Existing members must not receive another invitation to join the same workspace.
        User existingUser = userRepository.findByEmail(email);

        if (existingUser != null) {
            boolean alreadyMember = membershipRepository
                    .findUserMembershipsByStatus(
                            existingUser.getUserId(),
                            WorkspaceMembership.MembershipStatus.ACTIVE
                    )
                    .stream()
                    .anyMatch(m ->
                            m.getWorkspace().getWorkspaceId()
                                    .equals(workspaceId)
                    );

            if (alreadyMember) {
                throw new IllegalArgumentException(
                        "This account is already a member of the workspace."
                );
            }

            boolean hasExistingMembership = membershipRepository
                    .findAll()
                    .stream()
                    .anyMatch(m ->
                            m.getUser().getUserId()
                                    .equals(existingUser.getUserId())
                                    && m.getWorkspace().getWorkspaceId()
                                    .equals(workspaceId)
                    );

            if (hasExistingMembership) {
                throw new IllegalArgumentException(
                        "An existing workspace membership must be managed by an Admin."
                );
            }
        }

        boolean pendingInvitationExists = invitationRepository
                .existsByWorkspace_WorkspaceIdAndEmailIgnoreCaseAndRoleAndStatus(
                        workspaceId,
                        email,
                        request.role(),
                        WorkspaceInvitation.InvitationStatus.PENDING
                );

        if (pendingInvitationExists) {
            throw new IllegalArgumentException(
                    "A pending invitation already exists for this email and role."
            );
        }

        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        WorkspaceInvitation invitation =
                new WorkspaceInvitation();

        invitation.setWorkspace(workspace);
        invitation.setInvitedBy(admin);
        invitation.setEmail(email);
        invitation.setRole(request.role());
        invitation.setProgrammeId(request.programmeId());
        invitation.setSectionEntity(section);
        invitation.setTokenHash(hashToken(rawToken));
        invitation.setStatus(
                WorkspaceInvitation.InvitationStatus.PENDING
        );
        invitation.setExpiresAt(
                LocalDateTime.now()
                        .plusDays(INVITATION_VALIDITY_DAYS)
        );

        WorkspaceInvitation saved =
                invitationRepository.save(invitation);

        // Raw token is returned once for delivery.
        // Only the SHA-256 hash is stored in the database.
        return new IssuedInvitation(
                saved.getInvitationId(),
                rawToken,
                saved.getExpiresAt()
        );
    }

    // ==========================================
    // REVOKE AN INVITATION
    // ==========================================

    @Transactional
    public void revokeInvitation(
            Integer workspaceId,
            Integer invitationId
    ) {

        User admin = workspaceAccessService.getAuthenticatedUser();

        workspaceAccessService.requireActiveWorkspace(workspaceId);

        workspaceAccessService.requireWorkspaceRole(
                admin.getUserId(),
                workspaceId,
                WorkspaceMembership.WorkspaceRole.ADMIN
        );


        WorkspaceInvitation invitation =
                invitationRepository.findById(invitationId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Invitation not found."
                                )
                        );

        if (!invitation.getWorkspace()
                .getWorkspaceId().equals(workspaceId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Invitation not found in this workspace."
            );
        }

        if (invitation.getStatus()
                != WorkspaceInvitation.InvitationStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending invitations can be revoked."
            );
        }

        invitation.setStatus(
                WorkspaceInvitation.InvitationStatus.REVOKED
        );

        invitationRepository.save(invitation);
    }

    // ==========================================
    // ACCEPT AS AN EXISTING AUTHENTICATED USER
    // ==========================================

    @Transactional
    public Integer acceptInvitation(String rawToken) {


        User currentUser =
                workspaceAccessService.getAuthenticatedUser();


        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Invitation token is required."
            );
        }

        WorkspaceInvitation invitation =
                invitationRepository.findByTokenHashForUpdate(
                        hashToken(rawToken.trim())
                ).orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Invitation not found."
                        )
                );

        if (invitation.getStatus()
                != WorkspaceInvitation.InvitationStatus.PENDING) {
            throw new IllegalStateException(
                    "This invitation is no longer pending."
            );
        }

        if (!invitation.getExpiresAt()
                .isAfter(LocalDateTime.now())) {
            invitation.setStatus(
                    WorkspaceInvitation.InvitationStatus.EXPIRED
            );
            invitationRepository.save(invitation);

            throw new ResponseStatusException(
                    HttpStatus.GONE,
                    "This invitation has expired."
            );
        }

        String invitedEmail = invitation.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (!currentUser.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT)
                .equals(invitedEmail)) {
            throw new AccessDeniedException(
                    "You must sign in with the invited email address."
            );
        }

        Workspace workspace =
                workspaceAccessService.requireActiveWorkspace(
                        invitation.getWorkspace().getWorkspaceId()
                );

        validateInvitationRole(
                workspace,
                invitation.getRole()
        );

        // Ensure the assigned academic records are still valid.
        if (invitation.getRole()
                == WorkspaceMembership.WorkspaceRole.STUDENT) {

            Programme programme = programmeRepository
                    .findById(invitation.getProgrammeId())
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "Invitation programme no longer exists."
                            )
                    );

            Section section = invitation.getSectionEntity();

            if (section == null
                    || section.getBatch() == null
                    || programme.getWorkspace() == null
                    || section.getBatch().getWorkspace() == null
                    || !programme.getWorkspace().getWorkspaceId()
                    .equals(workspace.getWorkspaceId())
                    || !section.getBatch().getWorkspace()
                    .getWorkspaceId()
                    .equals(workspace.getWorkspaceId())) {

                throw new IllegalStateException(
                        "Invitation academic assignment is invalid."
                );
            }

            // The current User entity has only one academic profile. Reject conflicting assignments until workspace-specific profiles are implemented.
            if (currentUser.getProgramme() != null
                    && currentUser.getProgramme().getProgrammeId()
                    != programme.getProgrammeId()) {
                throw new IllegalStateException(
                        "Account already has a different programme."
                );
            }

            if (currentUser.getSection() != null
                    && !currentUser.getSection().getSectionId()
                    .equals(section.getSectionId())) {
                throw new IllegalStateException(
                        "Account already has a different section."
                );
            }

            currentUser.setProgramme(programme);
            currentUser.setSection(section);
            userRepository.save(currentUser);
        }

        // Do not reactivate or overwrite a suspended/removed membership through an invitation.
        boolean membershipExists = membershipRepository.findAll()
                .stream()
                .anyMatch(m ->
                        m.getUser().getUserId()
                                .equals(currentUser.getUserId())
                                && m.getWorkspace().getWorkspaceId()
                                .equals(workspace.getWorkspaceId())
                );

        if (membershipExists) {
            throw new IllegalStateException(
                    "An existing membership requires Admin management."
            );
        }

        WorkspaceMembership membership =
                new WorkspaceMembership();

        membership.setUser(currentUser);
        membership.setWorkspace(workspace);
        membership.setRole(invitation.getRole());
        membership.setStatus(
                WorkspaceMembership.MembershipStatus.ACTIVE
        );

        membershipRepository.save(membership);

        invitation.setStatus(
                WorkspaceInvitation.InvitationStatus.ACCEPTED
        );
        invitation.setAcceptedAt(LocalDateTime.now());

        invitationRepository.save(invitation);

        return workspace.getWorkspaceId();
    }


    // ==========================================
    // ROLE AND ACADEMIC VALIDATION
    // ==========================================

    private void validateInvitationRole(
            Workspace workspace,
            WorkspaceMembership.WorkspaceRole role
    ) {
        boolean valid;

        if (workspace.getWorkspaceType()
                == Workspace.WorkspaceType.ACADEMIC) {
            valid = role == WorkspaceMembership.WorkspaceRole.ADMIN
                    || role == WorkspaceMembership.WorkspaceRole.STUDENT
                    || role == WorkspaceMembership.WorkspaceRole.SUPERVISOR;
        } else {
            valid = role == WorkspaceMembership.WorkspaceRole.ADMIN
                    || role == WorkspaceMembership.WorkspaceRole.MANAGER
                    || role == WorkspaceMembership.WorkspaceRole.MEMBER;
        }

        if (!valid) {
            throw new IllegalArgumentException(
                    "This role is not allowed for the workspace type."
            );
        }
    }

    private Section validateAcademicAssignment(
            Workspace workspace,
            WorkspaceInvitationRequest request
    ) {
        if (request.role()
                != WorkspaceMembership.WorkspaceRole.STUDENT) {

            if (request.programmeId() != null
                    || request.sectionId() != null) {
                throw new IllegalArgumentException(
                        "Academic assignment is only allowed for students."
                );
            }

            return null;
        }

        if (request.programmeId() == null
                || request.sectionId() == null) {
            throw new IllegalArgumentException(
                    "Student invitations require programmeId and sectionId."
            );
        }

        Programme programme = programmeRepository
                .findById(request.programmeId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Programme not found."
                        )
                );

        Section section = sectionRepository
                .findById(request.sectionId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Section not found."
                        )
                );

        if (section.getBatch() == null
                || programme.getWorkspace() == null
                || section.getBatch().getWorkspace() == null
                || !programme.getWorkspace().getWorkspaceId()
                .equals(workspace.getWorkspaceId())
                || !section.getBatch().getWorkspace()
                .getWorkspaceId()
                .equals(workspace.getWorkspaceId())) {

            throw new IllegalArgumentException(
                    "Programme and section must belong to this workspace."
            );
        }

        return section;
    }

    // ==========================================
    // SHA-256 TOKEN HASHING
    // ==========================================

    private String hashToken(String token) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hashed = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hashed);

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to hash invitation token.",
                    exception
            );
        }
    }


    // NEW USER: REGISTER THROUGH A VALID INVITATION
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerInvitedUser(
            @Valid @RequestBody
            InvitedUserRegistrationRequest request
    ) {

        Integer workspaceId =
                invitedUserRegistrationService.registerInvitedUser(
                        request
                );

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
    // PREVIEW AN INVITATION BEFORE REGISTRATION
    // ==========================================

    @Transactional(readOnly = true)
    public WorkspaceInvitationPreviewResponse previewInvitation(
            String rawToken
    ) {

        // STEP 1: Validate the supplied token.
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Invitation token is required."
            );
        }

        String tokenHash = hashToken(rawToken.trim());

        // STEP 2: Look up the invitation using its hash.
        WorkspaceInvitation invitation =
                invitationRepository.findByTokenHash(tokenHash)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Invalid invitation."
                                )
                        );

        // STEP 3: Only pending invitations can be previewed.
        if (invitation.getStatus()
                != WorkspaceInvitation.InvitationStatus.PENDING) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This invitation is no longer available."
            );
        }

        // STEP 4: Reject expired invitations.
        if (invitation.getExpiresAt() == null
                || !invitation.getExpiresAt()
                .isAfter(LocalDateTime.now())) {

            throw new ResponseStatusException(
                    HttpStatus.GONE,
                    "This invitation has expired."
            );
        }

        // STEP 5: Verify the workspace is active.
        Workspace workspace =
                workspaceAccessService.requireActiveWorkspace(
                        invitation.getWorkspace().getWorkspaceId()
                );

        // STEP 6: Validate the invited role.
        validateInvitationRole(
                workspace,
                invitation.getRole()
        );

        // STEP 7: Resolve academic information if necessary.
        Integer programmeId = null;
        String programmeName = null;

        Integer sectionId = null;
        String sectionName = null;
        String batchName = null;

        if (invitation.getRole()
                == WorkspaceMembership.WorkspaceRole.STUDENT) {

            if (invitation.getProgrammeId() == null
                    || invitation.getSectionEntity() == null) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Student invitation has incomplete academic details."
                );
            }

            Programme programme =
                    programmeRepository.findById(
                            invitation.getProgrammeId()
                    ).orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.CONFLICT,
                                    "Invited programme no longer exists."
                            )
                    );

            Section section = invitation.getSectionEntity();

            if (section.getBatch() == null
                    || programme.getWorkspace() == null
                    || section.getBatch().getWorkspace() == null
                    || !workspace.getWorkspaceId().equals(
                    programme.getWorkspace().getWorkspaceId()
            )
                    || !workspace.getWorkspaceId().equals(
                    section.getBatch()
                            .getWorkspace()
                            .getWorkspaceId()
            )) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "The academic assignment is no longer valid."
                );
            }

            programmeId = programme.getProgrammeId();
            programmeName = programme.getName();

            sectionId = section.getSectionId();
            sectionName = section.getName();
            batchName = section.getBatch().getBatchName();
        }

        // STEP 8: Return safe, read-only invitation details.
        return new WorkspaceInvitationPreviewResponse(
                workspace.getWorkspaceId(),
                workspace.getName(),
                workspace.getWorkspaceType(),
                invitation.getEmail(),
                invitation.getRole(),
                programmeId,
                programmeName,
                sectionId,
                sectionName,
                batchName,
                invitation.getExpiresAt()
        );
    }


}
