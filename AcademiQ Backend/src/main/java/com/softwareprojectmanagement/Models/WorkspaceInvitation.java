
package com.softwareprojectmanagement.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "workspace_invitations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invitation_id")
    private Integer invitationId;

    // Workspace that the invitation belongs to
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "workspace_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_invitation_workspace"
            )
    )
    private Workspace workspace;

    // Admin who sent the invitation
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "invited_by",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_invitation_inviter"
            )
    )
    private User invitedBy;

    // Invited email address
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    // Role assigned when the invitation is accepted
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private WorkspaceMembership.WorkspaceRole role;

    // Academic programme assigned by Admin
    @Column(name = "programme_id")
    private Integer programmeId;

    // Academic section assigned by Admin
    // The batch is accessible through section.getBatch()
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "section_id",
            foreignKey = @ForeignKey(
                    name = "fk_invitation_section"
            )
    )
    private Section sectionEntity;

    // Legacy invitation columns retained temporarily
    @Column(name = "batch_name", length = 50)
    private String batchName;

    @Column(name = "section", length = 20)
    private String section;

    // SHA-256 hash of the invitation token
    @Column(
            name = "token_hash",
            nullable = false,
            unique = true,
            length = 64
    )
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(
            name = "created_at",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    public enum InvitationStatus {
        PENDING,
        ACCEPTED,
        REVOKED,
        EXPIRED
    }
}
