package com.sr.scoreimport.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sr.scoreimport.dto.response.ImportBatchRow;
import com.sr.scoreimport.entity.ImportBatchEntity;

import jakarta.persistence.LockModeType;

public interface ImportBatchRepository extends JpaRepository<ImportBatchEntity, UUID> {

    /*
     * Finished imports (completed or reverted), newest first
     */
    @Query("""
        SELECT new com.sr.scoreimport.dto.response.ImportBatchRow(
            b.id,
            b.sourceSheetId,
            b.status,
            b.startedAt,
            importer.displayName,
            b.revertedAt,
            reverter.displayName
        )
        FROM ImportBatchEntity b
        LEFT JOIN UserEntity importer
            ON importer.id = b.createdBy
        LEFT JOIN UserEntity reverter
            ON reverter.id = b.revertedBy
        WHERE b.status IN ('COMPLETED', 'REVERTED')
        ORDER BY b.startedAt DESC
        """)
    List<ImportBatchRow> findHistory(Limit limit);

    /*
     * SELECT ... FOR UPDATE
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT b
        FROM ImportBatchEntity b
        WHERE b.id = :batchId
        """)
    Optional<ImportBatchEntity> findForUpdate(@Param("batchId") UUID batchId);
}
