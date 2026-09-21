package com.softwareprojectmanagement.DTO.Request.Invitation;

import lombok.Data;

@Data
public class InvitationRequest {
    private Integer invitedUserId;
    private Integer projectId;
}
