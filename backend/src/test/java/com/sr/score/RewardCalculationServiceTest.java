package com.sr.score;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import com.sr.reward.service.impl.RewardCalculationServiceImpl;

class RewardCalculationServiceTest {
    // private final RewardCalculationServiceImpl service = new RewardCalculationServiceImpl(10, 50000);

    // @Test
    // void exampleIsExact() {
    //     var score = service.finalScore(new BigDecimal("10"), new BigDecimal("8"));
    //     assertEquals(new BigDecimal("9.20"), score);
    //     assertEquals(2, service.starsFor(score));
    // }

    // @Test
    // void boundaries() {
    //     assertEquals(3, service.starsFor(new BigDecimal("10.00")));
    //     assertEquals(2, service.starsFor(new BigDecimal("9.00")));
    //     assertEquals(1, service.starsFor(new BigDecimal("8.00")));
    //     assertEquals(0, service.starsFor(new BigDecimal("7.99")));
    // }

    // @Test
    // void rejectsOutOfRange() {
    //     assertThrows(IllegalArgumentException.class,
    //             () -> service.finalScore(new BigDecimal("10.01"), BigDecimal.ZERO));
    // }
}
