
package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.Models.WorkspaceMembership;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkspaceMembershipRepository
        extends JpaRepository<WorkspaceMembership, Integer> {

    // Retrieve a user's active workspace memberships.
    // Fetch the workspace alongside the membership.

    @Query("""
            SELECT wm
            FROM WorkspaceMembership wm
            JOIN FETCH wm.workspace
            WHERE wm.user.userId = :userId
              AND wm.status = :status
            """)
    List<WorkspaceMembership> findUserMembershipsByStatus(
            @Param("userId") Integer userId,
            @Param("status")
            WorkspaceMembership.MembershipStatus status
    );
}
