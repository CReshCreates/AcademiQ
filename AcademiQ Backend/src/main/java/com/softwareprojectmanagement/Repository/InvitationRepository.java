package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.Models.Invitation;
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
}
