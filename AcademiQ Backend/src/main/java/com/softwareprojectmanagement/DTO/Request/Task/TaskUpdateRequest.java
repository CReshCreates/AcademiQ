
package com.softwareprojectmanagement.DTO.Request.Task;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskUpdateRequest {

    @Size(
            min = 1,
            max = 255,
            message = "Task title must contain between 1 and 255 characters."
    )
    private String title;

    private String description;

    private String priority;

    @FutureOrPresent(
            message = "Task deadline cannot be in the past."
    )
    private LocalDate deadline;

    private Integer memberId;
}
