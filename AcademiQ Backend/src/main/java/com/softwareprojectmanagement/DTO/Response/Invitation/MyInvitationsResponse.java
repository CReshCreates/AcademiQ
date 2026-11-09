package com.softwareprojectmanagement.DTO.Response.Invitation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MyInvitationsResponse {
    private Integer invitationId;
    private NotificationDetailsForInvitation notificationDetails;
}
