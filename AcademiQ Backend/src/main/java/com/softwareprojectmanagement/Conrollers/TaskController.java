
package com.softwareprojectmanagement.Conrollers;

import com.softwareprojectmanagement.DTO.Request.Task.TaskCreateRequest;
import com.softwareprojectmanagement.DTO.Request.Task.TaskUpdateRequest;
import com.softwareprojectmanagement.DTO.Response.Task.TaskResponse;
import com.softwareprojectmanagement.Services.TaskService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    // CREATE TASK

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable Integer projectId,
            @Valid @RequestBody TaskCreateRequest request
    ) {

        TaskResponse response =
                taskService.createTask(projectId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // GET ALL TASKS OF A PROJECT

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getProjectTasks(
            @PathVariable Integer projectId
    ) {

        List<TaskResponse> tasks =
                taskService.getProjectTasks(projectId);

        return ResponseEntity.ok(tasks);
    }


    // ==========================================
    // GET SINGLE TASK BY ID
    // ==========================================

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> getTaskById(
            @PathVariable Integer projectId,
            @PathVariable Integer taskId
    ) {

        TaskResponse task =
                taskService.getTaskById(projectId, taskId);

        return ResponseEntity.ok(task);
    }

    // UPDATE TASK DETAILS

    @PatchMapping("/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Integer projectId,
            @PathVariable Integer taskId,
            @Valid @RequestBody TaskUpdateRequest request
    ) {

        TaskResponse updatedTask = taskService.updateTask(
                projectId,
                taskId,
                request
        );

        return ResponseEntity.ok(updatedTask);
    }
    
    // DELETE TASK

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Integer projectId,
            @PathVariable Integer taskId
    ) {

        taskService.deleteTask(projectId, taskId);

        return ResponseEntity.noContent().build();
    }

}
