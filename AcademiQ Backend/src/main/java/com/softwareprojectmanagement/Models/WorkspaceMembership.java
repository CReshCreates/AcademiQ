
package com.softwareprojectmanagement.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "workspace_memberships",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_workspace_membership",
                        columnNames = {
                                "user_id",
                                "workspace_id"
                        }
                )
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "membership_id")
    private Integer membershipId;


    // ==========================================
    // USER ACCOUNT
    // ==========================================

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_membership_user"
            )
    )
    private User user;


    // ==========================================
    // WORKSPACE
    // ==========================================

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "workspace_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_membership_workspace"
            )
    )
    private Workspace workspace;


    // ==========================================
    // ROLE WITHIN THIS WORKSPACE
    // ==========================================

    @Enumerated(EnumType.STRING)
    @Column(
            name = "role",
            nullable = false,
            length = 30
    )
    private WorkspaceRole role;


    // ==========================================
    // MEMBERSHIP STATUS
    // ==========================================

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private MembershipStatus status = MembershipStatus.ACTIVE;


    // ==========================================
    // JOIN DATE
    // ==========================================

    @Column(
            name = "joined_at",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private LocalDateTime joinedAt;


    public enum WorkspaceRole {
        ADMIN,
        STUDENT,
        SUPERVISOR,
        MANAGER,
        MEMBER
    }


    public enum MembershipStatus {
        ACTIVE,
        INVITED,
        SUSPENDED,
        REMOVED
    }
}
