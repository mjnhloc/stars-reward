package com.sr.student.service;

import java.util.List;
import java.util.UUID;

import com.sr.student.dto.request.CreateStudentRequest;
import com.sr.student.dto.request.StudentStarsAdjustmentRequest;
import com.sr.student.dto.response.StudentResponse;
import com.sr.student.dto.response.StudentScoreResponse;
import com.sr.student.dto.response.StudentStarsAdjustmentResponse;
import com.sr.student.dto.response.StudentSummary;
import com.sr.student.dto.response.StudentTransactionResponse;

public interface StudentService {

    List<StudentResponse> getStudents(UUID classId, String search);

    List<StudentTransactionResponse> getStudentTransactions(UUID studentId);

    StudentStarsAdjustmentResponse adjustStudentStars(
        UUID actor,
        UUID student,
        StudentStarsAdjustmentRequest r,
        String key
    );

    List<StudentScoreResponse> getStudentScores(UUID studentId);

    StudentSummary createStudent(CreateStudentRequest request);
}
