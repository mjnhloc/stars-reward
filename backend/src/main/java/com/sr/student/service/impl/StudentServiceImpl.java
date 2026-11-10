package com.sr.student.service.impl;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.sr.auth.service.AuthorizationService;
import com.sr.classes.entity.ClassEntity;
import com.sr.classes.repository.ClassRepository;
import com.sr.common.audit.dto.AuditLogCommand;
import com.sr.common.audit.service.AuditLogService;
import com.sr.reward.repository.RewardRedemptionRepository;
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
import com.sr.student.entity.ClassEnrollmentEntity;
import com.sr.student.entity.StarTransactionEntity;
import com.sr.student.entity.StudentEntity;
import com.sr.student.entity.StudentStarBalanceEntity;
import com.sr.student.repository.ClassEnrollmentRepository;
import com.sr.student.repository.StarTransactionRepository;
import com.sr.student.repository.StudentRepository;
import com.sr.student.repository.StudentScoreRepository;
import com.sr.student.repository.StudentStarBalanceRepository;
import com.sr.student.service.StudentService;
import com.sr.user.service.UserService;

@Service
public class StudentServiceImpl implements StudentService {

    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final StudentStarBalanceRepository studentStarBalanceRepository;
    private final StarTransactionRepository starTransactionRepository;
    private final StudentScoreRepository studentScoreRepository;
    private final RewardRedemptionRepository rewardRedemptionRepository;
    private final AuthorizationService auth;
    private final RewardCalculationService rewardCalculationService;
    private final AuditLogService auditLogService;
    private final UserService userService;

    public StudentServiceImpl(
        StudentRepository studentRepository,
        ClassRepository classRepository,
        ClassEnrollmentRepository classEnrollmentRepository,
        StudentStarBalanceRepository studentStarBalanceRepository,
        StarTransactionRepository starTransactionRepository,
        StudentScoreRepository studentScoreRepository,
        RewardRedemptionRepository rewardRedemptionRepository,
        AuthorizationService auth,
        RewardCalculationService rcs,
        AuditLogService auditLogService,
        UserService userService
    ) {
        this.studentRepository = studentRepository;
        this.classRepository = classRepository;
        this.classEnrollmentRepository = classEnrollmentRepository;
        this.studentStarBalanceRepository = studentStarBalanceRepository;
        this.starTransactionRepository = starTransactionRepository;
        this.studentScoreRepository = studentScoreRepository;
        this.rewardRedemptionRepository = rewardRedemptionRepository;
        this.auth = auth;
        this.rewardCalculationService = rcs;
        this.auditLogService = auditLogService;
        this.userService = userService;
    }

    /*
     * Function search after name or student code for every character typed
     */
    @Override
    @Transactional(readOnly = true)
    public List<StudentResponse> getStudents(UUID classId, String query) {
        String searchQuery = "%" + (query == null ? "" : query.trim()) + "%";

        List<StudentSummary> sm = classId == null
            ? studentRepository.searchActiveStudents(searchQuery, Limit.of(500))
            : studentRepository.searchActiveStudentsInClass(classId, searchQuery, Limit.of(500));

        return sm.stream().map(student -> {
            int totalStars = student.totalStars();

            return new StudentResponse(
                student.id(),
                student.studentCode(),
                student.fullName(),
                student.classId(),
                student.className(),
                totalStars,
                rewardCalculationService.calculateRedeemableStars(totalStars),
                rewardCalculationService.calculateRedeemableValue(totalStars),
                rewardCalculationService.calculateRemainingStars(totalStars),
                student.startedAt()
            );
        }).toList();
    }

    /*
     * Function get student transaction history
     */
    @Override
    @Transactional(readOnly = true)
    public List<StudentTransactionResponse> getStudentTransactions(UUID studentId) {
        return starTransactionRepository.findHistory(studentId, Limit.of(200));
    }

    /*
     * Function to adjust student stars (add or substract)
     */
    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public StudentStarsAdjustmentResponse adjustStudentStars(
        UUID actorId,
        UUID studentId,
        StudentStarsAdjustmentRequest request,
        String idempotencyKey
    ) {
        if (request.stars() == 0) {
            throw new IllegalArgumentException("Số sao phải khác 0");
        }

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Thiếu Idempotency-Key");
        }

        /*
         * If key's already handled before, return the old one. This will prevent user
         * from double-clicking or frontend retry
         */
        StudentStarsAdjustmentResponse existingResult = findExistingAdjustment(idempotencyKey, studentId);
        if (existingResult != null) {
            return existingResult;
        }

        /*
         * Check if user have right to modify and if student is enroll in the right
         * class
         */
        auth.requireAdjust(actorId, request.classId());

        boolean enrolled = classEnrollmentRepository.existsByStudentIdAndClassIdAndEndedAtIsNull(
            studentId,
            request.classId()
        );

        if (!enrolled) {
            throw new IllegalArgumentException("Học sinh không thuộc lớp đã chọn");
        }

        /*
         * Lock row balance until transaction finish
         */
        StudentStarBalanceEntity balance = studentStarBalanceRepository.findForUpdate(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy số dư học sinh"));

        int balanceBefore = balance.getTotalStars();

        int balanceAfter = Math.addExact(balanceBefore, request.stars());
        if (balanceAfter < 0) {
            throw new IllegalArgumentException("Không thể trừ vì tổng ngôi sao sẽ nhỏ hơn 0");
        }

        String transactionType = request.stars() > 0 ? "EARN" : "DEDUCT";

        /*
         * insert new star transaction
         */
        StarTransactionEntity transaction = starTransactionRepository.save(
            StarTransactionEntity.builder()
                .studentId(studentId)
                .classId(request.classId())
                .transactionType(transactionType)
                .sourceType("MANUAL")
                .starDelta(request.stars())
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .reasonDetail(request.reasonDetail())
                .performedBy(actorId)
                .idempotencyKey(idempotencyKey)
                .build()
        );

        UUID transactionId = transaction.getId();

        /*
         * update student star balance (version and updated_at are handled by
         * Hibernate)
         */
        balance.setTotalStars(balanceAfter);

        /*
         * insert adjustment into audit logs
         */
        auditLogService.record(
            new AuditLogCommand(
                actorId,
                request.stars() > 0 ? "STARS_ADDED" : "STARS_DEDUCTED",
                "STAR_TRANSACTION",
                transactionId,
                Map.of(
                    "studentId",
                    studentId,
                    "classId",
                    request.classId(),
                    "transactionType",
                    transactionType,
                    "starDelta",
                    request.stars(),
                    "balanceBefore",
                    balanceBefore,
                    "balanceAfter",
                    balanceAfter,
                    "reasonDetail",
                    request.reasonDetail()
                )
            )
        );

        return new StudentStarsAdjustmentResponse(transactionId, studentId, request.stars(), balanceAfter);
    }

    /*
     * Support function to find if there is any existing adjustment with the same
     * idempotency-key
     */
    private StudentStarsAdjustmentResponse findExistingAdjustment(
        String idempotencyKey,
        UUID studentId
    ) {
        return starTransactionRepository.findByIdempotencyKey(idempotencyKey)
            .map(transaction -> {
                if (!studentId.equals(transaction.getStudentId())) {
                    throw new IllegalArgumentException(
                        "Idempotency-Key đã được sử dụng "
                            + "cho một yêu cầu khác"
                    );
                }

                return new StudentStarsAdjustmentResponse(
                    transaction.getId(),
                    transaction.getStudentId(),
                    transaction.getStarDelta(),
                    transaction.getBalanceAfter()
                );
            })
            .orElse(null);
    }

    /*
     * Function to get all the scores detail for a student
     */
    @Override
    @Transactional(readOnly = true)
    public List<StudentScoreResponse> getStudentScores(
        UUID studentId
    ) {
        return studentScoreRepository.findScores(studentId);
    }

    /*
     * Function to create a new student
     */

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public StudentSummary createStudent(CreateStudentRequest request) {
        String normalizedStudentCode = request.studentCode().trim().toUpperCase();

        String normalizedFullName = request.fullName().trim();

        LocalDate startedAt = request.startedAt() != null ? request.startedAt() : LocalDate.now();

        UUID actorId = userService.getUserId();

        String className = classRepository.findByIdAndArchivedAtIsNull(request.classId())
            .map(ClassEntity::getName)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học"));

        if (studentRepository.existsByStudentCode(normalizedStudentCode)) {
            throw new IllegalArgumentException("Mã học sinh đã tồn tại");
        }

        StudentEntity student = StudentEntity.create(normalizedStudentCode, normalizedFullName);

        /*
         * flush right away so that a duplicate student code is caught here
         */
        try {
            studentRepository.saveAndFlush(student);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException(
                "Mã học sinh đã tồn tại"
            );
        }

        UUID studentId = student.getId();

        ClassEnrollmentEntity enrollment = classEnrollmentRepository.save(
            ClassEnrollmentEntity.create(request.classId(), studentId, startedAt)
        );

        studentStarBalanceRepository.save(StudentStarBalanceEntity.create(studentId));

        auditLogService.record(
            new AuditLogCommand(
                actorId,
                "STUDENT_CREATED",
                "STUDENT",
                studentId,
                Map.of(
                    "studentCode",
                    normalizedStudentCode,
                    "fullName",
                    normalizedFullName,
                    "classId",
                    request.classId().toString(),
                    "className",
                    className,
                    "enrollmentId",
                    enrollment.getId().toString(),
                    "startedAt",
                    startedAt.toString()
                )
            )
        );

        return new StudentSummary(
            studentId,
            normalizedStudentCode,
            normalizedFullName,
            request.classId(),
            className,
            0,
            startedAt
        );
    }

    /*
     * Function to update student code, name and move student to another class.
     * Stars balance and transaction history stay the same
     */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public StudentSummary updateStudent(UUID studentId, UpdateStudentRequest request) {
        String normalizedStudentCode = request.studentCode().trim().toUpperCase();

        String normalizedFullName = request.fullName().trim();

        UUID actorId = userService.getUserId();

        StudentEntity student = studentRepository.findActiveForUpdate(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy học sinh"));

        String className = classRepository.findByIdAndArchivedAtIsNull(request.classId())
            .map(ClassEntity::getName)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học"));

        if (studentRepository.existsByStudentCodeAndIdNot(normalizedStudentCode, studentId)) {
            throw new IllegalArgumentException("Mã học sinh đã tồn tại");
        }

        ClassEnrollmentEntity currentEnrollment = classEnrollmentRepository.findByStudentIdAndEndedAtIsNull(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Học sinh hiện không thuộc lớp nào"));

        UUID fromClassId = currentEnrollment.getClassId();
        LocalDate fromStartedAt = currentEnrollment.getStartedAt();
        LocalDate startedAt = request.startedAt();

        if (startedAt.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày bắt đầu học không được sau hôm nay");
        }

        Map<String, Object> details = new HashMap<>();
        details.put(
            "before",
            Map.of(
                "studentCode",
                student.getStudentCode(),
                "fullName",
                student.getFullName(),
                "classId",
                fromClassId.toString(),
                "startedAt",
                fromStartedAt.toString()
            )
        );
        details.put(
            "after",
            Map.of(
                "studentCode",
                normalizedStudentCode,
                "fullName",
                normalizedFullName,
                "classId",
                request.classId().toString(),
                "startedAt",
                startedAt.toString()
            )
        );

        student.setStudentCode(normalizedStudentCode);
        student.setFullName(normalizedFullName);

        try {
            studentRepository.saveAndFlush(student);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("Mã học sinh đã tồn tại");
        }

        boolean classChanged = !fromClassId.equals(request.classId());

        if (classChanged && startedAt.isAfter(fromStartedAt)) {
            /*
             * Move student to another class from the chosen date. End the current
             * enrollment and flush before inserting the new one, so that the unique
             * index on active enrollment is never violated
             */
            currentEnrollment.setEndedAt(startedAt);
            classEnrollmentRepository.saveAndFlush(currentEnrollment);

            ClassEnrollmentEntity newEnrollment = ClassEnrollmentEntity.create(request.classId(), studentId, startedAt);
            saveEnrollment(newEnrollment);

            details.put("newEnrollmentId", newEnrollment.getId().toString());
        } else if (classChanged || !startedAt.equals(fromStartedAt)) {
            /*
             * Correct the current enrollment (wrong class from its start, or wrong start
             * date), no history needed. It cannot start before the previous one ended
             */
            classEnrollmentRepository.findFirstByStudentIdAndEndedAtIsNotNullOrderByEndedAtDesc(studentId)
                .filter(previous -> startedAt.isBefore(previous.getEndedAt()))
                .ifPresent(previous -> {
                    throw new IllegalArgumentException(
                        "Ngày bắt đầu học không được trước ngày học sinh rời lớp trước (%s)"
                            .formatted(DISPLAY_DATE.format(previous.getEndedAt()))
                    );
                });

            currentEnrollment.setClassId(request.classId());
            currentEnrollment.setStartedAt(startedAt);
            saveEnrollment(currentEnrollment);
        }

        if (classChanged) {
            details.put("fromClassId", fromClassId.toString());
            details.put("toClassId", request.classId().toString());
        }

        auditLogService.record(
            new AuditLogCommand(
                actorId,
                "STUDENT_UPDATED",
                "STUDENT",
                studentId,
                details
            )
        );

        int totalStars = studentStarBalanceRepository.findById(studentId)
            .map(StudentStarBalanceEntity::getTotalStars)
            .orElse(0);

        return new StudentSummary(
            studentId,
            normalizedStudentCode,
            normalizedFullName,
            request.classId(),
            className,
            totalStars,
            startedAt
        );
    }

    /*
     * Function to soft delete a student. Ledger, scores and redemptions are kept
     */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteStudent(UUID studentId) {
        UUID actorId = userService.getUserId();

        StudentEntity student = studentRepository.findActiveForUpdate(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy học sinh"));

        student.setArchivedAt(OffsetDateTime.now());

        Map<String, Object> details = new HashMap<>();
        details.put("studentCode", student.getStudentCode());
        details.put("fullName", student.getFullName());

        /*
         * End the active enrollment, so that student cannot receive or redeem stars
         * anymore
         */
        classEnrollmentRepository.findByStudentIdAndEndedAtIsNull(studentId)
            .ifPresent(enrollment -> {
                enrollment.setEndedAt(LocalDate.now());
                details.put("classId", enrollment.getClassId().toString());
            });

        details.put(
            "totalStars",
            studentStarBalanceRepository.findById(studentId)
                .map(StudentStarBalanceEntity::getTotalStars)
                .orElse(0)
        );

        auditLogService.record(
            new AuditLogCommand(
                actorId,
                "STUDENT_ARCHIVED",
                "STUDENT",
                studentId,
                details
            )
        );
    }

    /*
     * Function to list deleted students (the trash), newest first
     */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<StudentTrashResponse> getStudentTrash() {
        return studentRepository.findTrash();
    }

    /*
     * Function to restore a deleted student with the stars it had. The student goes
     * back to its old class, or to the chosen class when the old one was deleted
     */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public StudentSummary restoreStudent(UUID studentId, RestoreStudentRequest request) {
        UUID actorId = userService.getUserId();

        StudentEntity student = studentRepository.findArchivedForUpdate(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy học sinh trong thùng rác"));

        ClassEnrollmentEntity lastEnrollment = classEnrollmentRepository.findFirstByStudentIdOrderByStartedAtDesc(studentId)
            .orElse(null);

        UUID lastClassId = lastEnrollment == null ? null : lastEnrollment.getClassId();
        UUID classId = request != null && request.classId() != null ? request.classId() : lastClassId;

        if (classId == null) {
            throw new IllegalArgumentException("Hãy chọn lớp để khôi phục học sinh");
        }

        ClassEntity classEntity = classRepository.findByIdAndArchivedAtIsNull(classId)
            .orElseThrow(() -> new IllegalArgumentException(
                classId.equals(lastClassId)
                    ? "Lớp cũ của học sinh đã bị xóa, hãy chọn lớp khác hoặc khôi phục lớp trước"
                    : "Không tìm thấy lớp học"
            ));

        LocalDate startedAt;

        if (classId.equals(lastClassId)) {
            /*
             * Back to the old class: continue the old enrollment
             */
            lastEnrollment.setEndedAt(null);
            startedAt = lastEnrollment.getStartedAt();
        } else {
            startedAt = LocalDate.now();
            saveEnrollment(ClassEnrollmentEntity.create(classId, studentId, startedAt));
        }

        student.setArchivedAt(null);

        int totalStars = studentStarBalanceRepository.findById(studentId)
            .map(StudentStarBalanceEntity::getTotalStars)
            .orElse(0);

        auditLogService.record(
            new AuditLogCommand(
                actorId,
                "STUDENT_RESTORED",
                "STUDENT",
                studentId,
                Map.of(
                    "studentCode",
                    student.getStudentCode(),
                    "classId",
                    classId.toString(),
                    "totalStars",
                    totalStars
                )
            )
        );

        return new StudentSummary(
            studentId,
            student.getStudentCode(),
            student.getFullName(),
            classId,
            classEntity.getName(),
            totalStars,
            startedAt
        );
    }

    /*
     * Function to permanently delete a student in the trash with all of its
     * history: star transactions, redemptions, scores, balance and enrollments
     */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteStudentPermanently(UUID studentId) {
        UUID actorId = userService.getUserId();

        StudentEntity student = studentRepository.findArchivedForUpdate(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy học sinh trong thùng rác"));

        Map<String, Object> details = new HashMap<>();
        details.put("studentCode", student.getStudentCode());
        details.put("fullName", student.getFullName());

        /*
         * Order matters because of the foreign keys: transactions point to
         * redemptions and scores
         */
        details.put("transactions", starTransactionRepository.deleteAllOfStudent(studentId));
        details.put("redemptions", rewardRedemptionRepository.deleteAllOfStudent(studentId));
        details.put("scores", studentScoreRepository.deleteAllOfStudent(studentId));
        studentStarBalanceRepository.deleteOfStudent(studentId);
        classEnrollmentRepository.deleteAllOfStudent(studentId);
        studentRepository.deletePermanently(studentId);

        auditLogService.record(
            new AuditLogCommand(
                actorId,
                "STUDENT_DELETED_PERMANENTLY",
                "STUDENT",
                studentId,
                details
            )
        );
    }

    /*
     * Save an enrollment right away, so that a second enrollment of the student in
     * the same class with the same start date is reported clearly
     */
    private void saveEnrollment(ClassEnrollmentEntity enrollment) {
        try {
            classEnrollmentRepository.saveAndFlush(enrollment);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("Học sinh đã có một lần vào lớp này với cùng ngày bắt đầu");
        }
    }
}
