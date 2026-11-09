
package com.softwareprojectmanagement.DTO.Response.Subtask;

import com.softwareprojectmanagement.Models.SubtaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubtaskResponse {

    private Long subtaskId;

    private Integer taskId;

    private String title;

    private String description;

    private SubtaskStatus status;

    private Integer assignedToUserId;

    private String assignedToName;
}
