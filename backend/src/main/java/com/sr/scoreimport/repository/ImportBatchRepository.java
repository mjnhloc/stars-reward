package com.sr.scoreimport.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sr.scoreimport.entity.ImportBatchEntity;

public interface ImportBatchRepository extends JpaRepository<ImportBatchEntity, UUID> {
}
