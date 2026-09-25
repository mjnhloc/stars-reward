package com.sr.classes.service.impl;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import com.sr.classes.dto.response.ClassResponse;
import com.sr.classes.service.ClassService;

@Service
public class ClassServiceImpl implements ClassService {

    private final JdbcClient jdbcClient;

    public ClassServiceImpl(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<ClassResponse> getClasses() {
        StringBuilder sql = new StringBuilder("""
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
            """);

        JdbcClient.StatementSpec statement = jdbcClient.sql(sql.toString());
        return statement.query(ClassResponse.class).list();
    }
}
