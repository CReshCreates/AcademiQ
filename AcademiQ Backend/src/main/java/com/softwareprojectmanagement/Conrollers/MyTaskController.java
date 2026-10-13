
package com.softwareprojectmanagement.Conrollers;

import com.softwareprojectmanagement.DTO.Response.Task.MyTaskResponse;
import com.softwareprojectmanagement.Services.TaskService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tasks")
public class MyTaskController {

    private final TaskService taskService;

    // ==========================================
    // GET MY ASSIGNED TASKS ACROSS ALL PROJECTS
    // ==========================================

    @GetMapping("/assigned-to-me")
    public ResponseEntity<List<MyTaskResponse>> getMyAssignedTasks() {

        List<MyTaskResponse> tasks =
                taskService.getMyAssignedTasks();

        return ResponseEntity.ok(tasks);
    }
}
