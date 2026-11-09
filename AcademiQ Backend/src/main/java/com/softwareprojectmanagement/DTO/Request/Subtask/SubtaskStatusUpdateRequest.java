
package com.softwareprojectmanagement.DTO.Request.Subtask;

import com.softwareprojectmanagement.Models.SubtaskStatus;

import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubtaskStatusUpdateRequest {

    @NotNull(message = "Subtask status is required.")
    private SubtaskStatus status;

}
