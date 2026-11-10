package com.sr.scoreimport.dto.response;

import java.time.LocalDate;
import java.util.List;

/*
 * A student in the file that is not on the system yet. It is added only when the
 * user ticks it, problem is not null when it cannot be added
 */
public record ImportNewStudentResponse(
    String studentCode,
    String fullName,
    LocalDate startedAt,
    String classCode,
    String className,
    int newSessions,
    int starsToAward,
    List<ImportSessionResponse> sessions,
    String problem
) {}
