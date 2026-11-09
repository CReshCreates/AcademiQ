
package com.softwareprojectmanagement.Services;

import com.softwareprojectmanagement.DTO.Request.Workspace.InvitedUserRegistrationRequest;
import com.softwareprojectmanagement.Models.*;
import com.softwareprojectmanagement.Repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class InvitedUserRegistrationService {

    private final WorkspaceInvitationRepository invitationRepository;
    private final WorkspaceMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final ProgrammeRepository programmeRepository;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder(12);

    // ================================================
    // REGISTER NEW ACCOUNT THROUGH WORKSPACE INVITATION
    // ================================================

    @Transactional
    public Integer registerInvitedUser(
            InvitedUserRegistrationRequest request
    ) {

        if (request == null
                || request.token() == null
                || request.token().isBlank()
                || request.fullName() == null
                || request.fullName().isBlank()
                || request.password() == null
                || request.password().length() < 8) {

            throw new IllegalArgumentException(
                    "Valid invitation token, full name and password are required."
            );
        }

        String fullName = request.fullName().trim();

        if (fullName.length() > 150) {
            throw new IllegalArgumentException(
                    "Full name must not exceed 150 characters."
            );
        }

        // STEP 1: Find and lock the invitation.
        String tokenHash = hashToken(
                request.token().trim()
        );

        WorkspaceInvitation invitation =
                invitationRepository.findByTokenHashForUpdate(
                        tokenHash
                ).orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Invalid invitation."
                        )
                );

        // STEP 2: Verify invitation status and expiry.
        if (invitation.getStatus()
                != WorkspaceInvitation.InvitationStatus.PENDING) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invitation is no longer pending."
            );
        }

        if (invitation.getExpiresAt() == null
                || !invitation.getExpiresAt()
                .isAfter(LocalDateTime.now())) {

            throw new ResponseStatusException(
                    HttpStatus.GONE,
                    "Invitation has expired."
            );
        }

        // STEP 3: Verify the destination workspace.
        Workspace workspace = invitation.getWorkspace();

        if (workspace == null
                || workspace.getStatus()
                != Workspace.WorkspaceStatus.ACTIVE) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Invitation workspace is not active."
            );
        }

        // STEP 4: Normalize the invited email.
        String email = invitation.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An account already exists for this email. Sign in and accept the invitation."
            );
        }

        // STEP 5: Validate the invited role.
        WorkspaceMembership.WorkspaceRole role =
                invitation.getRole();

        boolean validRole;

        if (workspace.getWorkspaceType()
                == Workspace.WorkspaceType.ACADEMIC) {

            validRole = role
                    == WorkspaceMembership.WorkspaceRole.ADMIN
                    || role
                    == WorkspaceMembership.WorkspaceRole.STUDENT
                    || role
                    == WorkspaceMembership.WorkspaceRole.SUPERVISOR;

        } else if (workspace.getWorkspaceType()
                == Workspace.WorkspaceType.ORGANIZATIONAL) {

            validRole = role
                    == WorkspaceMembership.WorkspaceRole.ADMIN
                    || role
                    == WorkspaceMembership.WorkspaceRole.MANAGER
                    || role
                    == WorkspaceMembership.WorkspaceRole.MEMBER;

        } else {
            validRole = false;
        }

        if (!validRole) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invitation role is invalid for the workspace."
            );
        }

        // STEP 6: Validate academic information.
        Programme programme = null;
        Section section = null;

        if (role == WorkspaceMembership.WorkspaceRole.STUDENT) {

            if (invitation.getProgrammeId() == null
                    || invitation.getSectionEntity() == null) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Student invitation has incomplete academic details."
                );
            }

            programme = programmeRepository
                    .findById(invitation.getProgrammeId())
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.BAD_REQUEST,
                                    "Invited programme does not exist."
                            )
                    );

            section = invitation.getSectionEntity();

            if (section.getBatch() == null
                    || programme.getWorkspace() == null
                    || section.getBatch().getWorkspace() == null
                    || !programme.getWorkspace().getWorkspaceId()
                    .equals(workspace.getWorkspaceId())
                    || !section.getBatch().getWorkspace()
                    .getWorkspaceId()
                    .equals(workspace.getWorkspaceId())) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invited academic assignment does not belong to this workspace."
                );
            }

        } else if (invitation.getProgrammeId() != null
                || invitation.getSectionEntity() != null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Non-student invitation cannot contain academic assignments."
            );
        }

        // STEP 7: Create the account.
        User user = new User();

        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );

        // Legacy compatibility only.
        // Authorization must use workspace membership.
        user.setRole(role.name());
        user.setCreatedAt(LocalDateTime.now());

        if (role == WorkspaceMembership.WorkspaceRole.STUDENT) {
            user.setProgramme(programme);
            user.setSection(section);
        }

        User savedUser = userRepository.save(user);

        // STEP 8: Create the active workspace membership.
        WorkspaceMembership membership =
                new WorkspaceMembership();

        membership.setUser(savedUser);
        membership.setWorkspace(workspace);
        membership.setRole(role);
        membership.setStatus(
                WorkspaceMembership.MembershipStatus.ACTIVE
        );

        membershipRepository.save(membership);

        // STEP 9: Mark invitation as accepted.
        invitation.setStatus(
                WorkspaceInvitation.InvitationStatus.ACCEPTED
        );
        invitation.setAcceptedAt(LocalDateTime.now());

        invitationRepository.save(invitation);

        // STEP 10: Return the workspace ID.
        return workspace.getWorkspaceId();
    }

    // ================================================
    // HASH THE INVITATION TOKEN FOR DATABASE LOOKUP
    // ================================================

    private String hashToken(String rawToken) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    rawToken.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to validate invitation token.",
                    exception
            );
        }
    }
}
