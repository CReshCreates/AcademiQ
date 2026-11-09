
package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.Models.WorkspaceInvitation;
import com.softwareprojectmanagement.Models.WorkspaceMembership;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceInvitationRepository
        extends JpaRepository<WorkspaceInvitation, Integer> {

    // Retrieve an invitation by its secure token hash.
    Optional<WorkspaceInvitation> findByTokenHash(
            String tokenHash
    );

    // Lock an invitation during acceptance to prevent simultaneous acceptance of the same token.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT wi
            FROM WorkspaceInvitation wi
            WHERE wi.tokenHash = :tokenHash
            """)
    Optional<WorkspaceInvitation> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );

    // Retrieve a workspace's invitations for Admin management.
    List<WorkspaceInvitation> findByWorkspace_WorkspaceId(
            Integer workspaceId
    );

    // Find an email's invitations with a particular status.
    List<WorkspaceInvitation> findByEmailIgnoreCaseAndStatus(
            String email,
            WorkspaceInvitation.InvitationStatus status
    );

    // Prevent multiple pending invitations for the same email, role and workspace.
    boolean existsByWorkspace_WorkspaceIdAndEmailIgnoreCaseAndRoleAndStatus(
            Integer workspaceId,
            String email,
            WorkspaceMembership.WorkspaceRole role,
            WorkspaceInvitation.InvitationStatus status
    );
}
