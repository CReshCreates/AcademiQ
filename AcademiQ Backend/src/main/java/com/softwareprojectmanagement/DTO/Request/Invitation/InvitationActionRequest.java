package com.softwareprojectmanagement.DTO.Request.Invitation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvitationActionRequest {
    Integer invitationId;
    String action;
    Integer projectId;
}
