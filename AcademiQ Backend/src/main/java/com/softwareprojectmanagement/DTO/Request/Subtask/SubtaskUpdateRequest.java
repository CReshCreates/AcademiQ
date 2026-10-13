
package com.softwareprojectmanagement.DTO.Request.Subtask;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubtaskUpdateRequest {

    // Optional: updated subtask title
    @Size(
            min = 1,
            max = 255,
            message = "Subtask title must contain 1 to 255 characters."
    )
    private String title;

    // Optional: updated description
    @Size(
            max = 5000,
            message = "Subtask description cannot exceed 5000 characters."
    )
    private String description;

    // Optional: user ID of a member of the same project
    private Integer assignedToUserId;
}
