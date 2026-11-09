
package com.softwareprojectmanagement.DTO.Request.Task;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskCreateRequest {

    @NotBlank(message = "Task title is required.")
    @Size(max = 255, message = "Task title cannot exceed 255 characters.")
    private String title;

    @NotBlank(message = "Task description is required.")
    private String description;

    @NotBlank(message = "Task priority is required.")
    private String priority;

    @NotNull(message = "Task deadline is required.")
    @FutureOrPresent(message = "Task deadline cannot be in the past.")
    private LocalDate deadline;

    @NotNull(message = "Milestone ID is required.")
    private Integer milestoneId;

    @NotNull(message = "Assigned member ID is required.")
    private Integer memberId;
}
