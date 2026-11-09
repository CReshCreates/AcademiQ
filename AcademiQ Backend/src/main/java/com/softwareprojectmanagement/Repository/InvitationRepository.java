package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.Models.Invitation;
import com.softwareprojectmanagement.Models.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Integer> {

    @Query("""
    SELECT i.invitationId FROM Invitation i WHERE i.invitedUser.email = :email AND i.status = 'PENDING'
""")
    List<Integer> getInvitationIdsByEmail(@Param("email") String email);

    @Query("""
    SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
    FROM Invitation i
    WHERE i.project.project_id = :projectId
      AND i.invitedUser.userId = :invitedUserId
      AND i.status = :status
    """)
    boolean existsInvitationByProjectAndUserAndStatus(
            @Param("projectId") Integer projectId,
            @Param("invitedUserId") Integer invitedUserId,
            @Param("status") InvitationStatus status
    );


    /**
     * Count pending invitations for a project.
     * Only PENDING invitations reserve available positions.
     */
    @Query("""
    SELECT COUNT(i)
    FROM Invitation i
    WHERE i.project.project_id = :projectId
      AND i.status = :status
    """)
    long countInvitationsByProjectAndStatus(
            @Param("projectId") Integer projectId,
            @Param("status") InvitationStatus status
    );


}
