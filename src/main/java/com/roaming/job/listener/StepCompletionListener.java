package com.roaming.job.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
public class StepCompletionListener implements StepExecutionListener {

    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info("=== Step Started ===");
        log.info("Step Name: {}", stepExecution.getStepName());
        log.info("Start Time: {}", stepExecution.getStartTime());
        log.info("====================");
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.info("=== Step Completed ===");
        log.info("Step Name: {}", stepExecution.getStepName());
        log.info("Status: {}", stepExecution.getStatus());
        log.info("Exit Code: {}", stepExecution.getExitStatus().getExitCode());
        
        if (stepExecution.getStartTime() != null && stepExecution.getEndTime() != null) {
            Duration duration = Duration.between(
                stepExecution.getStartTime().toLocalDateTime(),
                stepExecution.getEndTime().toLocalDateTime()
            );
            log.info("Step Execution Time: {} ms", duration.toMillis());
        }
        
        // 처리 통계 로깅
        log.info("=== Processing Statistics ===");
        log.info("Read Count: {}", stepExecution.getReadCount());
        log.info("Write Count: {}", stepExecution.getWriteCount());
        log.info("Commit Count: {}", stepExecution.getCommitCount());
        log.info("Rollback Count: {}", stepExecution.getRollbackCount());
        log.info("Skip Count: {}", stepExecution.getSkipCount());
        log.info("Filter Count: {}", stepExecution.getFilterCount());
        log.info("Process Skip Count: {}", stepExecution.getProcessSkipCount());
        log.info("Read Skip Count: {}", stepExecution.getReadSkipCount());
        log.info("Write Skip Count: {}", stepExecution.getWriteSkipCount());
        
        // 오류 정보 로깅
        if (!stepExecution.getFailureExceptions().isEmpty()) {
            log.error("Step failed with {} exceptions:", stepExecution.getFailureExceptions().size());
            stepExecution.getFailureExceptions().forEach(exception -> 
                log.error("Exception: {}", exception.getMessage(), exception)
            );
        }
        
        if (stepExecution.getSkipCount() > 0) {
            log.warn("Step completed with {} skipped items", stepExecution.getSkipCount());
        }
        
        log.info("==============================");
        
        return stepExecution.getExitStatus();
    }
}