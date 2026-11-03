package com.sr.classes.service.impl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sr.classes.dto.request.CreateClassRequest;
import com.sr.classes.dto.request.UpdateClassRequest;
import com.sr.classes.dto.response.ClassDetailResponse;
import com.sr.classes.dto.response.ClassResponse;
import com.sr.classes.dto.response.CreateClassResponse;
import com.sr.classes.entity.ClassEntity;
import com.sr.classes.repository.ClassRepository;
import com.sr.classes.service.ClassService;
import com.sr.common.audit.dto.AuditLogCommand;
import com.sr.common.audit.service.AuditLogService;
import com.sr.student.repository.ClassEnrollmentRepository;
import com.sr.user.service.UserService;

@Service
public class ClassServiceImpl implements ClassService {

    private final ClassRepository classRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final UserService userService;
    private final AuditLogService auditLogService;

    public ClassServiceImpl(
        ClassRepository classRepository,
        ClassEnrollmentRepository classEnrollmentRepository,
        UserService userService,
        AuditLogService auditLogService
    ) {
        this.classRepository = classRepository;
        this.classEnrollmentRepository = classEnrollmentRepository;
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassResponse> getClasses() {
        return classRepository.findActiveClassSummaries();
    }

    /*
     * get detail of an active class
     */
    @Override
    @Transactional(readOnly = true)
    public ClassDetailResponse getClassDetail(UUID classId) {
        return classRepository.findByIdAndArchivedAtIsNull(classId)
            .map(this::toDetailResponse)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học"));
    }

    /*
     * create a new class
     */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public CreateClassResponse createClass(CreateClassRequest request) {
        String normalizedCode = request.code().trim().toUpperCase();
        String normalizedName = request.name().trim();

        UUID actorId = userService.getUserId();

        if (classRepository.existsByCode(normalizedCode)) {
            throw new IllegalArgumentException("Mã lớp đã tồn tại");
        }

        ClassEntity classEntity = ClassEntity.create(normalizedCode, normalizedName, request.schoolYear());

        /*
         * flush right away so that a duplicate code (2 requests at the same time) is
         * caught here instead of at commit
         */
        try {
            classRepository.saveAndFlush(classEntity);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("Mã lớp đã tồn tại");
        }

        UUID classId = classEntity.getId();

        /*
         * write to audit log
         */
        auditLogService.record(
            new AuditLogCommand(
                actorId,
                "CLASS_CREATED",
                "CLASS",
                classId,
                Map.of(
                    "code",
                    normalizedCode,
                    "name",
                    normalizedName,
                    "schoolYear",
                    request.schoolYear()
                )
            )
        );

        return new CreateClassResponse(
            classId,
            normalizedCode,
            normalizedName,
            request.schoolYear()
        );
    }

    /*
     * update code, name and school year of a class
     */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ClassDetailResponse updateClass(UUID classId, UpdateClassRequest request) {
        String normalizedCode = request.code().trim().toUpperCase();
        String normalizedName = request.name().trim();

        UUID actorId = userService.getUserId();

        ClassEntity classEntity = classRepository.findActiveForUpdate(classId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học"));

        if (classRepository.existsByCodeAndIdNot(normalizedCode, classId)) {
            throw new IllegalArgumentException("Mã lớp đã tồn tại");
        }

        Map<String, Object> before = Map.of(
            "code",
            classEntity.getCode(),
            "name",
            classEntity.getName(),
            "schoolYear",
            classEntity.getSchoolYear()
        );

        classEntity.setCode(normalizedCode);
        classEntity.setName(normalizedName);
        classEntity.setSchoolYear(request.schoolYear());

        try {
            classRepository.saveAndFlush(classEntity);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("Mã lớp đã tồn tại");
        }

        auditLogService.record(
            new AuditLogCommand(
                actorId,
                "CLASS_UPDATED",
                "CLASS",
                classId,
                Map.of(
                    "before",
                    before,
                    "after",
                    Map.of(
                        "code",
                        normalizedCode,
                        "name",
                        normalizedName,
                        "schoolYear",
                        request.schoolYear()
                    )
                )
            )
        );

        return toDetailResponse(classEntity);
    }

    /*
     * soft delete a class, only allowed when no student is studying in it
     */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteClass(UUID classId) {
        UUID actorId = userService.getUserId();

        ClassEntity classEntity = classRepository.findActiveForUpdate(classId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học"));

        long activeStudents = classEnrollmentRepository.countActiveStudents(classId);
        if (activeStudents > 0) {
            throw new IllegalArgumentException(
                "Lớp vẫn còn " + activeStudents
                    + " học sinh, hãy chuyển hoặc xóa học sinh trước khi xóa lớp"
            );
        }

        classEntity.setArchivedAt(OffsetDateTime.now());

        auditLogService.record(
            new AuditLogCommand(
                actorId,
                "CLASS_ARCHIVED",
                "CLASS",
                classId,
                Map.of(
                    "code",
                    classEntity.getCode(),
                    "name",
                    classEntity.getName()
                )
            )
        );
    }

    private ClassDetailResponse toDetailResponse(ClassEntity classEntity) {
        return new ClassDetailResponse(
            classEntity.getId(),
            classEntity.getCode(),
            classEntity.getName(),
            classEntity.getSchoolYear()
        );
    }
}
