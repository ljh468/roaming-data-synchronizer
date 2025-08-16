package com.roaming.job.tasklet;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 처리 완료된 파일들을 백업 디렉터리로 이동하는 Tasklet
 */
@Slf4j
@Component
public class FileArchiveTasklet implements Tasklet {

    @Value("${batch.archive.source-directory:src/main/resources/data}")
    private String sourceDirectory;

    @Value("${batch.archive.backup-directory:backup}")
    private String backupDirectory;

    @Value("${batch.archive.file-pattern:*.csv}")
    private String filePattern;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("파일 아카이브 작업을 시작합니다. source: {}, backup: {}", sourceDirectory, backupDirectory);

        try {
            // 백업 디렉터리 생성
            Path backupPath = createBackupDirectory();
            
            // 소스 디렉터리의 파일들을 백업 디렉터리로 이동
            int archivedCount = archiveFiles(backupPath);
            
            log.info("파일 아카이브 작업이 완료되었습니다. 처리된 파일 수: {}", archivedCount);
            
            // Step 실행 컨텍스트에 결과 저장
            chunkContext.getStepContext().getStepExecution()
                    .getExecutionContext().putInt("archivedFileCount", archivedCount);
            
            return RepeatStatus.FINISHED;
            
        } catch (Exception e) {
            log.error("파일 아카이브 작업 중 오류가 발생했습니다.", e);
            throw e;
        }
    }

    private Path createBackupDirectory() throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path backupPath = Paths.get(backupDirectory, "archive_" + timestamp);
        
        if (!Files.exists(backupPath)) {
            Files.createDirectories(backupPath);
            log.info("백업 디렉터리를 생성했습니다: {}", backupPath.toAbsolutePath());
        }
        
        return backupPath;
    }

    private int archiveFiles(Path backupPath) throws IOException {
        Path sourcePath = Paths.get(sourceDirectory);
        int archivedCount = 0;

        if (!Files.exists(sourcePath)) {
            log.warn("소스 디렉터리가 존재하지 않습니다: {}", sourcePath.toAbsolutePath());
            return 0;
        }

        try (var stream = Files.list(sourcePath)) {
            var files = stream
                    .filter(Files::isRegularFile)
                    .filter(this::matchesFilePattern)
                    .toList();

            for (Path file : files) {
                try {
                    Path targetPath = backupPath.resolve(file.getFileName());
                    Files.move(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    log.debug("파일을 이동했습니다: {} -> {}", file.getFileName(), targetPath);
                    archivedCount++;
                } catch (IOException e) {
                    log.error("파일 이동 중 오류 발생: {}", file.getFileName(), e);
                    // 개별 파일 실패는 전체 작업을 중단하지 않음
                }
            }
        }

        return archivedCount;
    }

    private boolean matchesFilePattern(Path file) {
        String fileName = file.getFileName().toString();
        String pattern = filePattern.replace("*", ".*").replace("?", ".");
        return fileName.matches(pattern);
    }
}