package com.softwareprojectmanagement.DTO.Response.Submission;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.core.io.Resource;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubmissionPdfResponse {
    private Resource resource;
    private String fileName;
}
