
package com.softwareprojectmanagement.DTO.Response.Task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

    private Integer taskId;

    private String title;

    private String description;

    private String status;

    private String priority;

    private LocalDate deadline;

    private Integer progress;

    private Integer milestoneId;

    private Integer assignedMemberId;

    private String assignedMemberName;

    private LocalDateTime createdAt;
}
