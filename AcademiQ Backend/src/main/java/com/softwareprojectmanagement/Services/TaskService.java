
package com.softwareprojectmanagement.Services;

import com.softwareprojectmanagement.DTO.Request.Task.TaskCreateRequest;
import com.softwareprojectmanagement.DTO.Request.Task.TaskUpdateRequest;
import com.softwareprojectmanagement.DTO.Response.Task.MyTaskResponse;
import com.softwareprojectmanagement.DTO.Response.Task.TaskResponse;
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
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final MilestoneRepository milestoneRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;

    // CREATE PROJECT TASK

    @Transactional
    public TaskResponse createTask(
            Integer projectId,
            TaskCreateRequest request
    ) {

        // STEP 1: Identify authenticated user.
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

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

        // STEP 2: Retrieve the project.
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Project not found."
                        )
                );


        // STEP 3: Validate the project's academic workspace.
        Workspace workspace = project.getWorkspace();

        if (workspace == null
                || workspace.getWorkspaceType()
                != Workspace.WorkspaceType.ACADEMIC
                || workspace.getStatus()
                != Workspace.WorkspaceStatus.ACTIVE) {

            throw new AccessDeniedException(
                    "The project's academic workspace is not active."
            );
        }

        // Require an active STUDENT membership in the workspace that owns this project.
        List<WorkspaceMembership> activeMemberships =
                workspaceMembershipRepository.findUserMembershipsByStatus(
                        currentUser.getUserId(),
                        WorkspaceMembership.MembershipStatus.ACTIVE
                );

        boolean isWorkspaceStudent = activeMemberships.stream()
                .anyMatch(membership ->
                        membership.getWorkspace().getWorkspaceId()
                                .equals(workspace.getWorkspaceId())
                                && membership.getRole()
                                == WorkspaceMembership.WorkspaceRole.STUDENT
                );

        if (!isWorkspaceStudent) {
            throw new AccessDeniedException(
                    "Active student membership in this workspace is required."
            );
        }

        // Only the creator of this project can create tasks.
        boolean isProjectLeader =
                project.getUser() != null
                        && currentUser.getUserId().equals(
                        project.getUser().getUserId()
                );

        if (!isProjectLeader) {
            throw new AccessDeniedException(
                    "Only the project leader can create tasks."
            );
        }


        // STEP 4: Check project workflow status.
        if (!Set.of("APPROVED", "IN_PROGRESS")
                .contains(project.getStatus())) {

            throw new IllegalStateException(
                    "Tasks cannot be created in the current project status."
            );
        }

        // STEP 5: Retrieve the milestone.
        Milestone milestone = milestoneRepository
                .findById(request.getMilestoneId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Milestone not found."
                        )
                );

        // Verify milestone belongs to this project.
        if (milestone.getProject() == null
                || !milestone.getProject().getProject_id()
                .equals(projectId)) {

            throw new IllegalArgumentException(
                    "The milestone does not belong to this project."
            );
        }

        // STEP 6: Retrieve the assigned project member.
        ProjectMember assignedMember =
                projectMemberRepository
                        .findById(request.getMemberId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Project member not found."
                                )
                        );

        // Verify member belongs to this project.
        if (assignedMember.getProject() == null
                || !assignedMember.getProject().getProject_id()
                .equals(projectId)) {

            throw new IllegalArgumentException(
                    "You can only assign tasks to members of this project."
            );
        }

        // STEP 7: Validate task information.
        if (request.getTitle() == null
                || request.getTitle().isBlank()
                || request.getTitle().length() > 200) {

            throw new IllegalArgumentException(
                    "Task title must contain 1 to 200 characters."
            );
        }

        if (request.getDescription() == null
                || request.getDescription().isBlank()) {

            throw new IllegalArgumentException(
                    "Task description is required."
            );
        }

        if (request.getDeadline() == null
                || request.getDeadline().isBefore(LocalDate.now())) {

            throw new IllegalArgumentException(
                    "Task deadline cannot be in the past."
            );
        }

        String priority = request.getPriority();

        if (priority == null
                || !Set.of("LOW", "MEDIUM", "HIGH")
                .contains(priority)) {

            throw new IllegalArgumentException(
                    "Priority must be LOW, MEDIUM, or HIGH."
            );
        }

        // STEP 8: Create task entity.
        Task task = new Task();

        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription().trim());
        task.setPriority(priority);
        task.setDeadline(request.getDeadline());

        task.setStatus("TODO");
        task.setProgress(0);
        task.setCreatedAt(LocalDateTime.now());

        task.setMilestone(milestone);
        task.setProjectMember(assignedMember);

        // STEP 9: Save task in the database.
        Task savedTask = taskRepository.save(task);

        // STEP 10: Return task information.
        return mapToResponse(savedTask);
    }


    // ==========================================
    // GET ALL TASKS OF A PROJECT
    // ==========================================

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<TaskResponse> getProjectTasks(Integer projectId) {

        // STEP 1: Identify the authenticated user.
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

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

        // STEP 2: Check whether the project exists.
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new com.softwareprojectmanagement.Exceptions
                                .NoSuchProjectsException(
                                "Project not found."
                        )
                );


        // STEP 3: Validate the project's workspace.
        Workspace workspace = project.getWorkspace();

        if (workspace == null
                || workspace.getWorkspaceType()
                != Workspace.WorkspaceType.ACADEMIC
                || workspace.getStatus()
                != Workspace.WorkspaceStatus.ACTIVE) {

            throw new AccessDeniedException(
                    "The project's academic workspace is not active."
            );
        }

        // STEP 4: Require active membership in this workspace.
        List<WorkspaceMembership> memberships =
                workspaceMembershipRepository.findUserMembershipsByStatus(
                        currentUser.getUserId(),
                        WorkspaceMembership.MembershipStatus.ACTIVE
                );

        WorkspaceMembership membership = memberships.stream()
                .filter(item ->
                        item.getWorkspace().getWorkspaceId()
                                .equals(workspace.getWorkspaceId())
                )
                .findFirst()
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Active workspace membership is required."
                        )
                );

        // STEP 5: Determine permissions from the workspace role.
        WorkspaceMembership.WorkspaceRole role =
                membership.getRole();

        boolean isAdmin =
                role == WorkspaceMembership.WorkspaceRole.ADMIN;

        boolean isSupervisor =
                role == WorkspaceMembership.WorkspaceRole.SUPERVISOR
                        && project.getSupervisor() != null
                        && project.getSupervisor().getUserId()
                        .equals(currentUser.getUserId());

        boolean isStudentMember =
                role == WorkspaceMembership.WorkspaceRole.STUDENT
                        && project.getProjectMember()
                        .stream()
                        .anyMatch(member ->
                                member.getUser().getUserId()
                                        .equals(currentUser.getUserId())
                        );

        // STEP 6: Reject users without project access.
        if (!isAdmin && !isSupervisor && !isStudentMember) {
            throw new AccessDeniedException(
                    "You are not authorized to view tasks for this project."
            );
        }


        // STEP 7: Retrieve tasks belonging to this project.
        List<Task> tasks =
                taskRepository.findTasksByProjectId(projectId);

        // STEP 8: Convert Task entities into response DTOs.
        return tasks.stream()
                .map(this::mapToResponse)
                .toList();
    }


// GET SINGLE TASK DETAILS

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public TaskResponse getTaskById(
            Integer projectId,
            Integer taskId
    ) {

        // STEP 1: Verify that the authenticated user
        // is authorized to access this project.
        // Reuse the existing authorization logic.
        getProjectTasks(projectId);

        // STEP 2: Find the requested task.
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Task not found."
                        )
                );

        // STEP 3: Verify the task belongs to the
        // project specified in the URL.
        if (task.getMilestone() == null
                || task.getMilestone().getProject() == null
                || !task.getMilestone()
                .getProject()
                .getProject_id()
                .equals(projectId)) {

            throw new IllegalArgumentException(
                    "Task not found in the specified project."
            );
        }

        // STEP 4: Return the safe response DTO.
        return mapToResponse(task);
    }


    // ==========================================
    // UPDATE TASK DETAILS
    // ==========================================

    @Transactional
    public TaskResponse updateTask(
            Integer projectId,
            Integer taskId,
            TaskUpdateRequest request
    ) {

        // STEP 1: Identify the authenticated user.
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

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

        // STEP 2: Retrieve the project.
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Project not found."
                        )
                );


        // STEP 3: Validate the project's academic workspace.
        Workspace workspace = project.getWorkspace();

        if (workspace == null
                || workspace.getWorkspaceType()
                != Workspace.WorkspaceType.ACADEMIC
                || workspace.getStatus()
                != Workspace.WorkspaceStatus.ACTIVE) {

            throw new AccessDeniedException(
                    "The project's academic workspace is not active."
            );
        }

        // Require an active STUDENT membership in the workspace that owns this project.
        List<WorkspaceMembership> activeMemberships =
                workspaceMembershipRepository.findUserMembershipsByStatus(
                        currentUser.getUserId(),
                        WorkspaceMembership.MembershipStatus.ACTIVE
                );

        boolean isWorkspaceStudent = activeMemberships.stream()
                .anyMatch(membership ->
                        membership.getWorkspace().getWorkspaceId()
                                .equals(workspace.getWorkspaceId())
                                && membership.getRole()
                                == WorkspaceMembership.WorkspaceRole.STUDENT
                );

        if (!isWorkspaceStudent) {
            throw new AccessDeniedException(
                    "Active student membership in this workspace is required."
            );
        }

        // Only the project leader may update task details.
        boolean isProjectLeader =
                project.getUser() != null
                        && currentUser.getUserId().equals(
                        project.getUser().getUserId()
                );

        if (!isProjectLeader) {
            throw new AccessDeniedException(
                    "Only the project leader can update task details."
            );
        }


        // STEP 4: Retrieve the task.
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Task not found."
                        )
                );

        // STEP 5: Ensure the task belongs to this project.
        if (task.getMilestone() == null
                || task.getMilestone().getProject() == null
                || !projectId.equals(
                task.getMilestone()
                        .getProject()
                        .getProject_id())) {

            throw new IllegalArgumentException(
                    "Task does not belong to this project."
            );
        }

        // STEP 6: Update only supplied fields.

        if ("UNDER_REVIEW".equalsIgnoreCase(task.getStatus())
                || "COMPLETED".equalsIgnoreCase(task.getStatus())) {

            throw new IllegalStateException(
                    "Tasks under review or completed "
                            + "cannot be edited or reassigned."
            );
        }

        boolean updated = false;

        // Update task title.
        if (request.getTitle() != null) {

            String title = request.getTitle().trim();

            if (title.isEmpty() || title.length() > 200) {
                throw new IllegalArgumentException(
                        "Task title must contain 1 to 200 characters."
                );
            }

            task.setTitle(title);
            updated = true;
        }

        // Update task description.
        if (request.getDescription() != null) {

            String description =
                    request.getDescription().trim();

            if (description.isEmpty()) {
                throw new IllegalArgumentException(
                        "Task description cannot be blank."
                );
            }

            task.setDescription(description);
            updated = true;
        }

        // Update task priority.
        if (request.getPriority() != null) {

            String priority = request.getPriority()
                    .trim()
                    .toUpperCase(java.util.Locale.ROOT);

            if (!Set.of("LOW", "MEDIUM", "HIGH")
                    .contains(priority)) {

                throw new IllegalArgumentException(
                        "Priority must be LOW, MEDIUM, or HIGH."
                );
            }

            task.setPriority(priority);
            updated = true;
        }

        // Update task deadline.
        if (request.getDeadline() != null) {

            if (request.getDeadline()
                    .isBefore(LocalDate.now())) {

                throw new IllegalArgumentException(
                        "Task deadline cannot be in the past."
                );
            }

            task.setDeadline(request.getDeadline());
            updated = true;
        }

        // STEP 7: Reassign task to another project member.
        if (request.getMemberId() != null) {

            ProjectMember assignedMember =
                    projectMemberRepository
                            .findById(request.getMemberId())
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Assigned member not found."
                                    )
                            );

            if (assignedMember.getProject() == null
                    || !projectId.equals(
                    assignedMember.getProject()
                            .getProject_id())) {

                throw new IllegalArgumentException(
                        "The assigned member must belong to this project."
                );
            }

            task.setProjectMember(assignedMember);
            updated = true;
        }

        // STEP 8: Reject empty update requests.
        if (!updated) {
            throw new IllegalArgumentException(
                    "Provide at least one task field to update."
            );
        }

        // STEP 9: Save the updated task.
        Task savedTask = taskRepository.save(task);

        // STEP 10: Return updated task details.
        return mapToResponse(savedTask);
    }



    // ==========================================
    // GET MY ASSIGNED TASKS IN ACADEMIC WORKSPACE
    // ==========================================

    @Transactional
    public List<MyTaskResponse> getMyAssignedTasks() {

        // STEP 1: Identify the authenticated user.
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

        // STEP 2: Verify the academic profile.
        if (currentUser.getProgramme() == null
                || currentUser.getSection() == null
                || currentUser.getSection().getBatch() == null) {

            throw new AccessDeniedException(
                    "Student academic profile is incomplete."
            );
        }

        // STEP 3: Resolve the student's academic workspace.
        Workspace programmeWorkspace =
                currentUser.getProgramme().getWorkspace();

        Workspace batchWorkspace =
                currentUser.getSection()
                        .getBatch()
                        .getWorkspace();

        if (programmeWorkspace == null
                || batchWorkspace == null) {

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

        // STEP 4: Verify the workspace is active and academic.
        if (academicWorkspace.getWorkspaceType()
                != Workspace.WorkspaceType.ACADEMIC
                || academicWorkspace.getStatus()
                != Workspace.WorkspaceStatus.ACTIVE) {

            throw new AccessDeniedException(
                    "The academic workspace is not active."
            );
        }

        // STEP 5: Require active STUDENT membership.
        List<WorkspaceMembership> activeMemberships =
                workspaceMembershipRepository.findUserMembershipsByStatus(
                        currentUser.getUserId(),
                        WorkspaceMembership.MembershipStatus.ACTIVE
                );

        boolean isWorkspaceStudent = activeMemberships.stream()
                .anyMatch(membership ->
                        membership.getWorkspace()
                                .getWorkspaceId()
                                .equals(academicWorkspace.getWorkspaceId())
                                && membership.getRole()
                                == WorkspaceMembership.WorkspaceRole.STUDENT
                );

        if (!isWorkspaceStudent) {
            throw new AccessDeniedException(
                    "Active student membership in this workspace is required."
            );
        }

        // STEP 6: Retrieve only assigned tasks
        // belonging to the authorized workspace.
        List<Task> tasks =
                taskRepository.findAllTasksAssignedToUserInWorkspace(
                        currentUser.getUserId(),
                        academicWorkspace.getWorkspaceId()
                );

        // STEP 7: Convert entities into response DTOs.
        return tasks.stream()
                .map(this::mapToMyTaskResponse)
                .toList();
    }



    // ==========================================
    // MAP TASK TO MY TASK RESPONSE
    // ==========================================

    private MyTaskResponse mapToMyTaskResponse(
            Task task
    ) {

        Milestone milestone = task.getMilestone();

        Project project = milestone.getProject();

        return new MyTaskResponse(
                task.getTaskId(),
                project.getProject_id(),
                project.getTitle(),
                milestone.getMilestoneId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDeadline(),
                task.getProgress()
        );
    }




    // CONVERT TASK ENTITY TO RESPONSE DTO

    private TaskResponse mapToResponse(Task task) {

        ProjectMember assignedMember =
                task.getProjectMember();

        return new TaskResponse(
                task.getTaskId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDeadline(),
                task.getProgress(),
                task.getMilestone().getMilestoneId(),
                assignedMember != null
                        ? assignedMember.getMemberId()
                        : null,
                assignedMember != null
                        && assignedMember.getUser() != null
                        ? assignedMember.getUser().getFullName()
                        : null,
                task.getCreatedAt()
        );
    }


    // DELETE TASK

    @Transactional
    public void deleteTask(
            Integer projectId,
            Integer taskId
    ) {

        // STEP 1: Identify the authenticated user.
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

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

        // STEP 2: Retrieve the project.
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new com.softwareprojectmanagement.Exceptions
                                .NoSuchProjectsException(
                                "Project not found."
                        )
                );


        // STEP 3: Validate the project's academic workspace.
        Workspace workspace = project.getWorkspace();

        if (workspace == null
                || workspace.getWorkspaceType()
                != Workspace.WorkspaceType.ACADEMIC
                || workspace.getStatus()
                != Workspace.WorkspaceStatus.ACTIVE) {

            throw new AccessDeniedException(
                    "The project's academic workspace is not active."
            );
        }

        // Require an active STUDENT membership
        // in the workspace that owns this project.
        List<WorkspaceMembership> activeMemberships =
                workspaceMembershipRepository.findUserMembershipsByStatus(
                        currentUser.getUserId(),
                        WorkspaceMembership.MembershipStatus.ACTIVE
                );

        boolean isWorkspaceStudent = activeMemberships.stream()
                .anyMatch(membership ->
                        membership.getWorkspace().getWorkspaceId()
                                .equals(workspace.getWorkspaceId())
                                && membership.getRole()
                                == WorkspaceMembership.WorkspaceRole.STUDENT
                );

        if (!isWorkspaceStudent) {
            throw new AccessDeniedException(
                    "Active student membership in this workspace is required."
            );
        }

        // Only the project leader can delete tasks.
        boolean isProjectLeader =
                project.getUser() != null
                        && currentUser.getUserId().equals(
                        project.getUser().getUserId()
                );

        if (!isProjectLeader) {
            throw new AccessDeniedException(
                    "Only the project leader can delete tasks."
            );
        }


        // STEP 4: Find the requested task.
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Task not found."
                        )
                );

        // STEP 5: Confirm task belongs to the project.
        if (task.getMilestone() == null
                || task.getMilestone().getProject() == null
                || !projectId.equals(
                task.getMilestone()
                        .getProject()
                        .getProject_id())) {

            throw new IllegalArgumentException(
                    "Task not found in this project."
            );
        }

        // STEP 6: Prevent deleting completed academic work.
        if ("COMPLETED".equals(task.getStatus())) {
            throw new IllegalStateException(
                    "Completed tasks cannot be deleted."
            );
        }

        // STEP 7: Prevent cascading deletion of subtasks.
        if (task.getSubtasks() != null
                && !task.getSubtasks().isEmpty()) {

            throw new IllegalStateException(
                    "Delete the subtasks before deleting this task."
            );
        }

        // STEP 8: Delete the task.
        taskRepository.delete(task);
    }

}
