package com.softwareprojectmanagement.DTO.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectCreationRequest {
    private String projectTitle;
    private String projectDescription;
    private Integer programmeSubjectId;
    private Integer supervisorId;
    private MultipartFile uploadFile;
}
