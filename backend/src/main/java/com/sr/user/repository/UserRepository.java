package com.sr.user.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sr.user.entity.UserEntity;

import jakarta.persistence.LockModeType;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByIdAndActiveTrue(UUID id);

    /*
     * Use LOWER(email) so that the query can use index uq_users_email_lower
     */
    @Query("""
        SELECT u
        FROM UserEntity u
        WHERE LOWER(u.email) = :email
        """)
    Optional<UserEntity> findByNormalizedEmail(@Param("email") String email);

    /*
     * SELECT ... FOR UPDATE
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT u
        FROM UserEntity u
        WHERE u.id = :userId
            AND u.active = TRUE
        """)
    Optional<UserEntity> findActiveForUpdate(@Param("userId") UUID userId);

    List<UserEntity> findAllByOrderByCreatedAtAsc();

    long countByRoleAndActiveTrue(String role);

    /*
     * SELECT ... FOR UPDATE, also for locked users
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT u
        FROM UserEntity u
        WHERE u.id = :userId
        """)
    Optional<UserEntity> findForUpdate(@Param("userId") UUID userId);
}
