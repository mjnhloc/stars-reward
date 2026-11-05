package com.sr.scoreimport.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "import_batches")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImportBatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Uploaded file name
     */
    @Column(name = "source_sheet_id", nullable = false)
    private String sourceSheetId;

    @Column(nullable = false)
    private String status;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "row_count", nullable = false)
    private int rowCount;

    @Column(name = "error_count", nullable = false)
    private int errorCount;

    public static ImportBatchEntity start(String sourceSheetId, UUID createdBy) {
        ImportBatchEntity batch = new ImportBatchEntity();
        batch.sourceSheetId = sourceSheetId;
        batch.status = "PROCESSING";
        batch.startedAt = OffsetDateTime.now();
        batch.createdBy = createdBy;
        return batch;
    }
}
