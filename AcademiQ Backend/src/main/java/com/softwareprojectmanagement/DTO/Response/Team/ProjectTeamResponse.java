
package com.softwareprojectmanagement.DTO.Response.Team;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectTeamResponse {

    // Project information
    private Integer projectId;
    private String projectTitle;
    private String projectStatus;

    // Accepted members
    private Integer memberCount;

    // Maximum permitted members
    private Integer maxMembers;

    // Invitations awaiting a response
    private Integer pendingInvitationCount;

    // Number of unreserved team positions
    private Integer availableSlots;

    // Whether the authenticated student is the leader of this project
    private Boolean isLeader;

    // Whether the frontend should display an enabled Invite Member action
    private Boolean canInvite;

    // Accepted project members
    private List<TeamMemberResponse> members;
}
