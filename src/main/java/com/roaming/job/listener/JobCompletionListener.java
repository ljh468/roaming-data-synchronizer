package com.roaming.job.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Component
public class JobCompletionListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("=== Job Started ===");
        log.info("Job Name: {}", jobExecution.getJobInstance().getJobName());
        log.info("Job Parameters: {}", jobExecution.getJobParameters());
        log.info("Start Time: {}", jobExecution.getStartTime());
        log.info("==================");
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("=== Job Completed ===");
        log.info("Job Name: {}", jobExecution.getJobInstance().getJobName());
        log.info("Job Status: {}", jobExecution.getStatus());
        log.info("Start Time: {}", jobExecution.getStartTime());
        log.info("End Time: {}", jobExecution.getEndTime());
        
        if (jobExecution.getStartTime() != null && jobExecution.getEndTime() != null) {
            Duration duration = Duration.between(
                jobExecution.getStartTime(),
                jobExecution.getEndTime()
            );
            log.info("Total Execution Time: {} ms", duration.toMillis());
        }
        
        // Step별 실행 결과 요약
        log.info("=== Step Summary ===");
        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            log.info("Step: {} | Status: {} | Read: {} | Write: {} | Skip: {} | Errors: {}",
                stepExecution.getStepName(),
                stepExecution.getStatus(),
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                stepExecution.getSkipCount(),
                stepExecution.getFailureExceptions().size()
            );
        }
        
        if (jobExecution.getStatus().isUnsuccessful()) {
            log.error("Job failed with exceptions:");
            jobExecution.getAllFailureExceptions().forEach(exception -> 
                log.error("Exception: {}", exception.getMessage(), exception)
            );
        }
        
        log.info("=====================");
    }
}