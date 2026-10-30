package com.sr.classes.service.impl;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sr.classes.dto.request.CreateClassRequest;
import com.sr.classes.dto.response.ClassResponse;
import com.sr.classes.dto.response.CreateClassResponse;
import com.sr.classes.service.ClassService;
import com.sr.common.audit.dto.AuditLogCommand;
import com.sr.common.audit.service.AuditLogService;
import com.sr.user.service.UserService;

@Service
public class ClassServiceImpl implements ClassService {

    private final JdbcClient jdbcClient;
    private final UserService userService;
    private final AuditLogService auditLogService;

    public ClassServiceImpl(JdbcClient jdbcClient, UserService userService, AuditLogService auditLogService) {
        this.jdbcClient = jdbcClient;
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassResponse> getClasses() {
        return jdbcClient.sql("""
                    SELECT
                        c.id AS class_id,
                        c.code AS class_code,
                        c.name AS class_name,
                        COUNT(e.student_id) AS student_count,
                        COALESCE(SUM(b.total_stars), 0) AS total_stars
                    FROM classes c
                    LEFT JOIN class_enrollments e
                        ON e.class_id = c.id
                        AND e.ended_at IS NULL
                    LEFT JOIN student_star_balances b
                        ON b.student_id = e.student_id
                    WHERE c.archived_at IS NULL
                    GROUP BY c.id, c.code, c.name
                    ORDER BY c.name
            """)
            .query(ClassResponse.class)
            .list();
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

        UUID classId = UUID.randomUUID();
        UUID actorId = userService.getUserId();

        try {
            int insertedRows = jdbcClient.sql("""
                    INSERT INTO classes (
                        id,
                        code,
                        name,
                        school_year
                    )
                    VALUES (
                        :classId,
                        :code,
                        :name,
                        :schoolYear
                    )
                """)
                .param("classId", classId)
                .param("code", normalizedCode)
                .param("name", normalizedName)
                .param("schoolYear", request.schoolYear())
                .update();

            if (insertedRows != 1) {
                throw new IllegalStateException("Không thể tạo lớp học");
            }
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("Mã lớp đã tồn tại");
        }

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
}
