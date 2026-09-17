package com.softwareprojectmanagement.Conrollers;

import com.softwareprojectmanagement.DTO.Request.Submission.SubmissionApprovalRequest;
import com.softwareprojectmanagement.DTO.Response.Submission.SubmissionPdfResponse;
import com.softwareprojectmanagement.DTO.Response.Submission.SubmissionQueueResponse;
import com.softwareprojectmanagement.Services.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

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
}
