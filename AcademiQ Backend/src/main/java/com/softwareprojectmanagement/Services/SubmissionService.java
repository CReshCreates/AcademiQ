package com.softwareprojectmanagement.Services;

import com.softwareprojectmanagement.DTO.Request.Submission.SubmissionApprovalRequest;
import com.softwareprojectmanagement.DTO.Response.Submission.SubmissionPdfResponse;
import com.softwareprojectmanagement.DTO.Response.Submission.SubmissionQueueResponse;
import com.softwareprojectmanagement.Exceptions.NoSubmissionException;
import com.softwareprojectmanagement.Models.*;
import com.softwareprojectmanagement.Repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubmissionService {
    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final CommentRepository commentRepository;
    private final ProjectActivitiesRepository projectActivitiesRepository;
    private final MilestoneRepository milestoneRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public List<SubmissionQueueResponse> getSubmissionQueueForSupervisor(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        User user = userRepository.findByEmail(authentication.getName());

        if(user == null){
            throw new UsernameNotFoundException("User not found");
        }

        List<SubmissionQueueResponse> submissionQueue = submissionRepository.findPendingProjectReviews(user.getUserId());

        if(submissionQueue.isEmpty()){
            throw new NoSubmissionException("There are currently no submissions to review!");
        };

        return submissionQueue;
    }



    public SubmissionPdfResponse getPdfNames(
            Integer submissionId
    ) throws IOException {

        // Identify the authenticated user.
        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new AccessDeniedException(
                    "Authentication is required."
            );
        }

        User currentUser = userRepository
                .findByEmail(authentication.getName());

        if (currentUser == null) {
            throw new UsernameNotFoundException(
                    "Authenticated user not found."
            );
        }

        // Restrict this endpoint to supervisors.
        if (!"SUPERVISOR".equalsIgnoreCase(
                currentUser.getRole()
        )) {
            throw new AccessDeniedException(
                    "Only supervisors can access this endpoint."
            );
        }

        // STEP 3: Retrieve the submission.
        Submission submission = submissionRepository
                .findById(submissionId)
                .orElseThrow(() ->
                        new NoSubmissionException(
                                "Submission not found."
                        )
                );

        // Verify the submission belongs
        // to a valid project.
        Milestone milestone = submission.getMilestone();

        if (milestone == null
                || milestone.getProject() == null) {
            throw new IllegalStateException(
                    "Submission is not linked to a valid project."
            );
        }

        Project project = milestone.getProject();

        // Check supervisor authorization.
        User assignedSupervisor = project.getSupervisor();

        if (assignedSupervisor == null
                || !currentUser.getUserId().equals(
                assignedSupervisor.getUserId()
        )) {
            throw new AccessDeniedException(
                    "You are not authorized to access this submission."
            );
        }

        // Validate the stored file reference.
        String storedPath = submission.getFileUrl();

        if (storedPath == null
                || !storedPath.startsWith("/uploads/")) {
            throw new AccessDeniedException(
                    "Invalid submission file reference."
            );
        }

        String relativePath =
                storedPath.substring("/uploads/".length());

        if (relativePath.isBlank()
                || relativePath.contains("\\")
                || relativePath.startsWith("/")) {
            throw new AccessDeniedException(
                    "Invalid submission file path."
            );
        }

        // Resolve the file inside
        // the configured upload directory.
        Path uploadPath = Paths.get(uploadDir)
                .toAbsolutePath()
                .normalize();

        Path filePath = uploadPath
                .resolve(relativePath)
                .normalize();

        if (!filePath.startsWith(uploadPath)
                || filePath.equals(uploadPath)) {
            throw new AccessDeniedException(
                    "File path is outside the upload directory."
            );
        }

        // Verify the file exists
        // and is not itself a symbolic link.
        if (!Files.isRegularFile(
                filePath,
                LinkOption.NOFOLLOW_LINKS
        )) {
            throw new NoSubmissionException(
                    "Submission PDF not found."
            );
        }

        // Resolve real filesystem paths
        // to detect symbolic-link directory traversal.
        Path realUploadPath = uploadPath.toRealPath();
        Path realFilePath = filePath.toRealPath();

        if (!realFilePath.startsWith(realUploadPath)) {
            throw new AccessDeniedException(
                    "Invalid submission file location."
            );
        }

        // Return the protected file.
        Resource resource = new UrlResource(
                realFilePath.toUri()
        );

        if (!resource.isReadable()) {
            throw new NoSubmissionException(
                    "Submission PDF is not readable."
            );
        }

        return new SubmissionPdfResponse(
                resource,
                submission.getFileName()
        );
    }

    // VERIFY SUPERVISOR REVIEW AUTHORIZATION

    private void verifySupervisorCanReview(
            User currentUser,
            Submission submission
    ) {

        // Only supervisors can review submissions.
        if (currentUser == null
                || !"SUPERVISOR".equalsIgnoreCase(
                currentUser.getRole()
        )) {
            throw new AccessDeniedException(
                    "Only supervisors can review submissions."
            );
        }

        // The submission must belong to a project.
        Milestone milestone = submission.getMilestone();

        if (milestone == null
                || milestone.getProject() == null) {
            throw new IllegalStateException(
                    "Submission is not linked to a valid project."
            );
        }

        Project project = milestone.getProject();

        // Only the supervisor assigned to this project
        // can approve, reject, or request changes.
        User assignedSupervisor = project.getSupervisor();

        if (assignedSupervisor == null
                || !currentUser.getUserId().equals(
                assignedSupervisor.getUserId()
        )) {
            throw new AccessDeniedException(
                    "You are not authorized to review this submission."
            );
        }
    }


    @Transactional
    public String approveSubmission(SubmissionApprovalRequest approvalRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        User user = userRepository.findByEmail(authentication.getName());

        if(user == null){
            throw new UsernameNotFoundException("User not found");
        }

        Submission submission = submissionRepository.findById(approvalRequest.getSubmissionId()).orElseThrow(()->new NoSubmissionException("No Submission found!"));

        verifySupervisorCanReview(user, submission);

        submission.setStatus("APPROVED");
        Submission savedSubmission = submissionRepository.save(submission);

        Milestone milestone = savedSubmission.getMilestone();
        System.out.println("Milestone ID: " + milestone.getMilestoneId());
        milestone.setStatus("COMPLETED");
        milestone.setProgress(100);
        milestoneRepository.save(milestone);

        Comment comment = new Comment();

        comment.setContent(approvalRequest.getComment());
        comment.setCreatedAt(LocalDateTime.now());
        comment.setSubmission(savedSubmission);
        comment.setUser(user);

        commentRepository.save(comment);

        Project project = savedSubmission.getMilestone().getProject();

        ProjectActivities projectActivities = new ProjectActivities();
        projectActivities.setActivityType("PROPOSAL_APPROVED");
        projectActivities.setDescription("Project " + project.getTitle() + " has been approved by " + user.getFullName());
        projectActivities.setProject(project);
        projectActivities.setCreatedAt(LocalDateTime.now());
        projectActivities.setUser(user);

        projectActivitiesRepository.save(projectActivities);

        return "Project Approved";
    }

    @Transactional
    public String rejectSubmission(SubmissionApprovalRequest approvalRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        User user = userRepository.findByEmail(authentication.getName());

        if(user == null){
            throw new UsernameNotFoundException("User not found");
        }

        Submission submission = submissionRepository.findById(approvalRequest.getSubmissionId()).orElseThrow(()->new NoSubmissionException("No Submission found!"));

        verifySupervisorCanReview(user, submission);

        submission.setStatus("REJECTED");
        Submission savedSubmission = submissionRepository.save(submission);

        Comment comment = new Comment();

        comment.setContent(approvalRequest.getComment());
        comment.setCreatedAt(LocalDateTime.now());
        comment.setSubmission(savedSubmission);
        comment.setUser(user);

        commentRepository.save(comment);

        Project project = savedSubmission.getMilestone().getProject();

        ProjectActivities projectActivities = new ProjectActivities();
        projectActivities.setActivityType("PROPOSAL_REJECTED");
        projectActivities.setDescription("Project " + project.getTitle() + " has been REJECTED by " + user.getFullName());
        projectActivities.setProject(project);
        projectActivities.setCreatedAt(LocalDateTime.now());
        projectActivities.setUser(user);

        projectActivitiesRepository.save(projectActivities);

        return "Project Rejected";
    }

    @Transactional
    public String submissionChangeRequest(SubmissionApprovalRequest approvalRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        User user = userRepository.findByEmail(authentication.getName());

        if(user == null){
            throw new UsernameNotFoundException("User not found");
        }

        Submission submission = submissionRepository.findById(approvalRequest.getSubmissionId()).orElseThrow(()->new NoSubmissionException("No Submission found!"));

        verifySupervisorCanReview(user, submission);
        
        submission.setStatus("CHANGES_REQUESTED");
        Submission savedSubmission = submissionRepository.save(submission);

        Comment comment = new Comment();

        comment.setContent(approvalRequest.getComment());
        comment.setCreatedAt(LocalDateTime.now());
        comment.setSubmission(savedSubmission);
        comment.setUser(user);

        commentRepository.save(comment);

        Project project = savedSubmission.getMilestone().getProject();

        ProjectActivities projectActivities = new ProjectActivities();
        projectActivities.setActivityType("PROPOSAL_CHANGES_REQUESTED");
        projectActivities.setDescription(user.getFullName() + " has requested changes in project " + project.getTitle());
        projectActivities.setProject(project);
        projectActivities.setCreatedAt(LocalDateTime.now());
        projectActivities.setUser(user);

        projectActivitiesRepository.save(projectActivities);

        return "Changes Requested";
    }
}
