package com.capacityconnect.controller;

import com.capacityconnect.dto.DepartmentResponse;
import com.capacityconnect.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService service;

    @GetMapping
    public List<DepartmentResponse> getActiveDepartments() {
        return service.getActiveDepartments();
    }

    @GetMapping("/{id}")
    public DepartmentResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }
}
