package com.ecoal.api.controller;

import com.ecoal.api.dto.task.TaskRequest;
import com.ecoal.api.dto.task.TaskResponse;
import com.ecoal.api.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/goals/{goalId}/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @GetMapping
    public ResponseEntity<List<TaskResponse>> list(
            @PathVariable Long goalId,
            @RequestParam(required = false) Boolean completed,
            @RequestParam(name = "created_at", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAt) {
        return ResponseEntity.ok(taskService.list(goalId, completed, createdAt));
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(
            @PathVariable Long goalId,
            @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.status(201).body(taskService.create(goalId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> get(
            @PathVariable Long goalId,
            @PathVariable Long id) {
        return ResponseEntity.ok(taskService.get(goalId, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> update(
            @PathVariable Long goalId,
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.ok(taskService.update(goalId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long goalId,
            @PathVariable Long id) {
        taskService.delete(goalId, id);
        return ResponseEntity.noContent().build();
    }
}
