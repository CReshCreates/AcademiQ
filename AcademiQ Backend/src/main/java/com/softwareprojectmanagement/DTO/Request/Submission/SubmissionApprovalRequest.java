package com.softwareprojectmanagement.DTO.Request.Submission;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubmissionApprovalRequest {
    Integer submissionId;
    String comment;
}
