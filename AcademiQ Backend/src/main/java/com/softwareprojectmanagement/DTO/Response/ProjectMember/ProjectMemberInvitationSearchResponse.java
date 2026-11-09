package com.softwareprojectmanagement.DTO.Response.ProjectMember;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectMemberInvitationSearchResponse {
    private Integer userId;
    private String fullName;
    private String email;
}
