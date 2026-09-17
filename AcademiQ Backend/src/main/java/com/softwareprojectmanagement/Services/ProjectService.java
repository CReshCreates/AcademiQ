package com.softwareprojectmanagement.Services;

import com.softwareprojectmanagement.DTO.Response.Project.*;
import com.softwareprojectmanagement.DTO.Request.Project.ProjectCreationRequest;
import com.softwareprojectmanagement.DTO.Response.Submission.SubmissionPdfResponse;
import com.softwareprojectmanagement.DTO.Response.Submission.SubmissionQueueResponse;
import com.softwareprojectmanagement.Exceptions.NoAssignedSupervisorException;
import com.softwareprojectmanagement.Exceptions.NoSubmissionException;
import com.softwareprojectmanagement.Exceptions.SubjectNotFoundException;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final SubjectSupervisorRepository subjectSupervisorRepository;
    private final ProjectRepository projectRepository;
    private final ProgrammeSubjectRepository programmeSubjectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectActivitiesRepository projectActivitiesRepository;
    private final FileStorageService fileStorageService;
    private final MilestoneRepository milestoneRepository;
    private final SubmissionRepository submissionRepository;

    public List<AvailableSubjectsForProjectCreation> getSubjects(String email){
        User user = userRepository.findByEmail(email);
        Batch batch = user.getSection().getBatch();

        LocalDate startingYear = batch.getStartYear();
        LocalDate currentYear = LocalDate.now();

        Long monthsBetween = ChronoUnit.MONTHS.between(startingYear, currentYear);

        int sem = (int) (monthsBetween / 6) + 1;

        sem = Math.min(sem, 8);


        List<AvailableSubjectsForProjectCreation> subjects = subjectRepository.getSubjects(email, sem);

        if(subjects.isEmpty()){
            throw new SubjectNotFoundException("There is no subject in this semester that require project submission.");
        }

        return subjects;
    }

    public List<AvailableSupervisor> getSupervisors(Integer subjectId){
        List<AvailableSupervisor> supervisors = subjectSupervisorRepository.getSupervisorBySubject(subjectId);
        if(supervisors.isEmpty()){
            throw new NoAssignedSupervisorException("No supervisors are currently assigned for this subject ");
        }
        return supervisors;
    }

    @Transactional
    public CreatedProjectResponse getCreatedProject(ProjectCreationRequest projectCreationRequest){

        String proposedPdfUrl = fileStorageService.storeProposalPdf(projectCreationRequest.getUploadFile());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        User user = userRepository.findByEmail(authentication.getName());

        if(user == null){
            throw new UsernameNotFoundException("User not found");
        }

        ProgrammeSubject programmeSubject = programmeSubjectRepository.findByProgrammeSubjectId(projectCreationRequest.getProgrammeSubjectId()).orElseThrow(() -> new RuntimeException("Programme subject not found."));

        SubjectSupervisor subjectSupervisor =
                subjectSupervisorRepository
                        .findByUser_UserIdAndProgrammeSubject_ProgrammeSubjectId(
                                projectCreationRequest.getSupervisorId(),
                                projectCreationRequest.getProgrammeSubjectId()
                        )
                        .orElseThrow(() ->
                                new NoAssignedSupervisorException(
                                        "Supervisor is not assigned to this subject."
                                )
                        );

        Project createdProject = new Project();

        createdProject.setTitle(projectCreationRequest.getProjectTitle());
        createdProject.setDescription(projectCreationRequest.getProjectDescription());
        createdProject.setStatus("PROPOSED");
        createdProject.setUser(user);
        createdProject.setProgrammeSubject(programmeSubject);
        createdProject.setSupervisor(subjectSupervisor.getUser());
        createdProject.setCreated_at(LocalDateTime.now());
        projectRepository.save(createdProject);

        ProjectMember createdProjectMember = new ProjectMember();
        createdProjectMember.setUser(user);
        createdProjectMember.setRole("LEADER");
        createdProjectMember.setProject(createdProject);
        createdProjectMember.setJoinedAt(LocalDateTime.now());
        projectMemberRepository.save(createdProjectMember);

        Milestone projectProposal = new Milestone();
        projectProposal.setProject(createdProject);
        projectProposal.setTitle("Project Proposal");
        projectProposal.setDescription("Initial project proposal and supervisor approval.");
        projectProposal.setStatus("PENDING");
        projectProposal.setProgress(0);
        Milestone savedMilestone = milestoneRepository.save(projectProposal);

        Milestone requirements = new Milestone();
        requirements.setProject(createdProject);
        requirements.setTitle("Requirements Analysis");
        requirements.setDescription("Analysis and documentation of system requirements.");
        requirements.setStatus("PENDING");
        requirements.setProgress(0);
        milestoneRepository.save(requirements);

        Milestone systemDesign = new Milestone();
        systemDesign.setProject(createdProject);
        systemDesign.setTitle("System Design");
        systemDesign.setDescription("Design of the system architecture, database and user interface.");
        systemDesign.setStatus("PENDING");
        systemDesign.setProgress(0);
        milestoneRepository.save(systemDesign);

        Milestone implementation = new Milestone();
        implementation.setProject(createdProject);
        implementation.setTitle("Implementation");
        implementation.setDescription("Development and implementation of the system.");
        implementation.setStatus("PENDING");
        implementation.setProgress(0);
        milestoneRepository.save(implementation);

        Milestone testing = new Milestone();
        testing.setProject(createdProject);
        testing.setTitle("Testing & QA");
        testing.setDescription("Testing, debugging and quality assurance of the system.");
        testing.setStatus("PENDING");
        testing.setProgress(0);
        milestoneRepository.save(testing);

        Milestone finalSubmission = new Milestone();
        finalSubmission.setProject(createdProject);
        finalSubmission.setTitle("Final Submission");
        finalSubmission.setDescription("Final project submission and completion.");
        finalSubmission.setStatus("PENDING");
        finalSubmission.setProgress(0);
        milestoneRepository.save(finalSubmission);

        Submission submission = new Submission();
        submission.setProjectMember(createdProjectMember);
        submission.setFileUrl(proposedPdfUrl);
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setStatus("PENDING");
        submission.setMilestone(savedMilestone);
        submission.setFileName(projectCreationRequest.getUploadFile().getOriginalFilename());
        submission.setSubmissionVersion(1);
        submissionRepository.save(submission);

        ProjectActivities projectActivities = new ProjectActivities();
        projectActivities.setProject(createdProject);
        projectActivities.setUser(user);
        projectActivities.setActivityType("PROJECT_CREATED");
        projectActivities.setDescription(user.getFullName() + " Created this project.");
        projectActivities.setCreatedAt(LocalDateTime.now());
        projectActivitiesRepository.save(projectActivities);

        return projectRepository.getCreatedProject(createdProject.getProject_id());
    }
}
