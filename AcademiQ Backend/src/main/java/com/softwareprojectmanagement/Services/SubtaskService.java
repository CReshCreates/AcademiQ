
package com.softwareprojectmanagement.Services;

import com.softwareprojectmanagement.DTO.Request.Subtask.SubtaskCreateRequest;
import com.softwareprojectmanagement.DTO.Request.Subtask.SubtaskStatusUpdateRequest;
import com.softwareprojectmanagement.DTO.Request.Subtask.SubtaskUpdateRequest;
import com.softwareprojectmanagement.DTO.Response.Subtask.SubtaskResponse;

import com.softwareprojectmanagement.Models.*;
import com.softwareprojectmanagement.Repository.*;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubtaskService {

    private final SubtaskRepository subtaskRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;


    // ==========================================
    // GET AUTHENTICATED STUDENT
    // ==========================================

    private User getCurrentStudent() {

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

        if (!"STUDENT".equalsIgnoreCase(
                currentUser.getRole()
        )) {
            throw new AccessDeniedException(
                    "Only students can access this operation."
            );
        }

        return currentUser;
    }


    // ==========================================
    // VERIFY TASK VIEWING ACCESS
    // ==========================================

    private Task getAuthorizedTaskForViewing(
            Integer projectId,
            Integer taskId
    ) {

        User currentUser = getCurrentStudent();

        Task task = taskRepository
                .findById(taskId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Task not found."
                        )
                );

        if (task.getMilestone() == null
                || task.getMilestone().getProject() == null
                || !projectId.equals(
                task.getMilestone()
                        .getProject()
                        .getProject_id()
        )) {
            throw new IllegalArgumentException(
                    "Task does not belong to this project."
            );
        }

        Project project =
                task.getMilestone().getProject();

        boolean isProjectMember =
                project.getProjectMember() != null
                        && project.getProjectMember()
                        .stream()
                        .anyMatch(member ->
                                member.getUser() != null
                                        && currentUser.getUserId().equals(
                                        member.getUser().getUserId()
                                )
                        );

        if (!isProjectMember) {
            throw new AccessDeniedException(
                    "You are not a member of this project."
            );
        }

        return task;
    }



    // ==========================================
    // VERIFY TASK MODIFICATION ACCESS
    // AND LOCK THE PARENT TASK
    // ==========================================

    private Task getAuthorizedTaskForModification(
            Integer projectId,
            Integer taskId
    ) {

        // STEP 1: Identify the authenticated student.
        User currentUser = getCurrentStudent();

        // STEP 2: Retrieve and lock the parent task.
        // The lock is held until the surrounding
        // @Transactional method finishes.
        Task task = taskRepository
                .findByIdForUpdate(taskId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Task not found."
                        )
                );

        // STEP 3: Verify the task belongs
        // to the project in the URL.
        if (task.getMilestone() == null
                || task.getMilestone().getProject() == null
                || !projectId.equals(
                task.getMilestone()
                        .getProject()
                        .getProject_id()
        )) {
            throw new IllegalArgumentException(
                    "Task does not belong to this project."
            );
        }

        // STEP 4: Verify the authenticated student
        // is a member of the parent project.
        Project project =
                task.getMilestone().getProject();

        boolean isProjectMember =
                project.getProjectMember() != null
                        && project.getProjectMember()
                        .stream()
                        .anyMatch(member ->
                                member.getUser() != null
                                        && currentUser.getUserId().equals(
                                        member.getUser().getUserId()
                                )
                        );

        if (!isProjectMember) {
            throw new AccessDeniedException(
                    "You are not a member of this project."
            );
        }

        // STEP 5: Only the member assigned
        // to the parent task can manage its subtasks.
        ProjectMember assignedMember =
                task.getProjectMember();

        if (assignedMember == null
                || assignedMember.getUser() == null
                || assignedMember.getProject() == null
                || !projectId.equals(
                assignedMember.getProject()
                        .getProject_id()
        )
                || !currentUser.getUserId().equals(
                assignedMember.getUser().getUserId()
        )) {
            throw new AccessDeniedException(
                    "Only the member assigned to this task "
                            + "can manage its subtasks."
            );
        }

        return task;
    }



    // ==========================================
    // GET SUBTASK BELONGING TO PARENT TASK
    // ==========================================

    private Subtask getSubtaskForTask(
            Integer taskId,
            Long subtaskId
    ) {

        Subtask subtask = subtaskRepository
                .findById(subtaskId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Subtask not found."
                        )
                );

        if (subtask.getTask() == null
                || !taskId.equals(
                subtask.getTask().getTaskId()
        )) {

            throw new IllegalArgumentException(
                    "Subtask does not belong to this task."
            );
        }

        return subtask;
    }


    // ==========================================
    // VERIFY SUBTASK ASSIGNEE
    // ==========================================

    private User getValidSubtaskAssignee(
            Project project,
            Integer assignedToUserId
    ) {

        User assignedUser = userRepository
                .findById(assignedToUserId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Assigned student not found."
                        )
                );

        boolean isProjectMember =
                project.getProjectMember() != null
                        && project.getProjectMember()
                        .stream()
                        .anyMatch(member ->
                                member.getUser() != null
                                        && assignedUser.getUserId().equals(
                                        member.getUser().getUserId()
                                )
                        );

        if (!isProjectMember) {
            throw new IllegalArgumentException(
                    "The assigned student must be "
                            + "a member of this project."
            );
        }

        return assignedUser;
    }


    // ==========================================
    // GET ALL SUBTASKS OF A TASK
    // ==========================================

    @Transactional
    public List<SubtaskResponse> getTaskSubtasks(
            Integer projectId,
            Integer taskId
    ) {

        getAuthorizedTaskForViewing(projectId, taskId);

        return subtaskRepository
                .findByTaskId(taskId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // ==========================================
    // GET SINGLE SUBTASK
    // ==========================================

    @Transactional
    public SubtaskResponse getSubtaskById(
            Integer projectId,
            Integer taskId,
            Long subtaskId
    ) {

        getAuthorizedTaskForViewing(projectId, taskId);

        Subtask subtask = getSubtaskForTask(
                taskId,
                subtaskId
        );

        return mapToResponse(subtask);
    }


    // ==========================================
    // CREATE SUBTASK
    // ==========================================

    @Transactional
    public SubtaskResponse createSubtask(
            Integer projectId,
            Integer taskId,
            SubtaskCreateRequest request
    ) {

        Task task = getAuthorizedTaskForModification(
                projectId,
                taskId
        );

        if (request == null
                || request.getTitle() == null
                || request.getTitle().isBlank()) {

            throw new IllegalArgumentException(
                    "Subtask title is required."
            );
        }

        String title = request.getTitle().trim();

        if (title.length() > 255) {
            throw new IllegalArgumentException(
                    "Subtask title cannot exceed 255 characters."
            );
        }

        String description = request.getDescription();

        if (description != null
                && description.length() > 5000) {
            throw new IllegalArgumentException(
                    "Subtask description cannot exceed 5000 characters."
            );
        }

        User assignedUser = null;

        if (request.getAssignedToUserId() != null) {
            Project project =
                    task.getMilestone().getProject();

            assignedUser = getValidSubtaskAssignee(
                    project,
                    request.getAssignedToUserId()
            );
        }

        Subtask subtask = new Subtask();

        subtask.setTask(task);
        subtask.setTitle(title);
        subtask.setDescription(
                description == null
                        ? null
                        : description.trim()
        );
        subtask.setStatus(SubtaskStatus.TODO);
        subtask.setAssignedTo(assignedUser);

        // Flush the new subtask before counting.
        Subtask savedSubtask =
                subtaskRepository.saveAndFlush(subtask);

        // Recalculate progress after creation.
        recalculateTaskProgress(task);

        return mapToResponse(savedSubtask);
    }


    // ==========================================
    // UPDATE SUBTASK DETAILS
    // ==========================================

    @Transactional
    public SubtaskResponse updateSubtask(
            Integer projectId,
            Integer taskId,
            Long subtaskId,
            SubtaskUpdateRequest request
    ) {

        Task task = getAuthorizedTaskForModification(
                projectId,
                taskId
        );

        if (request == null) {
            throw new IllegalArgumentException(
                    "Subtask update request is required."
            );
        }

        Subtask subtask = getSubtaskForTask(
                taskId,
                subtaskId
        );

        // Update title.
        if (request.getTitle() != null) {

            String title = request.getTitle().trim();

            if (title.isBlank()
                    || title.length() > 255) {

                throw new IllegalArgumentException(
                        "Subtask title must contain "
                                + "1 to 255 characters."
                );
            }

            subtask.setTitle(title);
        }

        // Update description.
        if (request.getDescription() != null) {

            String description =
                    request.getDescription().trim();

            if (description.length() > 5000) {
                throw new IllegalArgumentException(
                        "Subtask description cannot exceed "
                                + "5000 characters."
                );
            }

            subtask.setDescription(description);
        }

        // Update assignee.
        if (request.getAssignedToUserId() != null) {

            Project project =
                    task.getMilestone().getProject();

            User assignedUser = getValidSubtaskAssignee(
                    project,
                    request.getAssignedToUserId()
            );

            subtask.setAssignedTo(assignedUser);
        }

        Subtask savedSubtask =
                subtaskRepository.save(subtask);

        // No progress recalculation is needed here:
        // title, description, and assignee do not
        // change the completed-subtask count.
        return mapToResponse(savedSubtask);
    }


    // ==========================================
    // UPDATE SUBTASK STATUS
    // ==========================================

    @Transactional
    public SubtaskResponse updateSubtaskStatus(
            Integer projectId,
            Integer taskId,
            Long subtaskId,
            SubtaskStatusUpdateRequest request
    ) {

        Task task = getAuthorizedTaskForModification(
                projectId,
                taskId
        );

        if (request == null
                || request.getStatus() == null) {

            throw new IllegalArgumentException(
                    "Subtask status is required."
            );
        }

        Subtask subtask = getSubtaskForTask(
                taskId,
                subtaskId
        );

        // Apply status change.
        subtask.setStatus(request.getStatus());

        // Flush before recalculating the counts.
        Subtask savedSubtask =
                subtaskRepository.saveAndFlush(subtask);

        // One shared progress calculation.
        recalculateTaskProgress(task);

        return mapToResponse(savedSubtask);
    }



    // ==========================================
    // DELETE SUBTASK
    // ==========================================

    @Transactional
    public void deleteSubtask(
            Integer projectId,
            Integer taskId,
            Long subtaskId
    ) {

        // Verify the authenticated student is assigned to the parent task.
        Task task = getAuthorizedTaskForModification(
                projectId,
                taskId
        );

        // Retrieve the subtask and verify that it belongs to the parent task.
        Subtask subtask = getSubtaskForTask(
                taskId,
                subtaskId
        );

        // Delete and flush the subtask.
        subtaskRepository.delete(subtask);
        subtaskRepository.flush();

        // Recalculate progress from the remaining subtasks.
        recalculateTaskProgress(task);
    }

    // ==========================================
    // RECALCULATE TASK PROGRESS AND STATUS
    // ==========================================

    private void recalculateTaskProgress(Task task) {

        Integer taskId = task.getTaskId();

        // Count total subtasks.
        long totalSubtasks =
                subtaskRepository.countByTaskId(taskId);

        // Count completed subtasks.
        long completedSubtasks =
                subtaskRepository.countByTaskIdAndStatus(
                        taskId,
                        SubtaskStatus.COMPLETED
                );

        // Count subtasks currently in progress.
        long inProgressSubtasks =
                subtaskRepository.countByTaskIdAndStatus(
                        taskId,
                        SubtaskStatus.IN_PROGRESS
                );

        // Calculate percentage.
        int progress = totalSubtasks == 0
                ? 0
                : (int) Math.round(
                completedSubtasks * 100.0
                        / totalSubtasks
        );

        task.setProgress(progress);

        // Determine task status automatically.
        if (totalSubtasks == 0) {

            task.setStatus("TODO");

        } else if (completedSubtasks == totalSubtasks) {

            // All subtasks completed.
            task.setStatus("COMPLETED");

        } else if (completedSubtasks > 0
                || inProgressSubtasks > 0) {

            // At least one subtask has started.
            task.setStatus("IN_PROGRESS");

        } else {

            // All subtasks remain TODO.
            task.setStatus("TODO");
        }

        // Persist the parent task.
        taskRepository.save(task);
    }



    // ==========================================
    // MAP ENTITY TO RESPONSE DTO
    // ==========================================

    private SubtaskResponse mapToResponse(
            Subtask subtask
    ) {

        User assignedUser = subtask.getAssignedTo();

        return new SubtaskResponse(
                subtask.getSubtaskId(),
                subtask.getTask().getTaskId(),
                subtask.getTitle(),
                subtask.getDescription(),
                subtask.getStatus(),
                assignedUser != null
                        ? assignedUser.getUserId()
                        : null,
                assignedUser != null
                        ? assignedUser.getFullName()
                        : null
        );
    }
}
