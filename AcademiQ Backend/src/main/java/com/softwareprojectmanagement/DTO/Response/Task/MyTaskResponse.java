
package com.softwareprojectmanagement.DTO.Response.Task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyTaskResponse {

    // Task identification
    private Integer taskId;

    // Project identification
    private Integer projectId;
    private String projectTitle;

    // Milestone identification
    private Integer milestoneId;

    // Task details
    private String title;
    private String description;
    private String status;
    private String priority;
    private LocalDate deadline;
    private Integer progress;
}
