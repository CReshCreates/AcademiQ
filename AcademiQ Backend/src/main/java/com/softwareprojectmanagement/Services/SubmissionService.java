package com.softwareprojectmanagement.Services;

import com.softwareprojectmanagement.DTO.Request.Submission.SubmissionApprovalRequest;
import com.softwareprojectmanagement.DTO.Response.Submission.SubmissionHistoryResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubmissionService {
    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final CommentRepository commentRepository;
    private final ProjectActivitiesRepository projectActivitiesRepository;
    private final MilestoneRepository milestoneRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final FileStorageService fileStorageService;

    @Value("${file.upload-dir}")
    private String uploadDir;


    // ==========================================
    // SUPERVISOR: GET PENDING SUBMISSION QUEUE
    // ==========================================

    @Transactional
    public List<SubmissionQueueResponse> getSubmissionQueueForSupervisor() {

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(
                authentication.getPrincipal()
        )) {

            throw new AccessDeniedException(
                    "Authentication is required."
            );
        }

        User user = userRepository.findByEmail(
                authentication.getName()
        );

        if (user == null) {
            throw new UsernameNotFoundException(
                    "Authenticated user not found."
            );
        }

        if (!"SUPERVISOR".equalsIgnoreCase(
                user.getRole()
        )) {

            throw new AccessDeniedException(
                    "Only supervisors can view the submission queue."
            );
        }

        return submissionRepository.findPendingProjectReviews(
                user.getUserId()
        );
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

        // STEP 3: Retrieve the submission.
        Submission submission = submissionRepository
                .findById(submissionId)
                .orElseThrow(() ->
                        new NoSubmissionException(
                                "Submission not found."
                        )
                );

        // Verify the submission belongs to a valid project.
        Milestone milestone = submission.getMilestone();

        if (milestone == null
                || milestone.getProject() == null) {
            throw new IllegalStateException(
                    "Submission is not linked to a valid project."
            );
        }

        Project project = milestone.getProject();

        // ==========================================
        // AUTHORIZE SUBMISSION PDF ACCESS
        // ==========================================

        Integer currentUserId = currentUser.getUserId();

        boolean isProjectCreator =
                "STUDENT".equalsIgnoreCase(currentUser.getRole())
                        && project.getUser() != null
                        && currentUserId.equals(
                        project.getUser().getUserId()
                );

        boolean isAssignedSupervisor =
                "SUPERVISOR".equalsIgnoreCase(currentUser.getRole())
                        && project.getSupervisor() != null
                        && currentUserId.equals(
                        project.getSupervisor().getUserId()
                );

        boolean isProjectMember =
                "STUDENT".equalsIgnoreCase(currentUser.getRole())
                        && projectMemberRepository
                        .findMembersByProjectId(
                                project.getProject_id()
                        )
                        .stream()
                        .anyMatch(member ->
                                member.getUser() != null
                                        && currentUserId.equals(
                                        member.getUser().getUserId()
                                )
                        );

        if (!isProjectCreator
                && !isAssignedSupervisor
                && !isProjectMember) {

            throw new AccessDeniedException(
                    "You are not authorized to access "
                            + "this submission PDF."
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

        // Resolve the file inside the configured upload directory.
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

        // Verify the file exists and is not itself a symbolic link.
        if (!Files.isRegularFile(
                filePath,
                LinkOption.NOFOLLOW_LINKS
        )) {
            throw new NoSubmissionException(
                    "Submission PDF not found."
            );
        }

        // Resolve real filesystem paths to detect symbolic-link directory traversal.
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


    // VERIFY SUPERVISOR REVIEW AUTHORIZATION AND SUBMISSION STATUS
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

        // Submission must belong to a valid project.
        Milestone milestone = submission.getMilestone();

        if (milestone == null
                || milestone.getProject() == null) {

            throw new IllegalStateException(
                    "Submission is not linked to a valid project."
            );
        }

        Project project = milestone.getProject();

        // Only the assigned supervisor can review.
        User assignedSupervisor = project.getSupervisor();

        if (assignedSupervisor == null
                || !currentUser.getUserId().equals(
                assignedSupervisor.getUserId()
        )) {

            throw new AccessDeniedException(
                    "You are not authorized to review this submission."
            );
        }

        // Only pending submissions can be reviewed.
        if (!"PENDING".equalsIgnoreCase(
                submission.getStatus()
        )) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This submission has already been reviewed "
                            + "or is not awaiting review."
            );
        }

        // Prevent review of an already completed milestone.
        if ("COMPLETED".equalsIgnoreCase(
                milestone.getStatus()
        )) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This milestone has already been completed."
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

        Submission submission = submissionRepository
                .findByIdForUpdate(approvalRequest.getSubmissionId())
                .orElseThrow(() ->
                        new NoSubmissionException("No Submission found!")
                );

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


    // ==========================================
    // STUDENT MILESTONE SUBMISSION
    // ==========================================

    @Transactional
    public Integer submitMilestone(
            Integer projectId,
            Integer milestoneId,
            MultipartFile file
    ) {

        // ======================================
        // STEP 1: AUTHENTICATE STUDENT
        // ======================================

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(
                authentication.getPrincipal()
        )) {

            throw new AccessDeniedException(
                    "Authentication is required."
            );
        }

        User currentUser = userRepository.findByEmail(
                authentication.getName()
        );

        if (currentUser == null) {
            throw new UsernameNotFoundException(
                    "Authenticated user not found."
            );
        }

        if (!"STUDENT".equalsIgnoreCase(
                currentUser.getRole()
        )) {

            throw new AccessDeniedException(
                    "Only students can submit milestone documents."
            );
        }

        // ======================================
        // STEP 2: VALIDATE THE PDF
        // ======================================

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A PDF document is required."
            );
        }

        String originalFilename =
                file.getOriginalFilename();

        if (originalFilename == null
                || !originalFilename.toLowerCase()
                .endsWith(".pdf")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only PDF documents are allowed."
            );
        }

        // ======================================
        // STEP 3: LOCK THE PROJECT
        // ======================================

        // Serialize submissions for milestones
        // belonging to the same project.

        Project project = projectRepository
                .findByIdForUpdate(projectId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Project not found."
                        )
                );

        // ======================================
        // STEP 4: VERIFY PROJECT MEMBERSHIP
        // ======================================

        ProjectMember submittingMember =
                projectMemberRepository
                        .findMembersByProjectId(projectId)
                        .stream()
                        .filter(member ->
                                member.getUser() != null
                                        && currentUser.getUserId()
                                        .equals(
                                                member.getUser()
                                                        .getUserId()
                                        )
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new AccessDeniedException(
                                        "You are not a member "
                                                + "of this project."
                                )
                        );

        // ======================================
        // STEP 5: VALIDATE MILESTONE
        // ======================================

        Milestone milestone = milestoneRepository
                .findById(milestoneId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Milestone not found."
                        )
                );

        if (milestone.getProject() == null
                || !project.getProject_id().equals(
                milestone.getProject()
                        .getProject_id()
        )) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Milestone does not belong to this project."
            );
        }

        // ======================================
        // STEP 6: CHECK PREVIOUS SUBMISSION
        // ======================================

        Submission previousSubmission =
                submissionRepository
                        .findLatestSubmissionByMilestoneId(
                                milestoneId
                        );

        int nextVersion = 1;

        if (previousSubmission != null) {

            String previousStatus =
                    previousSubmission.getStatus();

            if ("PENDING".equalsIgnoreCase(
                    previousStatus
            )) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "This milestone already has "
                                + "a pending submission."
                );
            }

            if ("APPROVED".equalsIgnoreCase(
                    previousStatus
            )) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "This milestone has already been approved."
                );
            }

            if (!"REJECTED".equalsIgnoreCase(
                    previousStatus
            ) && !"CHANGES_REQUESTED".equalsIgnoreCase(
                    previousStatus
            )) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "The previous submission status "
                                + "does not permit resubmission."
                );
            }

            nextVersion =
                    previousSubmission.getSubmissionVersion() + 1;
        }

        // A completed milestone should not receive another regular submission.

        if ("COMPLETED".equalsIgnoreCase(
                milestone.getStatus()
        )) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This milestone is already completed."
            );
        }

        // STEP 7: STORE DOCUMENT
        String storedFilePath =
                fileStorageService.storeMilestonePdf(file);

        // STEP 8: CREATE SUBMISSION RECORD
        Submission submission = new Submission();

        submission.setMilestone(milestone);
        submission.setProjectMember(submittingMember);

        submission.setFileUrl(storedFilePath);
        submission.setFileName(originalFilename);

        submission.setSubmissionVersion(nextVersion);
        submission.setStatus("PENDING");
        submission.setSubmittedAt(LocalDateTime.now());

        Submission savedSubmission =
                submissionRepository.save(submission);

        // STEP 9: RETURN SUBMISSION ID
        return savedSubmission.getSubmissionId();
    }


    // ==========================================
    // GET PROJECT SUBMISSION HISTORY
    // ==========================================

    @Transactional
    public List<SubmissionHistoryResponse> getProjectSubmissionHistory(
            Integer projectId
    ) {

        // STEP 1: Get authenticated user.

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(
                authentication.getPrincipal()
        )) {

            throw new AccessDeniedException(
                    "Authentication is required."
            );
        }

        User currentUser = userRepository.findByEmail(
                authentication.getName()
        );

        if (currentUser == null) {
            throw new UsernameNotFoundException(
                    "Authenticated user not found."
            );
        }

        // STEP 2: Retrieve project.

        Project project = projectRepository
                .findById(projectId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Project not found."
                        )
                );

        Integer currentUserId = currentUser.getUserId();

        // STEP 3: Verify project authorization.

        boolean isCreator =
                project.getUser() != null
                        && currentUserId.equals(
                        project.getUser().getUserId()
                );

        boolean isAssignedSupervisor =
                "SUPERVISOR".equalsIgnoreCase(
                        currentUser.getRole()
                )
                        && project.getSupervisor() != null
                        && currentUserId.equals(
                        project.getSupervisor().getUserId()
                );

        boolean isProjectMember =
                projectMemberRepository
                        .findMembersByProjectId(projectId)
                        .stream()
                        .anyMatch(member ->
                                member.getUser() != null
                                        && currentUserId.equals(
                                        member.getUser().getUserId()
                                )
                        );

        if (!isCreator
                && !isAssignedSupervisor
                && !isProjectMember) {

            throw new AccessDeniedException(
                    "You are not authorized to view "
                            + "this project's submissions."
            );
        }

        // STEP 4: Retrieve all submission versions.

        List<Submission> submissions =
                submissionRepository
                        .findSubmissionHistoryByProjectId(
                                projectId
                        );

        // STEP 5: Convert to response DTOs.

        return submissions.stream()
                .map(submission ->
                        mapToSubmissionHistoryResponse(
                                submission,
                                project
                        )
                )
                .toList();
    }


    // MAP SUBMISSION TO HISTORY RESPONSE
    private SubmissionHistoryResponse mapToSubmissionHistoryResponse(
            Submission submission,
            Project project
    ) {

        User submittedBy =
                submission.getProjectMember().getUser();

        // Only comments from the assigned supervisor are treated as official review feedback.

        Integer supervisorId =
                project.getSupervisor() == null
                        ? null
                        : project.getSupervisor().getUserId();

        Comment latestFeedback =
                submission.getComment() == null
                        ? null
                        : submission.getComment()
                        .stream()
                        .filter(comment ->
                                comment.getUser() != null
                                        && supervisorId != null
                                        && supervisorId.equals(
                                        comment.getUser()
                                                .getUserId()
                                )
                        )
                        .max(
                                Comparator
                                        .comparing(
                                                Comment::getCreatedAt
                                        )
                                        .thenComparing(
                                                Comment::getCommentId
                                        )
                        )
                        .orElse(null);

        String feedback =
                latestFeedback == null
                        ? null
                        : latestFeedback.getContent();

        String reviewedByName =
                latestFeedback == null
                        ? null
                        : latestFeedback.getUser().getFullName();

        LocalDateTime feedbackCreatedAt =
                latestFeedback == null
                        ? null
                        : latestFeedback.getCreatedAt();

        return new SubmissionHistoryResponse(
                submission.getSubmissionId(),
                submission.getMilestone().getMilestoneId(),
                submission.getMilestone().getTitle(),
                submission.getFileName(),
                submission.getSubmissionVersion(),
                submission.getSubmittedAt(),
                submission.getStatus(),
                submittedBy.getUserId(),
                submittedBy.getFullName(),
                feedback,
                reviewedByName,
                feedbackCreatedAt
        );
    }


}
