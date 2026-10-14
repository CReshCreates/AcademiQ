
package com.softwareprojectmanagement.DTO.Response.Submission;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionHistoryResponse {

    // Submission information
    private Integer submissionId;
    private Integer milestoneId;
    private String milestoneTitle;

    // Uploaded document
    private String fileName;
    private Integer submissionVersion;
    private LocalDateTime submittedAt;

    // Supervisor review status
    private String status;

    // Student who submitted the document
    private Integer submittedByUserId;
    private String submittedByName;

    // Latest supervisor feedback, if available
    private String feedback;
    private String reviewedByName;
    private LocalDateTime feedbackCreatedAt;
}
