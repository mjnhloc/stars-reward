package com.sr.scoreimport.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/*
 * One session of a student in the score sheet (15p = test, BTVN = homework)
 */
public record ParsedSession(
    LocalDate assessmentDate,
    BigDecimal homeworkScore,
    BigDecimal testScore
) {}
