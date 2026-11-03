package com.sr.classes.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sr.classes.dto.request.CreateClassRequest;
import com.sr.classes.dto.request.UpdateClassRequest;
import com.sr.classes.dto.response.ClassDetailResponse;
import com.sr.classes.dto.response.ClassResponse;
import com.sr.classes.dto.response.CreateClassResponse;
import com.sr.classes.service.ClassService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/classes")
public class ClassController {

    private final ClassService classService;

    public ClassController(ClassService classService) {
        this.classService = classService;
    }

    @GetMapping
    public List<ClassResponse> getClasses() {
        return classService.getClasses();
    }

    @GetMapping("/{classId}")
    public ClassDetailResponse getClassDetail(
        @PathVariable
        UUID classId
    ) {
        return classService.getClassDetail(classId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateClassResponse createClass(
        @Valid
        @RequestBody
        CreateClassRequest request
    ) {
        return classService.createClass(request);
    }

    @PutMapping("/{classId}")
    public ClassDetailResponse updateClass(
        @PathVariable
        UUID classId,
        @Valid
        @RequestBody
        UpdateClassRequest request
    ) {
        return classService.updateClass(classId, request);
    }

    @DeleteMapping("/{classId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteClass(
        @PathVariable
        UUID classId
    ) {
        classService.deleteClass(classId);
    }

}
