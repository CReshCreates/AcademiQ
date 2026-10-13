
package com.softwareprojectmanagement.Conrollers;

import com.softwareprojectmanagement.DTO.Request.Subtask.SubtaskCreateRequest;
import com.softwareprojectmanagement.DTO.Request.Subtask.SubtaskUpdateRequest;
import com.softwareprojectmanagement.DTO.Request.Subtask.SubtaskStatusUpdateRequest;
import com.softwareprojectmanagement.DTO.Response.Subtask.SubtaskResponse;
import com.softwareprojectmanagement.Services.SubtaskService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects/{projectId}/tasks/{taskId}/subtasks")
public class SubtaskController {

    private final SubtaskService subtaskService;


    // ==========================================
    // CREATE SUBTASK
    // ==========================================

    @PostMapping
    public ResponseEntity<SubtaskResponse> createSubtask(
            @PathVariable Integer projectId,
            @PathVariable Integer taskId,
            @Valid @RequestBody SubtaskCreateRequest request
    ) {

        SubtaskResponse response =
                subtaskService.createSubtask(
                        projectId,
                        taskId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // ==========================================
    // GET ALL SUBTASKS OF A TASK
    // ==========================================

    @GetMapping
    public ResponseEntity<List<SubtaskResponse>> getTaskSubtasks(
            @PathVariable Integer projectId,
            @PathVariable Integer taskId
    ) {

        List<SubtaskResponse> responses =
                subtaskService.getTaskSubtasks(
                        projectId,
                        taskId
                );

        return ResponseEntity.ok(responses);
    }


    // ==========================================
    // GET SINGLE SUBTASK
    // ==========================================

    @GetMapping("/{subtaskId}")
    public ResponseEntity<SubtaskResponse> getSubtaskById(
            @PathVariable Integer projectId,
            @PathVariable Integer taskId,
            @PathVariable Long subtaskId
    ) {

        SubtaskResponse response =
                subtaskService.getSubtaskById(
                        projectId,
                        taskId,
                        subtaskId
                );

        return ResponseEntity.ok(response);
    }


    // ==========================================
    // UPDATE SUBTASK DETAILS
    // ==========================================

    @PatchMapping("/{subtaskId}")
    public ResponseEntity<SubtaskResponse> updateSubtask(
            @PathVariable Integer projectId,
            @PathVariable Integer taskId,
            @PathVariable Long subtaskId,
            @Valid @RequestBody SubtaskUpdateRequest request
    ) {

        SubtaskResponse response =
                subtaskService.updateSubtask(
                        projectId,
                        taskId,
                        subtaskId,
                        request
                );

        return ResponseEntity.ok(response);
    }


    // ==========================================
    // UPDATE SUBTASK STATUS
    // ==========================================

    @PatchMapping("/{subtaskId}/status")
    public ResponseEntity<SubtaskResponse> updateSubtaskStatus(
            @PathVariable Integer projectId,
            @PathVariable Integer taskId,
            @PathVariable Long subtaskId,
            @Valid @RequestBody SubtaskStatusUpdateRequest request
    ) {

        SubtaskResponse response =
                subtaskService.updateSubtaskStatus(
                        projectId,
                        taskId,
                        subtaskId,
                        request
                );

        return ResponseEntity.ok(response);
    }


    // ==========================================
    // DELETE SUBTASK
    // ==========================================

    @DeleteMapping("/{subtaskId}")
    public ResponseEntity<Void> deleteSubtask(
            @PathVariable Integer projectId,
            @PathVariable Integer taskId,
            @PathVariable Long subtaskId
    ) {

        subtaskService.deleteSubtask(
                projectId,
                taskId,
                subtaskId
        );

        return ResponseEntity.noContent().build();
    }
}
