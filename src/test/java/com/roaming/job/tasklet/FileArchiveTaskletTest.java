package com.roaming.job.tasklet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileArchiveTaskletTest {

    private FileArchiveTasklet fileArchiveTasklet;
    private StepContribution stepContribution;
    private ChunkContext chunkContext;
    private StepContext stepContext;
    private StepExecution stepExecution;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileArchiveTasklet = new FileArchiveTasklet();
        stepContribution = mock(StepContribution.class);
        chunkContext = mock(ChunkContext.class);
        stepContext = mock(StepContext.class);
        stepExecution = mock(StepExecution.class);

        when(chunkContext.getStepContext()).thenReturn(stepContext);
        when(stepContext.getStepExecution()).thenReturn(stepExecution);
        when(stepExecution.getExecutionContext()).thenReturn(new org.springframework.batch.item.ExecutionContext());
    }

    @Test
    void 파일_아카이브_작업_성공() throws Exception {
        // Given
        Path sourceDir = tempDir.resolve("source");
        Path testFile = sourceDir.resolve("test.csv");
        Files.createDirectories(sourceDir);
        Files.write(testFile, "test data".getBytes());

        ReflectionTestUtils.setField(fileArchiveTasklet, "sourceDirectory", sourceDir.toString());
        ReflectionTestUtils.setField(fileArchiveTasklet, "backupDirectory", tempDir.resolve("backup").toString());
        ReflectionTestUtils.setField(fileArchiveTasklet, "filePattern", "*.csv");

        // When
        RepeatStatus result = fileArchiveTasklet.execute(stepContribution, chunkContext);

        // Then
        assertEquals(RepeatStatus.FINISHED, result);
        assertFalse(Files.exists(testFile)); // 원본 파일이 이동됨
        
        // 백업 디렉터리에 파일이 존재하는지 확인
        Path backupDir = tempDir.resolve("backup");
        assertTrue(Files.exists(backupDir));
        assertTrue(Files.list(backupDir).anyMatch(path -> 
            Files.isDirectory(path) && path.getFileName().toString().startsWith("archive_")));
    }

    @Test
    void 소스_디렉터리가_없는_경우() throws Exception {
        // Given
        ReflectionTestUtils.setField(fileArchiveTasklet, "sourceDirectory", "non-existent-dir");
        ReflectionTestUtils.setField(fileArchiveTasklet, "backupDirectory", tempDir.resolve("backup").toString());
        ReflectionTestUtils.setField(fileArchiveTasklet, "filePattern", "*.csv");

        // When
        RepeatStatus result = fileArchiveTasklet.execute(stepContribution, chunkContext);

        // Then
        assertEquals(RepeatStatus.FINISHED, result);
    }

    @Test
    void 아카이브할_파일이_없는_경우() throws Exception {
        // Given
        Path sourceDir = tempDir.resolve("source");
        Files.createDirectories(sourceDir);

        ReflectionTestUtils.setField(fileArchiveTasklet, "sourceDirectory", sourceDir.toString());
        ReflectionTestUtils.setField(fileArchiveTasklet, "backupDirectory", tempDir.resolve("backup").toString());
        ReflectionTestUtils.setField(fileArchiveTasklet, "filePattern", "*.csv");

        // When
        RepeatStatus result = fileArchiveTasklet.execute(stepContribution, chunkContext);

        // Then
        assertEquals(RepeatStatus.FINISHED, result);
    }
}