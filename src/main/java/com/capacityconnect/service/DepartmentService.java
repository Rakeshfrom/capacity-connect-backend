package com.capacityconnect.service;

import com.capacityconnect.dto.DepartmentResponse;
import com.capacityconnect.entity.Department;
import com.capacityconnect.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository repository;

    public List<DepartmentResponse> getActiveDepartments() {
        return repository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public DepartmentResponse getById(Long id) {
        return toResponse(repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found")));
    }

    private DepartmentResponse toResponse(Department department) {
        return DepartmentResponse.builder()
                .id(department.getId())
                .name(department.getName())
                .code(department.getCode())
                .description(department.getDescription())
                .active(department.getActive())
                .build();
    }
}
