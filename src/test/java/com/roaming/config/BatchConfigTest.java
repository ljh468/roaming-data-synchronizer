package com.roaming.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.batch.job.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@SpringBatchTest
class BatchConfigTest {

    @Autowired
    private BatchConfig batchConfig;

    @Test
    @DisplayName("chunkSyncJob 빈이 정상적으로 생성되어야 한다")
    void should_CreateChunkSyncJob_when_SpringContextLoaded() {
        // When
        Job chunkSyncJob = batchConfig.chunkSyncJob();

        // Then
        assertThat(chunkSyncJob).isNotNull();
        assertThat(chunkSyncJob.getName()).isEqualTo("chunkSyncJob");
    }

    @Test
    @DisplayName("robustSyncJob 빈이 정상적으로 생성되어야 한다")
    void should_CreateRobustSyncJob_when_SpringContextLoaded() {
        // When
        Job robustSyncJob = batchConfig.robustSyncJob();

        // Then
        assertThat(robustSyncJob).isNotNull();
        assertThat(robustSyncJob.getName()).isEqualTo("robustSyncJob");
    }

    @Test
    @DisplayName("chunkReadAndSaveStep 빈이 정상적으로 생성되어야 한다")
    void should_CreateChunkReadAndSaveStep_when_SpringContextLoaded() {
        // When
        Step step = batchConfig.chunkReadAndSaveStep();

        // Then
        assertThat(step).isNotNull();
        assertThat(step.getName()).isEqualTo("chunkReadAndSaveStep");
    }

    @Test
    @DisplayName("robustReadAndSaveStep 빈이 정상적으로 생성되어야 한다")
    void should_CreateRobustReadAndSaveStep_when_SpringContextLoaded() {
        // When
        Step step = batchConfig.robustReadAndSaveStep();

        // Then
        assertThat(step).isNotNull();
        assertThat(step.getName()).isEqualTo("robustReadAndSaveStep");
    }

    @Test
    @DisplayName("csvItemReader 빈이 정상적으로 생성되어야 한다")
    void should_CreateCsvItemReader_when_SpringContextLoaded() {
        // When
        FlatFileItemReader<?> reader = batchConfig.csvItemReader();

        // Then
        assertThat(reader).isNotNull();
        assertThat(reader.getName()).isEqualTo("csvItemReader");
    }

    @Test
    @DisplayName("jpaItemWriter 빈이 정상적으로 생성되어야 한다")
    void should_CreateJpaItemWriter_when_SpringContextLoaded() {
        // When
        JpaItemWriter<?> writer = batchConfig.jpaItemWriter();

        // Then
        assertThat(writer).isNotNull();
    }
}