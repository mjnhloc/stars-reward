package com.sr.dashboard.service.impl;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import com.sr.dashboard.dto.response.DashboardResponse;
import com.sr.dashboard.service.DashboardService;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final JdbcClient jdbcClient;

    public DashboardServiceImpl(
        JdbcClient jdbcClient
    ) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public DashboardResponse getDashboard() {

        StringBuilder sql = new StringBuilder("""
                SELECT
                    COUNT(*) AS total_students,
                    COALESCE(SUM(total_stars), 0) AS total_stars
                FROM student_star_balances
            """);

        JdbcClient.StatementSpec statement = jdbcClient.sql(sql.toString());
        return statement.query(DashboardResponse.class).single();
    }
}
