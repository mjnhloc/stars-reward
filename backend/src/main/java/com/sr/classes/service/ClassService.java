package com.sr.classes.service;

import java.util.List;
import java.util.UUID;

import com.sr.classes.dto.request.CreateClassRequest;
import com.sr.classes.dto.request.UpdateClassRequest;
import com.sr.classes.dto.response.ClassDetailResponse;
import com.sr.classes.dto.response.ClassResponse;
import com.sr.classes.dto.response.ClassTrashResponse;
import com.sr.classes.dto.response.CreateClassResponse;

public interface ClassService {

    List<ClassResponse> getClasses();

    ClassDetailResponse getClassDetail(UUID classId);

    CreateClassResponse createClass(CreateClassRequest request);

    ClassDetailResponse updateClass(UUID classId, UpdateClassRequest request);

    void deleteClass(UUID classId);

    List<ClassTrashResponse> getClassTrash();

    ClassDetailResponse restoreClass(UUID classId);

    void deleteClassPermanently(UUID classId);
}
