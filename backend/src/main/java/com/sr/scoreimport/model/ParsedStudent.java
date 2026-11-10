package com.sr.scoreimport.model;

import java.time.LocalDate;
import java.util.List;

public record ParsedStudent(
    int rowNumber,
    String studentCode,
    String fullName,
    /*
     * from the "Ngày bắt đầu học" column, only needed to add a new student
     */
    LocalDate startedAt,
    List<ParsedSession> sessions
) {}
