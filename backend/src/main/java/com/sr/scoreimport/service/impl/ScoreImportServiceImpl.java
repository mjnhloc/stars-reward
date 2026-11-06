package com.sr.scoreimport.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.sr.classes.entity.ClassEntity;
import com.sr.classes.repository.ClassRepository;
import com.sr.common.audit.dto.AuditLogCommand;
import com.sr.common.audit.service.AuditLogService;
import com.sr.scoreimport.dto.response.ImportBatchClass;
import com.sr.scoreimport.dto.response.ImportBatchResponse;
import com.sr.scoreimport.dto.response.ImportBatchRow;
import com.sr.scoreimport.dto.response.ImportBatchSummary;
import com.sr.scoreimport.dto.response.ImportClassResponse;
import com.sr.scoreimport.dto.response.ImportIssueResponse;
import com.sr.scoreimport.dto.response.ImportRevertPreviewResponse;
import com.sr.scoreimport.dto.response.ImportRevertStudentResponse;
import com.sr.scoreimport.dto.response.ImportSessionResponse;
import com.sr.scoreimport.dto.response.ImportStudentResponse;
import com.sr.scoreimport.dto.response.ImportTotalsResponse;
import com.sr.scoreimport.dto.response.ScoreImportPreviewResponse;
import com.sr.scoreimport.dto.response.ScoreImportResultResponse;
import com.sr.scoreimport.dto.response.ScoreImportRevertResponse;
import com.sr.scoreimport.entity.ImportBatchEntity;
import com.sr.scoreimport.model.ParsedClassBlock;
import com.sr.scoreimport.model.ParsedSession;
import com.sr.scoreimport.model.ParsedSheet;
import com.sr.scoreimport.model.ParsedStudent;
import com.sr.scoreimport.repository.ImportBatchRepository;
import com.sr.scoreimport.service.ScoreCalculator;
import com.sr.scoreimport.service.ScoreImportService;
import com.sr.scoreimport.service.ScoreSheetParser;
import com.sr.student.entity.ClassEnrollmentEntity;
import com.sr.student.entity.StarTransactionEntity;
import com.sr.student.entity.StudentEntity;
import com.sr.student.entity.StudentScoreEntity;
import com.sr.student.entity.StudentStarBalanceEntity;
import com.sr.student.repository.ClassEnrollmentRepository;
import com.sr.student.repository.StarTransactionRepository;
import com.sr.student.repository.StudentRepository;
import com.sr.student.repository.StudentScoreRepository;
import com.sr.student.repository.StudentStarBalanceRepository;
import com.sr.user.service.UserService;

@Service
public class ScoreImportServiceImpl implements ScoreImportService {

    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int MAX_FILE_NAME_LENGTH = 250;
    private static final int MAX_IMPORT_HISTORY = 100;

    private final ScoreSheetParser scoreSheetParser;
    private final ScoreCalculator scoreCalculator;
    private final ClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final StudentScoreRepository studentScoreRepository;
    private final StudentStarBalanceRepository studentStarBalanceRepository;
    private final StarTransactionRepository starTransactionRepository;
    private final ImportBatchRepository importBatchRepository;
    private final AuditLogService auditLogService;
    private final UserService userService;

    public ScoreImportServiceImpl(
        ScoreSheetParser scoreSheetParser,
        ScoreCalculator scoreCalculator,
        ClassRepository classRepository,
        StudentRepository studentRepository,
        ClassEnrollmentRepository classEnrollmentRepository,
        StudentScoreRepository studentScoreRepository,
        StudentStarBalanceRepository studentStarBalanceRepository,
        StarTransactionRepository starTransactionRepository,
        ImportBatchRepository importBatchRepository,
        AuditLogService auditLogService,
        UserService userService
    ) {
        this.scoreSheetParser = scoreSheetParser;
        this.scoreCalculator = scoreCalculator;
        this.classRepository = classRepository;
        this.studentRepository = studentRepository;
        this.classEnrollmentRepository = classEnrollmentRepository;
        this.studentScoreRepository = studentScoreRepository;
        this.studentStarBalanceRepository = studentStarBalanceRepository;
        this.starTransactionRepository = starTransactionRepository;
        this.importBatchRepository = importBatchRepository;
        this.auditLogService = auditLogService;
        this.userService = userService;
    }

    /*
     * A new score that will be inserted
     */
    private record PlannedScore(
        UUID studentId,
        UUID classId,
        String studentCode,
        LocalDate assessmentDate,
        BigDecimal homeworkScore,
        BigDecimal testScore,
        BigDecimal finalScore,
        int awardedStars
    ) {}

    private record Analysis(
        String fileName,
        ScoreImportPreviewResponse preview,
        List<PlannedScore> plannedScores
    ) {}

    /*
     * What a revert takes back: active scores and their star transactions
     */
    private record RevertPlan(
        List<StudentScoreEntity> scores,
        Map<UUID, List<StarTransactionEntity>> earnsByStudent,
        Map<UUID, Integer> starsByStudent,
        int totalStars
    ) {}

    /*
     * Function to read the file and show what will be imported, nothing is saved
     */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ScoreImportPreviewResponse preview(MultipartFile file) {
        return analyse(file).preview();
    }

    /*
     * Function to import new scores and award stars. The file is analysed again
     * so that the result is always the same as the preview at this moment
     */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ScoreImportResultResponse importScores(MultipartFile file) {
        UUID actorId = userService.getUserId();

        Analysis analysis = analyse(file);
        List<PlannedScore> plannedScores = analysis.plannedScores();
        ImportTotalsResponse totals = analysis.preview().totals();

        if (plannedScores.isEmpty()) {
            throw new IllegalArgumentException("Không có buổi điểm mới nào để nhập");
        }

        ImportBatchEntity batch = importBatchRepository.save(ImportBatchEntity.start(analysis.fileName(), actorId));

        UUID batchId = batch.getId();

        List<StudentScoreEntity> scores = plannedScores.stream()
            .map(planned -> StudentScoreEntity.create(
                planned.studentId(),
                planned.classId(),
                planned.homeworkScore(),
                planned.testScore(),
                planned.finalScore(),
                planned.awardedStars(),
                planned.assessmentDate(),
                batchId,
                planned.studentCode() + "|" + planned.assessmentDate()
            ))
            .toList();

        /*
         * flush right away so that a session imported at the same time by another
         * request is caught here (unique student and assessment date)
         */
        try {
            studentScoreRepository.saveAllAndFlush(scores);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException(
                "Một số buổi vừa được nhập bởi yêu cầu khác. Hãy chọn lại file để xem trước"
            );
        }

        Map<UUID, List<StudentScoreEntity>> scoresByStudent = new LinkedHashMap<>();

        for (StudentScoreEntity score : scores) {
            if (score.getAwardedStars() > 0) {
                scoresByStudent.computeIfAbsent(score.getStudentId(), key -> new ArrayList<>()).add(score);
            }
        }

        int awardedStars = 0;

        for (Map.Entry<UUID, List<StudentScoreEntity>> entry : scoresByStudent.entrySet()) {
            /*
             * Lock row balance until transaction finish
             */
            StudentStarBalanceEntity balance = studentStarBalanceRepository.findForUpdate(entry.getKey())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy số dư học sinh"));

            int currentBalance = balance.getTotalStars();

            /*
             * one ledger row for every session that has stars
             */
            for (StudentScoreEntity score : entry.getValue()) {
                int balanceAfter = Math.addExact(currentBalance, score.getAwardedStars());

                starTransactionRepository.save(
                    StarTransactionEntity.builder()
                        .studentId(score.getStudentId())
                        .classId(score.getClassId())
                        .transactionType("EARN")
                        .sourceType("SCORE")
                        .starDelta(score.getAwardedStars())
                        .balanceBefore(currentBalance)
                        .balanceAfter(balanceAfter)
                        .reasonDetail(scoreReason(score))
                        .scoreId(score.getId())
                        .performedBy(actorId)
                        .idempotencyKey("score:" + score.getId())
                        .build()
                );

                currentBalance = balanceAfter;
                awardedStars += score.getAwardedStars();
            }

            balance.setTotalStars(currentBalance);
        }

        batch.setRowCount(plannedScores.size());
        batch.setErrorCount(totals.errors());
        batch.setStatus("COMPLETED");
        batch.setCompletedAt(OffsetDateTime.now());

        int studentCount = (int) plannedScores.stream().map(PlannedScore::studentId).distinct().count();

        auditLogService.record(
            new AuditLogCommand(
                actorId,
                "SCORES_IMPORTED",
                "IMPORT_BATCH",
                batchId,
                Map.of(
                    "fileName",
                    analysis.fileName(),
                    "sheetName",
                    analysis.preview().sheetName(),
                    "importedSessions",
                    plannedScores.size(),
                    "skippedSessions",
                    totals.skippedSessions(),
                    "awardedStars",
                    awardedStars,
                    "students",
                    studentCount,
                    "errors",
                    totals.errors()
                )
            )
        );

        return new ScoreImportResultResponse(
            batchId,
            plannedScores.size(),
            awardedStars,
            studentCount,
            totals.skippedSessions()
        );
    }

    /*
     * Function to list finished imports, newest first
     */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<ImportBatchResponse> getImports() {
        List<ImportBatchRow> rows = importBatchRepository.findHistory(Limit.of(MAX_IMPORT_HISTORY));

        if (rows.isEmpty()) {
            return List.of();
        }

        List<UUID> batchIds = rows.stream().map(ImportBatchRow::id).toList();

        Map<UUID, ImportBatchSummary> summaries = studentScoreRepository.summarizeBatches(batchIds)
            .stream()
            .collect(Collectors.toMap(ImportBatchSummary::batchId, Function.identity()));

        Map<UUID, List<String>> classNames = studentScoreRepository.findBatchClasses(batchIds)
            .stream()
            .collect(
                Collectors.groupingBy(
                    ImportBatchClass::batchId,
                    Collectors.mapping(ImportBatchClass::className, Collectors.toList())
                )
            );

        return rows.stream().map(row -> {
            ImportBatchSummary summary = summaries.get(row.id());

            return new ImportBatchResponse(
                row.id(),
                row.fileName(),
                row.status(),
                row.importedAt(),
                row.importedBy(),
                summary == null ? 0 : summary.sessionCount(),
                summary == null ? 0 : summary.studentCount(),
                summary == null ? 0 : summary.awardedStars(),
                classNames.getOrDefault(row.id(), List.of()).stream().sorted().toList(),
                row.revertedAt(),
                row.revertedBy()
            );
        }).toList();
    }

    /*
     * Function to show which students lose how many stars when an import is
     * reverted, nothing is saved
     */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ImportRevertPreviewResponse getRevertPreview(UUID batchId) {
        ImportBatchEntity batch = importBatchRepository.findById(batchId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lần nhập điểm"));

        requireRevertable(batch);

        RevertPlan plan = planRevert(batchId);

        Map<UUID, Integer> balances = plan.starsByStudent().isEmpty()
            ? Map.of()
            : studentStarBalanceRepository.findAllById(plan.starsByStudent().keySet())
                .stream()
                .collect(Collectors.toMap(StudentStarBalanceEntity::getStudentId, StudentStarBalanceEntity::getTotalStars));

        List<ImportRevertStudentResponse> students = revertStudents(plan, balances);

        return new ImportRevertPreviewResponse(
            batchId,
            batch.getSourceSheetId(),
            plan.scores().size(),
            plan.totalStars(),
            students,
            students.stream().allMatch(ImportRevertStudentResponse::enoughBalance)
        );
    }

    /*
     * Function to revert an import: every star it awarded is taken back with a
     * reversal transaction and its scores are voided, so that the file can be
     * imported again. Nothing is reverted when a student does not have enough
     * stars anymore
     */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ScoreImportRevertResponse revertImport(UUID batchId) {
        UUID actorId = userService.getUserId();

        /*
         * Lock batch so that it cannot be reverted twice at the same time
         */
        ImportBatchEntity batch = importBatchRepository.findForUpdate(batchId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lần nhập điểm"));

        requireRevertable(batch);

        RevertPlan plan = planRevert(batchId);

        /*
         * Lock balances in a fixed order (student id) to avoid deadlocks
         */
        Map<UUID, StudentStarBalanceEntity> balances = new LinkedHashMap<>();

        for (UUID studentId : plan.starsByStudent().keySet()) {
            balances.put(
                studentId,
                studentStarBalanceRepository.findForUpdate(studentId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy số dư học sinh"))
            );
        }

        /*
         * Check every student before writing anything
         */
        Map<UUID, Integer> currentBalances = balances.values()
            .stream()
            .collect(Collectors.toMap(StudentStarBalanceEntity::getStudentId, StudentStarBalanceEntity::getTotalStars));

        List<String> shortages = revertStudents(plan, currentBalances).stream()
            .filter(student -> !student.enoughBalance())
            .map(student -> "%s (còn %d/%d sao)".formatted(
                student.studentCode(),
                student.currentBalance(),
                student.starsToReverse()
            ))
            .toList();

        if (!shortages.isEmpty()) {
            throw new IllegalArgumentException(
                "Không thể hoàn tác vì học sinh đã đổi thưởng hoặc bị trừ sao nên không đủ sao để trừ lại: "
                    + String.join(", ", shortages)
            );
        }

        Map<UUID, StudentScoreEntity> scoresById = plan.scores()
            .stream()
            .collect(Collectors.toMap(StudentScoreEntity::getId, Function.identity()));

        int reversedStars = 0;

        for (Map.Entry<UUID, StudentStarBalanceEntity> entry : balances.entrySet()) {
            StudentStarBalanceEntity balance = entry.getValue();

            int currentBalance = balance.getTotalStars();

            /*
             * one reversal for every star transaction of the import
             */
            for (StarTransactionEntity earn : plan.earnsByStudent().get(entry.getKey())) {
                StudentScoreEntity score = scoresById.get(earn.getScoreId());

                int balanceAfter = Math.subtractExact(currentBalance, earn.getStarDelta());

                starTransactionRepository.save(
                    StarTransactionEntity.builder()
                        .studentId(earn.getStudentId())
                        .classId(earn.getClassId())
                        .transactionType("REVERSAL")
                        .sourceType("SCORE")
                        .starDelta(-earn.getStarDelta())
                        .balanceBefore(currentBalance)
                        .balanceAfter(balanceAfter)
                        .reasonDetail(
                            "Hoàn tác nhập điểm buổi %s (file %s)".formatted(
                                DISPLAY_DATE.format(score.getAssessmentDate()),
                                batch.getSourceSheetId()
                            )
                        )
                        .scoreId(earn.getScoreId())
                        .performedBy(actorId)
                        .idempotencyKey("reversal:" + earn.getId())
                        .reversedTransactionId(earn.getId())
                        .build()
                );

                currentBalance = balanceAfter;
                reversedStars += earn.getStarDelta();
            }

            balance.setTotalStars(currentBalance);
        }

        /*
         * voided scores no longer count, the sessions can be imported again
         */
        plan.scores().forEach(score -> score.voidBy(actorId));

        batch.revert(actorId);

        int studentCount = (int) plan.scores().stream().map(StudentScoreEntity::getStudentId).distinct().count();

        auditLogService.record(
            new AuditLogCommand(
                actorId,
                "SCORE_IMPORT_REVERTED",
                "IMPORT_BATCH",
                batchId,
                Map.of(
                    "fileName",
                    batch.getSourceSheetId(),
                    "voidedSessions",
                    plan.scores().size(),
                    "reversedStars",
                    reversedStars,
                    "students",
                    studentCount
                )
            )
        );

        return new ScoreImportRevertResponse(batchId, plan.scores().size(), reversedStars, studentCount);
    }

    private void requireRevertable(ImportBatchEntity batch) {
        if ("REVERTED".equals(batch.getStatus())) {
            throw new IllegalArgumentException("Lần nhập này đã được hoàn tác");
        }

        if (!"COMPLETED".equals(batch.getStatus())) {
            throw new IllegalArgumentException("Lần nhập này chưa hoàn thành nên không thể hoàn tác");
        }
    }

    /*
     * Active scores of an import and their star transactions, grouped by student
     * (sorted by student id)
     */
    private RevertPlan planRevert(UUID batchId) {
        List<StudentScoreEntity> scores = studentScoreRepository.findByImportBatchIdAndVoidedAtIsNull(batchId);

        Map<UUID, LocalDate> dateByScore = scores.stream()
            .collect(Collectors.toMap(StudentScoreEntity::getId, StudentScoreEntity::getAssessmentDate));

        List<StarTransactionEntity> earns = scores.isEmpty()
            ? List.of()
            : starTransactionRepository.findByScoreIdInAndTransactionType(dateByScore.keySet(), "EARN");

        Map<UUID, List<StarTransactionEntity>> earnsByStudent = new TreeMap<>();

        earns.stream()
            .sorted(Comparator.comparing(earn -> dateByScore.get(earn.getScoreId())))
            .forEach(earn -> earnsByStudent.computeIfAbsent(earn.getStudentId(), key -> new ArrayList<>()).add(earn));

        Map<UUID, Integer> starsByStudent = new TreeMap<>();

        earnsByStudent.forEach(
            (studentId, studentEarns) -> starsByStudent.put(
                studentId,
                studentEarns.stream().mapToInt(StarTransactionEntity::getStarDelta).sum()
            )
        );

        int totalStars = starsByStudent.values().stream().mapToInt(Integer::intValue).sum();

        return new RevertPlan(scores, earnsByStudent, starsByStudent, totalStars);
    }

    private List<ImportRevertStudentResponse> revertStudents(RevertPlan plan, Map<UUID, Integer> balances) {
        if (plan.starsByStudent().isEmpty()) {
            return List.of();
        }

        Map<UUID, StudentEntity> students = studentRepository.findAllById(plan.starsByStudent().keySet())
            .stream()
            .collect(Collectors.toMap(StudentEntity::getId, Function.identity()));

        return plan.starsByStudent().entrySet().stream().map(entry -> {
            StudentEntity student = students.get(entry.getKey());
            int currentBalance = balances.getOrDefault(entry.getKey(), 0);

            return new ImportRevertStudentResponse(
                student.getStudentCode(),
                student.getFullName(),
                entry.getValue(),
                currentBalance,
                currentBalance >= entry.getValue()
            );
        })
            .sorted(Comparator.comparing(ImportRevertStudentResponse::studentCode))
            .toList();
    }

    /*
     * Parse the file and match classes, students and already imported sessions
     */
    private Analysis analyse(MultipartFile file) {
        String fileName = validateFile(file);

        ParsedSheet sheet;

        try (InputStream input = file.getInputStream()) {
            sheet = scoreSheetParser.parse(input);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Không đọc được file. Hãy thử lại");
        }

        List<ImportIssueResponse> issues = new ArrayList<>(sheet.issues());

        Map<String, ClassEntity> classesByCode = classRepository.findByArchivedAtIsNull()
            .stream()
            .collect(Collectors.toMap(c -> normalizeCode(c.getCode()), Function.identity(), (first, second) -> first));

        Set<String> studentCodes = sheet.blocks()
            .stream()
            .flatMap(block -> block.students().stream())
            .map(student -> normalizeCode(student.studentCode()))
            .collect(Collectors.toSet());

        Map<String, StudentEntity> studentsByCode = studentCodes.isEmpty()
            ? Map.of()
            : studentRepository.findByStudentCodeIn(studentCodes)
                .stream()
                .collect(Collectors.toMap(StudentEntity::getStudentCode, Function.identity()));

        List<UUID> studentIds = studentsByCode.values().stream().map(StudentEntity::getId).toList();

        Map<UUID, UUID> currentClassByStudent = studentIds.isEmpty()
            ? Map.of()
            : classEnrollmentRepository.findByStudentIdInAndEndedAtIsNull(studentIds)
                .stream()
                .collect(Collectors.toMap(ClassEnrollmentEntity::getStudentId, ClassEnrollmentEntity::getClassId));

        Map<UUID, Map<LocalDate, StudentScoreEntity>> existingScores = new LinkedHashMap<>();

        if (!studentIds.isEmpty()) {
            for (StudentScoreEntity score : studentScoreRepository.findByStudentIdInAndVoidedAtIsNull(studentIds)) {
                existingScores.computeIfAbsent(score.getStudentId(), key -> new LinkedHashMap<>())
                    .putIfAbsent(score.getAssessmentDate(), score);
            }
        }

        List<ImportClassResponse> classes = new ArrayList<>();
        List<ImportStudentResponse> students = new ArrayList<>();
        List<PlannedScore> plannedScores = new ArrayList<>();
        Set<String> seenStudentCodes = new HashSet<>();

        int skippedSessions = 0;
        int starsToAward = 0;

        for (ParsedClassBlock block : sheet.blocks()) {
            String location = "Lớp " + block.classCode();
            ClassEntity classEntity = classesByCode.get(normalizeCode(block.classCode()));

            if (classEntity == null) {
                classes.add(new ImportClassResponse(block.classCode(), null, false, block.students().size()));
                issues.add(
                    ImportIssueResponse.error(
                        location,
                        "Không tìm thấy lớp có mã \"%s\" trên hệ thống. %d học sinh của lớp này sẽ không được nhập"
                            .formatted(block.classCode(), block.students().size())
                    )
                );
                continue;
            }

            classes.add(
                new ImportClassResponse(block.classCode(), classEntity.getName(), true, block.students().size())
            );

            for (ParsedStudent parsedStudent : block.students()) {
                String studentCode = normalizeCode(parsedStudent.studentCode());
                String rowLocation = location + " · dòng " + parsedStudent.rowNumber();
                String nameSuffix = parsedStudent.fullName() == null ? "" : " - " + parsedStudent.fullName();

                if (!seenStudentCodes.add(studentCode)) {
                    issues.add(
                        ImportIssueResponse.error(
                            rowLocation,
                            "Mã học sinh %s xuất hiện nhiều lần trong file, dòng này bị bỏ qua".formatted(studentCode)
                        )
                    );
                    continue;
                }

                StudentEntity student = studentsByCode.get(studentCode);

                if (student == null) {
                    issues.add(
                        ImportIssueResponse.error(
                            rowLocation,
                            "Không tìm thấy học sinh %s%s trên hệ thống. Hãy thêm học sinh trước rồi upload lại"
                                .formatted(studentCode, nameSuffix)
                        )
                    );
                    continue;
                }

                if (student.getArchivedAt() != null) {
                    issues.add(
                        ImportIssueResponse.error(
                            rowLocation,
                            "Học sinh %s%s đã bị xóa khỏi hệ thống".formatted(studentCode, nameSuffix)
                        )
                    );
                    continue;
                }

                if (!classEntity.getId().equals(currentClassByStudent.get(student.getId()))) {
                    issues.add(
                        ImportIssueResponse.error(
                            rowLocation,
                            "Học sinh %s%s hiện không học lớp %s. Hãy kiểm tra lại lớp của học sinh"
                                .formatted(studentCode, nameSuffix, classEntity.getName())
                        )
                    );
                    continue;
                }

                if (parsedStudent.fullName() != null && !sameName(parsedStudent.fullName(), student.getFullName())) {
                    issues.add(
                        ImportIssueResponse.warning(
                            rowLocation,
                            "Tên trong file (%s) khác tên trên hệ thống (%s) của mã %s"
                                .formatted(parsedStudent.fullName(), student.getFullName(), studentCode)
                        )
                    );
                }

                Map<LocalDate, StudentScoreEntity> studentScores = existingScores.getOrDefault(student.getId(), Map.of());

                List<ImportSessionResponse> sessions = new ArrayList<>();
                int studentNewSessions = 0;
                int studentSkippedSessions = 0;
                int studentStars = 0;

                for (ParsedSession session : parsedStudent.sessions()) {
                    BigDecimal exactFinalScore = scoreCalculator.exactFinalScore(
                        session.homeworkScore(),
                        session.testScore()
                    );
                    BigDecimal finalScore = scoreCalculator.storedFinalScore(exactFinalScore);
                    int awardedStars = scoreCalculator.awardedStars(exactFinalScore);

                    StudentScoreEntity existing = studentScores.get(session.assessmentDate());

                    String status;

                    if (existing == null) {
                        status = "NEW";
                        studentNewSessions++;
                        studentStars += awardedStars;

                        plannedScores.add(
                            new PlannedScore(
                                student.getId(),
                                classEntity.getId(),
                                studentCode,
                                session.assessmentDate(),
                                session.homeworkScore(),
                                session.testScore(),
                                finalScore,
                                awardedStars
                            )
                        );
                    } else if (sameScores(existing, session)) {
                        status = "EXISTS";
                        studentSkippedSessions++;
                    } else {
                        status = "CONFLICT";
                        studentSkippedSessions++;

                        issues.add(
                            ImportIssueResponse.warning(
                                rowLocation,
                                "Buổi %s của %s đã được nhập trước đó (BTVN %s · 15p %s). Điểm trong file khác nên không được cập nhật"
                                    .formatted(
                                        DISPLAY_DATE.format(session.assessmentDate()),
                                        studentCode,
                                        existing.getHomeworkScore().stripTrailingZeros().toPlainString(),
                                        existing.getTestScore().stripTrailingZeros().toPlainString()
                                    )
                            )
                        );
                    }

                    sessions.add(
                        new ImportSessionResponse(
                            session.assessmentDate(),
                            session.homeworkScore(),
                            session.testScore(),
                            finalScore,
                            awardedStars,
                            status
                        )
                    );
                }

                skippedSessions += studentSkippedSessions;
                starsToAward += studentStars;

                students.add(
                    new ImportStudentResponse(
                        studentCode,
                        student.getFullName(),
                        block.classCode(),
                        studentNewSessions,
                        studentSkippedSessions,
                        studentStars,
                        sessions
                    )
                );
            }
        }

        int errors = (int) issues.stream().filter(issue -> "ERROR".equals(issue.severity())).count();

        ScoreImportPreviewResponse preview = new ScoreImportPreviewResponse(
            fileName,
            sheet.sheetName(),
            classes,
            students,
            new ImportTotalsResponse(students.size(), plannedScores.size(), skippedSessions, starsToAward, errors),
            issues
        );

        return new Analysis(fileName, preview, plannedScores);
    }

    private String validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Hãy chọn file bảng điểm");
        }

        String fileName = file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()
            ? "bang-diem.xlsx"
            : file.getOriginalFilename().trim();

        String lowerFileName = fileName.toLowerCase(Locale.ROOT);

        if (!lowerFileName.endsWith(".xlsx") && !lowerFileName.endsWith(".xls")) {
            throw new IllegalArgumentException("Chỉ hỗ trợ file Excel (.xlsx)");
        }

        return fileName.length() > MAX_FILE_NAME_LENGTH ? fileName.substring(0, MAX_FILE_NAME_LENGTH) : fileName;
    }

    private String scoreReason(StudentScoreEntity score) {
        return "Điểm buổi %s: BTVN %s · 15p %s → %s".formatted(
            DISPLAY_DATE.format(score.getAssessmentDate()),
            score.getHomeworkScore().stripTrailingZeros().toPlainString(),
            score.getTestScore().stripTrailingZeros().toPlainString(),
            score.getFinalScore().stripTrailingZeros().toPlainString()
        );
    }

    private boolean sameScores(StudentScoreEntity existing, ParsedSession session) {
        return existing.getHomeworkScore().compareTo(session.homeworkScore()) == 0
            && existing.getTestScore().compareTo(session.testScore()) == 0;
    }

    private boolean sameName(String first, String second) {
        return normalizeName(first).equals(normalizeName(second));
    }

    private String normalizeName(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFC).trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /*
     * Class code and student code are compared in upper case, same as when they
     * are created
     */
    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
