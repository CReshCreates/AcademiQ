package com.softwareprojectmanagement.DTO.Response.Submission;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubmissionQueueResponse {
    private Integer submissionId;
    private String projectTitle;
    private LocalDateTime submittedAt;
    private String milestone;
    private Integer memberId;
    private String fullName;
}
