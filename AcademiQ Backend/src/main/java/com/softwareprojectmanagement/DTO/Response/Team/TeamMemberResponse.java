
package com.softwareprojectmanagement.DTO.Response.Team;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberResponse {

    // Unique project membership ID
    private Integer memberId;

    // Student identification
    private Integer userId;

    // Student display information
    private String fullName;

    // Role within this specific project
    private String role;

    // Date and time the student joined the project
    private LocalDateTime joinedAt;

}
