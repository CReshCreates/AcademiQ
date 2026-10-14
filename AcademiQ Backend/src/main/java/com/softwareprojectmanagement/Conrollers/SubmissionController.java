package com.softwareprojectmanagement.Conrollers;

import com.softwareprojectmanagement.DTO.Request.Submission.SubmissionApprovalRequest;
import com.softwareprojectmanagement.DTO.Response.Submission.SubmissionHistoryResponse;
import com.softwareprojectmanagement.DTO.Response.Submission.SubmissionPdfResponse;
import com.softwareprojectmanagement.DTO.Response.Submission.SubmissionQueueResponse;
import com.softwareprojectmanagement.Services.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController()
@RequestMapping("/submissions")
@RequiredArgsConstructor
public class SubmissionController {
    private final SubmissionService submissionService;

    @GetMapping("/supervisors/getSubmissionQueue")
    public ResponseEntity<List<SubmissionQueueResponse>> getSubmissionQueue(){
        return ResponseEntity.status(HttpStatus.OK).body(submissionService.getSubmissionQueueForSupervisor());
    }

    @GetMapping("/supervisors/getSubmissionPdf/{submissionId}")
    public ResponseEntity<Resource> getSubmissionPdf(@PathVariable Integer submissionId) throws IOException {
        SubmissionPdfResponse response = submissionService.getPdfNames(submissionId);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + response.getFileName() + "\""
                )
                .body(response.getResource());
    }

    @PostMapping("/supervisors/approveSubmission")
    public ResponseEntity<String> approvedSubmission(@RequestBody SubmissionApprovalRequest approvalRequest){
        return ResponseEntity.status(HttpStatus.OK).body(submissionService.approveSubmission(approvalRequest));
    }

    @PostMapping("/supervisors/rejectSubmission")
    public ResponseEntity<String> rejectedSubmission(@RequestBody SubmissionApprovalRequest approvalRequest){
        return ResponseEntity.status(HttpStatus.OK).body(submissionService.rejectSubmission(approvalRequest));
    }

    @PostMapping("/supervisors/requestChangesInSubmission")
    public ResponseEntity<String> requestChangesInSubmission(@RequestBody SubmissionApprovalRequest approvalRequest){
        return ResponseEntity.status(HttpStatus.OK).body(submissionService.submissionChangeRequest(approvalRequest));
    }


    // ==========================================
    // STUDENT: SUBMIT OR RESUBMIT MILESTONE PDF
    // ==========================================

    @PostMapping(
            value = "/students/projects/{projectId}/milestones/{milestoneId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Map<String, Object>> submitMilestone(
            @PathVariable Integer projectId,
            @PathVariable Integer milestoneId,
            @RequestPart("file") MultipartFile file
    ) {

        Integer submissionId =
                submissionService.submitMilestone(
                        projectId,
                        milestoneId,
                        file
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "submissionId", submissionId,
                        "message", "Milestone submitted successfully."
                ));
    }

    // STUDENT: GET PROJECT SUBMISSION HISTORY
    @GetMapping("/students/projects/{projectId}/history")
    public ResponseEntity<List<SubmissionHistoryResponse>>
    getProjectSubmissionHistory(
            @PathVariable Integer projectId
    ) {

        List<SubmissionHistoryResponse> history =
                submissionService.getProjectSubmissionHistory(
                        projectId
                );

        return ResponseEntity.ok(history);
    }


    // ==========================================
    // STUDENT: VIEW SUBMITTED MILESTONE PDF
    // ==========================================

    @GetMapping("/students/getSubmissionPdf/{submissionId}")
    public ResponseEntity<Resource> getStudentSubmissionPdf(
            @PathVariable Integer submissionId
    ) throws IOException {

        SubmissionPdfResponse response =
                submissionService.getPdfNames(submissionId);

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(
                                        response.getFileName(),
                                        StandardCharsets.UTF_8
                                )
                                .build()
                                .toString()
                )
                .body(response.getResource());
    }


}
