package com.softwareprojectmanagement.DTO.Response.Project;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatedProjectResponse {
    private Integer projectId;
    private String projectTitle;
    private String supervisorName;
    private String semesterName;
    private String programmeSubjectName;
    private Integer progress;
    private String currentStage;
}
