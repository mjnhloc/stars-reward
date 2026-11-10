package com.sr.student.dto.request;

import java.util.UUID;

/*
 * classId is only needed when the old class of the student was deleted
 */
public record RestoreStudentRequest(
    UUID classId
) {}
