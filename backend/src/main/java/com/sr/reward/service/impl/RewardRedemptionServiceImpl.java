package com.sr.reward.service.impl;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.sr.auth.service.AuthorizationService;
import com.sr.common.audit.dto.AuditLogCommand;
import com.sr.common.audit.service.AuditLogService;
import com.sr.reward.dto.request.RewardRedemptionRequest;
import com.sr.reward.dto.response.RewardRedemptionResponse;
import com.sr.reward.entity.RewardRedemptionEntity;
import com.sr.reward.repository.RewardRedemptionRepository;
import com.sr.reward.service.RewardCalculationService;
import com.sr.reward.service.RewardRedemptionService;
import com.sr.student.entity.StarTransactionEntity;
import com.sr.student.entity.StudentStarBalanceEntity;
import com.sr.student.repository.ClassEnrollmentRepository;
import com.sr.student.repository.StarTransactionRepository;
import com.sr.student.repository.StudentStarBalanceRepository;
import com.sr.user.service.UserService;

@Service
public class RewardRedemptionServiceImpl implements RewardRedemptionService {

    private final RewardRedemptionRepository rewardRedemptionRepository;
    private final StarTransactionRepository starTransactionRepository;
    private final StudentStarBalanceRepository studentStarBalanceRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final UserService userService;
    private final RewardCalculationService rewardCalculationService;
    private final AuthorizationService authorizationService;
    private final AuditLogService auditLogService;

    public RewardRedemptionServiceImpl(
        RewardRedemptionRepository rewardRedemptionRepository,
        StarTransactionRepository starTransactionRepository,
        StudentStarBalanceRepository studentStarBalanceRepository,
        ClassEnrollmentRepository classEnrollmentRepository,
        UserService userService,
        RewardCalculationService rewardCalculationService,
        AuthorizationService authorizationService,
        AuditLogService auditLogService
    ) {
        this.rewardRedemptionRepository = rewardRedemptionRepository;
        this.starTransactionRepository = starTransactionRepository;
        this.studentStarBalanceRepository = studentStarBalanceRepository;
        this.classEnrollmentRepository = classEnrollmentRepository;
        this.userService = userService;
        this.rewardCalculationService = rewardCalculationService;
        this.authorizationService = authorizationService;
        this.auditLogService = auditLogService;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RewardRedemptionResponse redeem(UUID studentId, String idempotencyKey, RewardRedemptionRequest request) {
        validateRequest(studentId, idempotencyKey, request);

        /*
         * If user doesn't have right to modify the class, stop before locking balance
         */
        UUID actorId = userService.getUserId();
        authorizationService.requireAdjust(actorId, request.classId());

        /*
         * Check to see if there is a same existing redemption
         */
        RewardRedemptionResponse existing = rewardRedemptionRepository.findByIdempotencyKey(idempotencyKey)
            .map(response -> {
                if (!studentId.equals(response.studentId())) {
                    throw new IllegalArgumentException(
                        "Idempotency-Key đã được sử dụng "
                            + "cho yêu cầu khác"
                    );
                }

                return response;
            })
            .orElse(null);

        if (existing != null) {
            return existing;
        }

        validateActiveEnrollment(studentId, request.classId());

        /*
         * Lock balance so that 2 requests cannot redeem the same stars
         */
        StudentStarBalanceEntity balance = studentStarBalanceRepository.findForUpdate(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tổng số sao của học sinh " + studentId));

        int totalStars = balance.getTotalStars();

        int redeemableStars = rewardCalculationService.calculateRedeemableStars(totalStars);
        long redeemableValue = rewardCalculationService.calculateRedeemableValue(totalStars);
        int remainingStars = rewardCalculationService.calculateRemainingStars(totalStars);

        OffsetDateTime now = OffsetDateTime.now();

        /*
         * Update insert reward redemption
         */
        RewardRedemptionEntity redemption = rewardRedemptionRepository.save(
            RewardRedemptionEntity.builder()
                .studentId(studentId)
                .starsRedeemed(redeemableStars)
                .starsPerRedemption(rewardCalculationService.getStarsPerRedemption())
                .valuePerRedemption(rewardCalculationService.getVndPerRedemption())
                .amountVnd(redeemableValue)
                .status("PAID")
                .createdBy(actorId)
                .paidAt(now)
                .build()
        );

        UUID redemptionId = redemption.getId();

        /*
         * update insert new star transaction
         */
        StarTransactionEntity transaction = starTransactionRepository.save(
            StarTransactionEntity.builder()
                .studentId(studentId)
                .classId(request.classId())
                .redemptionId(redemptionId)
                .transactionType("REDEEM")
                .sourceType("REDEMPTION")
                .starDelta(-redeemableStars)
                .balanceBefore(totalStars)
                .balanceAfter(remainingStars)
                .reasonDetail("Đổi " + redeemableStars + " sao thành " + redeemableValue + " VND")
                .performedBy(actorId)
                .idempotencyKey(idempotencyKey)
                .build()
        );

        UUID transactionId = transaction.getId();

        /*
         * update student star balance (version and updated_at are handled by
         * Hibernate)
         */
        balance.setTotalStars(remainingStars);

        auditLogService.record(
            new AuditLogCommand(
                actorId,
                "REWARD_REDEEMED",
                "REWARD_REDEMPTION",
                redemptionId,
                Map.of(
                    "studentId",
                    studentId.toString(),
                    "classId",
                    request.classId().toString(),
                    "transactionId",
                    transactionId.toString(),
                    "balanceBefore",
                    totalStars,
                    "starsRedeemed",
                    redeemableStars,
                    "balanceAfter",
                    remainingStars,
                    "amountVnd",
                    redeemableValue
                )
            )
        );

        return new RewardRedemptionResponse(
            redemptionId,
            studentId,
            redeemableStars,
            redeemableValue,
            remainingStars,
            "PAID",
            now
        );
    }

    /*
     * Helper function to validate if the student go to a specific class
     */
    private void validateActiveEnrollment(
        UUID studentId,
        UUID classId
    ) {
        boolean enrolled = classEnrollmentRepository.existsByStudentIdAndClassIdAndEndedAtIsNull(
            studentId,
            classId
        );

        if (!enrolled) {
            throw new IllegalArgumentException(
                "Học sinh không thuộc lớp đã chọn"
            );
        }
    }

    /*
     * Helper function to validate the request argument from frontend
     */
    private void validateRequest(
        UUID studentId,
        String idempotencyKey,
        RewardRedemptionRequest request
    ) {
        if (studentId == null) {
            throw new IllegalArgumentException(
                "Học sinh không được để trống"
            );
        }

        if (request == null
            || request.classId() == null) {
            throw new IllegalArgumentException(
                "Lớp học không được để trống"
            );
        }

        if (idempotencyKey == null
            || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(
                "Thiếu Idempotency-Key"
            );
        }

        if (idempotencyKey.length() > 150) {
            throw new IllegalArgumentException(
                "Idempotency-Key không được vượt quá 150 ký tự"
            );
        }
    }
}
