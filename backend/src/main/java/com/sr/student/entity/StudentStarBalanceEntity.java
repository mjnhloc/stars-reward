package com.sr.student.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "student_star_balances")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentStarBalanceEntity {

    /*
     * Primary key is also the student id (assigned manually, not generated)
     */
    @Id
    @Column(name = "student_id")
    private UUID studentId;

    @Column(name = "total_stars", nullable = false)
    private int totalStars;

    /*
     * Hibernate increases version automatically on every update. Wrapper type is
     * required so that Spring Data treats a null version as a new row
     */
    @Version
    @Column(nullable = false)
    private Long version;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public static StudentStarBalanceEntity create(UUID studentId) {
        StudentStarBalanceEntity balance = new StudentStarBalanceEntity();
        balance.studentId = studentId;
        balance.totalStars = 0;
        return balance;
    }
}
