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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.io.IOException;
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

    public SubmissionPdfResponse getPdfNames(Integer submissionId) throws IOException {

        Submission submission = submissionRepository.getPdfNameBySubmissionId(submissionId);

        if(submission == null){
            throw new NoSubmissionException("There is no submission with id: " + submissionId);
        }

        String relativePath = submission.getFileUrl()
                .replaceFirst("^/uploads/", "");

        Path uploadPath = Paths.get(uploadDir)
                .toAbsolutePath()
                .normalize();

        Path filePath = uploadPath
                .resolve(relativePath)
                .normalize();

        Resource resource = new UrlResource(filePath.toUri());

        if(!resource.exists() || !resource.isReadable()){
            throw new NoSubmissionException("There is no pdf");
        }

        return new SubmissionPdfResponse(resource, submission.getFileName());
    }

    @Transactional
    public String approveSubmission(SubmissionApprovalRequest approvalRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        User user = userRepository.findByEmail(authentication.getName());

        if(user == null){
            throw new UsernameNotFoundException("User not found");
        }

        Submission submission = submissionRepository.findById(approvalRequest.getSubmissionId()).orElseThrow(()->new NoSubmissionException("No Submission found!"));

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
