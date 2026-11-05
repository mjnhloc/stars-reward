package com.sr.scoreimport.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

/*
 * The only place that holds the score formula
 * final = homework * 0.70 + test * 0.30
 * final = 10 -> 3 stars, >= 9 -> 2 stars, >= 8 -> 1 star, otherwise 0
 */
@Component
public class ScoreCalculator {

    private static final BigDecimal HOMEWORK_WEIGHT = new BigDecimal("0.70");
    private static final BigDecimal TEST_WEIGHT = new BigDecimal("0.30");

    private static final BigDecimal THREE_STARS = new BigDecimal("10");
    private static final BigDecimal TWO_STARS = new BigDecimal("9");
    private static final BigDecimal ONE_STAR = new BigDecimal("8");

    /*
     * Exact value, not rounded. Use it to calculate stars
     */
    public BigDecimal exactFinalScore(BigDecimal homeworkScore, BigDecimal testScore) {
        return homeworkScore.multiply(HOMEWORK_WEIGHT).add(testScore.multiply(TEST_WEIGHT));
    }

    /*
     * Value stored in database (scale 2)
     */
    public BigDecimal storedFinalScore(BigDecimal exactFinalScore) {
        return exactFinalScore.setScale(2, RoundingMode.HALF_UP);
    }

    public int awardedStars(BigDecimal exactFinalScore) {
        if (exactFinalScore.compareTo(THREE_STARS) >= 0) {
            return 3;
        }

        if (exactFinalScore.compareTo(TWO_STARS) >= 0) {
            return 2;
        }

        if (exactFinalScore.compareTo(ONE_STAR) >= 0) {
            return 1;
        }

        return 0;
    }
}
