
package com.softwareprojectmanagement.DTO.Response.Milestone;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneResponse {

    private Integer milestoneId;

    private String title;

    private String description;

    private LocalDate deadline;

    private String status;

    private Integer progress;
}
