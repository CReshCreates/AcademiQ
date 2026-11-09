package com.softwareprojectmanagement.Services;

import com.softwareprojectmanagement.DTO.Response.Project.*;
import com.softwareprojectmanagement.DTO.Request.Project.ProjectCreationRequest;
import com.softwareprojectmanagement.Exceptions.NoAssignedSupervisorException;
import com.softwareprojectmanagement.Exceptions.SubjectNotFoundException;
import com.softwareprojectmanagement.Models.*;
import com.softwareprojectmanagement.Repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

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
    private final TaskRepository taskRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;

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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        User user = userRepository
                .findByEmailForUpdate(authentication.getName())
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Authenticated user not found."
                        )
                );


        if (!"STUDENT".equals(user.getRole())) {
            throw new AccessDeniedException(
                    "Only students are allowed to create academic projects."
            );
        }


        ProgrammeSubject programmeSubject = programmeSubjectRepository.findByProgrammeSubjectId(projectCreationRequest.getProgrammeSubjectId()).orElseThrow(() -> new RuntimeException("Programme subject not found."));


        if (user.getProgramme() == null
                || user.getSection() == null
                || user.getSection().getBatch() == null) {

            throw new IllegalStateException(
                    "Student academic profile is incomplete."
            );
        }


        // Resolve the academic workspace from the student's
        // existing programme and batch assignments.

        Workspace programmeWorkspace =
                user.getProgramme().getWorkspace();

        Workspace batchWorkspace =
                user.getSection().getBatch().getWorkspace();

        if (programmeWorkspace == null || batchWorkspace == null) {
            throw new IllegalStateException(
                    "Student programme or batch is not assigned to a workspace."
            );
        }

        if (!programmeWorkspace.getWorkspaceId()
                .equals(batchWorkspace.getWorkspaceId())) {
            throw new IllegalStateException(
                    "Student programme and batch belong to different workspaces."
            );
        }

        Workspace academicWorkspace = programmeWorkspace;

        if (academicWorkspace.getWorkspaceType()
                != Workspace.WorkspaceType.ACADEMIC
                || academicWorkspace.getStatus()
                != Workspace.WorkspaceStatus.ACTIVE) {
            throw new AccessDeniedException(
                    "The academic workspace is not active."
            );
        }

        List<WorkspaceMembership> activeMemberships =
                workspaceMembershipRepository.findUserMembershipsByStatus(
                        user.getUserId(),
                        WorkspaceMembership.MembershipStatus.ACTIVE
                );

        boolean hasStudentMembership = activeMemberships.stream()
                .anyMatch(membership ->
                        membership.getWorkspace().getWorkspaceId()
                                .equals(academicWorkspace.getWorkspaceId())
                                && membership.getRole()
                                == WorkspaceMembership.WorkspaceRole.STUDENT
                );

        if (!hasStudentMembership) {
            throw new AccessDeniedException(
                    "You do not have an active student membership in this academic workspace."
            );
        }


        List<AvailableSubjectsForProjectCreation> eligibleSubjects =
                getSubjects(user.getEmail());

        boolean subjectIsEligible = eligibleSubjects.stream()
                .anyMatch(subject ->
                        subject.getProgrammeSubjectId()
                                .equals(programmeSubject.getProgrammeSubjectId())
                );

        if (!subjectIsEligible) {
            throw new IllegalStateException(
                    "This subject is not available for your programme and current semester."
            );
        }

        boolean alreadyParticipating =
                projectMemberRepository.existsByUserAndSubject(
                        user.getUserId(),
                        programmeSubject.getSubject().getSubjectId()
                );

        if (alreadyParticipating) {
            throw new IllegalStateException(
                    "You are already participating in a project for this subject."
            );
        }

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

        String proposedPdfUrl = fileStorageService.storeProposalPdf(
                projectCreationRequest.getUploadFile()
        );

        Project createdProject = new Project();

        createdProject.setTitle(projectCreationRequest.getProjectTitle());
        createdProject.setDescription(projectCreationRequest.getProjectDescription());
        createdProject.setStatus("PROPOSED");
        createdProject.setUser(user);
        createdProject.setProgrammeSubject(programmeSubject);
        createdProject.setSupervisor(subjectSupervisor.getUser());
        createdProject.setWorkspace(academicWorkspace);
        createdProject.setCreated_at(LocalDateTime.now());
        projectRepository.save(createdProject);

        ProjectMember createdProjectMember = new ProjectMember();
        createdProjectMember.setUser(user);
        createdProjectMember.setRole("LEADER");
        createdProjectMember.setProject(createdProject);
        createdProjectMember.setJoinedAt(LocalDateTime.now());
        projectMemberRepository.save(createdProjectMember);

        // MILESTONE 1: PROPOSAL & REQUIREMENTS
        Milestone projectProposal = new Milestone();
        projectProposal.setProject(createdProject);
        projectProposal.setTitle(
                "Project Proposal & Requirements Collection"
        );
        projectProposal.setDescription(
                "Project proposal, objectives, scope, feasibility "
                        + "and software requirements collection."
        );
        projectProposal.setStatus("PENDING");
        projectProposal.setProgress(0);

        Milestone savedMilestone =
                milestoneRepository.save(projectProposal);


        // MILESTONE 2: DESIGN & IMPLEMENTATION
        Milestone designImplementation = new Milestone();
        designImplementation.setProject(createdProject);
        designImplementation.setTitle(
                "System Design & Implementation"
        );
        designImplementation.setDescription(
                "System architecture, UML diagrams, database design, "
                        + "user interface design and core implementation."
        );
        designImplementation.setStatus("PENDING");
        designImplementation.setProgress(0);

        milestoneRepository.save(designImplementation);

        // MILESTONE 3: TESTING & DOCUMENTATION
        Milestone testingDocumentation = new Milestone();
        testingDocumentation.setProject(createdProject);
        testingDocumentation.setTitle(
                "Testing & Documentation"
        );
        testingDocumentation.setDescription(
                "System testing, debugging, test reports, "
                        + "final documentation and project submission."
        );
        testingDocumentation.setStatus("PENDING");
        testingDocumentation.setProgress(0);

        milestoneRepository.save(testingDocumentation);


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



    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<CreatedProjectResponse> getMyProjects() {

        // Identify the authenticated user.
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {
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

        // This endpoint currently serves the academic Student Portal.
        if (user.getProgramme() == null
                || user.getSection() == null
                || user.getSection().getBatch() == null) {
            throw new AccessDeniedException(
                    "Student academic profile is incomplete."
            );
        }

        Workspace programmeWorkspace =
                user.getProgramme().getWorkspace();

        Workspace batchWorkspace =
                user.getSection().getBatch().getWorkspace();

        if (programmeWorkspace == null || batchWorkspace == null) {
            throw new AccessDeniedException(
                    "Student programme or batch has no workspace."
            );
        }

        if (!programmeWorkspace.getWorkspaceId()
                .equals(batchWorkspace.getWorkspaceId())) {
            throw new AccessDeniedException(
                    "Student programme and batch belong to different workspaces."
            );
        }

        Workspace academicWorkspace = programmeWorkspace;

        if (academicWorkspace.getWorkspaceType()
                != Workspace.WorkspaceType.ACADEMIC
                || academicWorkspace.getStatus()
                != Workspace.WorkspaceStatus.ACTIVE) {
            throw new AccessDeniedException(
                    "The academic workspace is not active."
            );
        }

        // Authorize using workspace-specific membership, not the user's global role.
        List<WorkspaceMembership> activeMemberships =
                workspaceMembershipRepository.findUserMembershipsByStatus(
                        user.getUserId(),
                        WorkspaceMembership.MembershipStatus.ACTIVE
                );

        boolean hasStudentMembership = activeMemberships.stream()
                .anyMatch(membership ->
                        membership.getWorkspace().getWorkspaceId()
                                .equals(academicWorkspace.getWorkspaceId())
                                && membership.getRole()
                                == WorkspaceMembership.WorkspaceRole.STUDENT
                );

        if (!hasStudentMembership) {
            throw new AccessDeniedException(
                    "You do not have an active student membership in this workspace."
            );
        }

        // Return only projects from the authorized workspace.
        return projectRepository.getMyProjectsInWorkspace(
                user.getUserId(),
                academicWorkspace.getWorkspaceId()
        );
    }



    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public MyProjectsPageInfo getMyProjectsPageInfo(Integer projectId) {

        // 1. Get the authenticated user.
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {
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

        // 2. Check that the project exists.
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new com.softwareprojectmanagement.Exceptions
                                .NoSuchProjectsException(
                                "Project not found."
                        )
                );


        // 3. Verify that the project belongs to an active academic workspace.

        Workspace projectWorkspace = project.getWorkspace();

        if (projectWorkspace == null) {
            throw new AccessDeniedException(
                    "This project is not assigned to a workspace."
            );
        }

        if (projectWorkspace.getWorkspaceType()
                != Workspace.WorkspaceType.ACADEMIC
                || projectWorkspace.getStatus()
                != Workspace.WorkspaceStatus.ACTIVE) {
            throw new AccessDeniedException(
                    "The project's academic workspace is not active."
            );
        }

        // 4. Retrieve the user's active workspace memberships.

        List<WorkspaceMembership> activeMemberships =
                workspaceMembershipRepository.findUserMembershipsByStatus(
                        user.getUserId(),
                        WorkspaceMembership.MembershipStatus.ACTIVE
                );

        WorkspaceMembership projectMembership =
                activeMemberships.stream()
                        .filter(membership ->
                                membership.getWorkspace()
                                        .getWorkspaceId()
                                        .equals(projectWorkspace.getWorkspaceId())
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new AccessDeniedException(
                                        "You are not an active member of this project's workspace."
                                )
                        );

        // 5. Determine permissions from the workspace role, not the user's global role.

        WorkspaceMembership.WorkspaceRole workspaceRole =
                projectMembership.getRole();

        boolean isAdmin =
                workspaceRole == WorkspaceMembership.WorkspaceRole.ADMIN;

        boolean isSupervisor =
                workspaceRole == WorkspaceMembership.WorkspaceRole.SUPERVISOR
                        && project.getSupervisor() != null
                        && project.getSupervisor()
                        .getUserId()
                        .equals(user.getUserId());

        boolean isStudentMember =
                workspaceRole == WorkspaceMembership.WorkspaceRole.STUDENT
                        && project.getProjectMember()
                        .stream()
                        .anyMatch(member ->
                                member.getUser()
                                        .getUserId()
                                        .equals(user.getUserId())
                        );

        // 6. Allow only authorized members of this workspace.

        if (!isAdmin && !isSupervisor && !isStudentMember) {
            throw new AccessDeniedException(
                    "You are not authorized to access this project."
            );
        }


        // 6. Retrieve dashboard information after authorization.
        ProjectKPICards kpiCards =
                projectRepository.getProjectKPICards(projectId);

        List<TeamMembers> teamMembers =
                projectRepository.getTeamMembers(projectId);

        MilestoneDeadlineCard milestoneDeadlineCard =
                milestoneRepository.getMilestoneDeadline(projectId);

        FeedbackCard feedbackCard =
                projectActivitiesRepository.getFeedbackCard(projectId);

        List<RecentActivitiesCard> recentActivities =
                projectActivitiesRepository.getRecentActivities(projectId);


        // BUILD PROJECT OVERVIEW RESPONSE
        MyProjectsPageInfo response = new MyProjectsPageInfo(
                kpiCards,
                teamMembers,
                milestoneDeadlineCard,
                feedbackCard,
                recentActivities
        );

        // Basic project information
        response.setProjectId(project.getProject_id());
        response.setProjectTitle(project.getTitle());
        response.setProjectDescription(project.getDescription());
        response.setProjectStatus(project.getStatus());
        response.setCreatedAt(project.getCreated_at());


        // CALCULATE OVERALL PROJECT PROGRESS
        Double averageProgress =
                taskRepository.getAverageTaskProgressByProjectId(
                        projectId
                );

        int overallProgress = averageProgress == null
                ? 0
                : (int) Math.round(averageProgress);

        // Keep the percentage within the valid range.
        overallProgress = Math.max(
                0,
                Math.min(100, overallProgress)
        );

        response.setOverallProgress(overallProgress);


        // Assigned supervisor information
        if (project.getSupervisor() != null) {
            response.setSupervisorId(
                    project.getSupervisor().getUserId()
            );

            response.setSupervisorName(
                    project.getSupervisor().getFullName()
            );
        }

        return response;

    }
}
