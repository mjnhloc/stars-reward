package com.sr.student.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sr.reward.service.RewardCalculationService;
import com.sr.student.dto.request.CreateStudentRequest;
import com.sr.student.dto.request.RestoreStudentRequest;
import com.sr.student.dto.request.StudentStarsAdjustmentRequest;
import com.sr.student.dto.request.UpdateStudentRequest;
import com.sr.student.dto.response.StudentResponse;
import com.sr.student.dto.response.StudentScoreResponse;
import com.sr.student.dto.response.StudentStarsAdjustmentResponse;
import com.sr.student.dto.response.StudentSummary;
import com.sr.student.dto.response.StudentTransactionResponse;
import com.sr.student.dto.response.StudentTrashResponse;
import com.sr.student.service.StudentService;
import com.sr.user.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentService studentService;
    private final UserService userService;

    public StudentController(
        StudentService studentService,
        UserService userService,
        RewardCalculationService rewardCalculationService
    ) {
        this.studentService = studentService;
        this.userService = userService;
    }

    @GetMapping
    public List<StudentResponse> getStudents(
        @RequestParam(required = false)
        UUID classId,
        @RequestParam(defaultValue = "")
        String query
    ) {
        return studentService.getStudents(classId, query);
    }

    @GetMapping("/trash")
    public List<StudentTrashResponse> getStudentTrash() {
        return studentService.getStudentTrash();
    }

    @GetMapping("/{studentId}/transactions")
    public List<StudentTransactionResponse> getStudentTransactions(
        @PathVariable
        UUID studentId
    ) {
        return studentService.getStudentTransactions(studentId);
    }

    @PostMapping("/{studentId}/stars/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public StudentStarsAdjustmentResponse adjustStudentStars(
        @PathVariable
        UUID studentId,
        @RequestHeader("Idempotency-Key")
        String key,
        @Valid
        @RequestBody
        StudentStarsAdjustmentRequest request
    ) {
        return studentService.adjustStudentStars(userService.getUserId(), studentId, request, key);
    }

    @GetMapping("/{studentId}/scores")
    public List<StudentScoreResponse> getStudentScores(
        @PathVariable
        UUID studentId
    ) {
        return studentService.getStudentScores(studentId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentSummary createStudent(
        @Valid
        @RequestBody
        CreateStudentRequest request
    ) {
        return studentService.createStudent(request);
    }

    @PutMapping("/{studentId}")
    public StudentSummary updateStudent(
        @PathVariable
        UUID studentId,
        @Valid
        @RequestBody
        UpdateStudentRequest request
    ) {
        return studentService.updateStudent(studentId, request);
    }

    @DeleteMapping("/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStudent(
        @PathVariable
        UUID studentId
    ) {
        studentService.deleteStudent(studentId);
    }

    @PostMapping("/{studentId}/restore")
    public StudentSummary restoreStudent(
        @PathVariable
        UUID studentId,
        @RequestBody(required = false)
        RestoreStudentRequest request
    ) {
        return studentService.restoreStudent(studentId, request);
    }

    @DeleteMapping("/{studentId}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStudentPermanently(
        @PathVariable
        UUID studentId
    ) {
        studentService.deleteStudentPermanently(studentId);
    }
}
