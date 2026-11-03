package com.sr.dashboard.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sr.dashboard.dto.response.DashboardResponse;
import com.sr.dashboard.service.DashboardService;
import com.sr.student.repository.StudentStarBalanceRepository;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final StudentStarBalanceRepository studentStarBalanceRepository;

    public DashboardServiceImpl(
        StudentStarBalanceRepository studentStarBalanceRepository
    ) {
        this.studentStarBalanceRepository = studentStarBalanceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        return studentStarBalanceRepository.getDashboardSummary();
    }
}
