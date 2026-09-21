package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.DTO.Response.Invitation.NotificationDetailsForInvitation;
import com.softwareprojectmanagement.Models.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    @Query("""
    SELECT n.notificationId AS notificationId,
    n.title AS notificationTitle,
    n.message AS notificationMessage,
    n.invitationId AS invitationId,
    i.project.project_id AS projectId
    FROM Notification n 
    JOIN Invitation i 
    ON n.invitationId = i.invitationId
    WHERE n.user.email = :email AND n.type = 'INVITATION'
""")
    List<NotificationDetailsForInvitation> getNotificationDetails(@Param("email") String invitedTo);
}
