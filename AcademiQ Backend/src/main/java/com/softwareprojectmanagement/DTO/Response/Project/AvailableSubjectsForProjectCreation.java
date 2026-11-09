package com.softwareprojectmanagement.DTO.Response.Project;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AvailableSubjectsForProjectCreation {
    private Integer subjectId;
    private String subjectName;
    private Integer programmeSubjectId;
}
