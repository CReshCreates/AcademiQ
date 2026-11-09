
package com.softwareprojectmanagement.DTO.Request.Subtask;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubtaskCreateRequest {

    @NotBlank(message = "Subtask title is required.")
    @Size(
            max = 255,
            message = "Subtask title cannot exceed 255 characters."
    )
    private String title;

    @Size(
            max = 5000,
            message = "Subtask description cannot exceed 5000 characters."
    )
    private String description;

    // Optional: user ID of a member of the parent project.
    private Integer assignedToUserId;
}
