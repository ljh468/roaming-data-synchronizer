package com.roaming.job.tasklet;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;

/**
 * Job 실행 완료 후 결과를 요약하고 알림을 발송하는 Tasklet
 */
@Slf4j
@Component
public class CompletionNotificationTasklet implements Tasklet {

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        JobExecution jobExecution = chunkContext.getStepContext().getStepExecution().getJobExecution();
        
        log.info("=".repeat(80));
        log.info("배치 작업 완료 알림을 시작합니다.");
        log.info("=".repeat(80));
        
        // Job 실행 정보 수집
        JobExecutionSummary summary = createJobExecutionSummary(jobExecution);
        
        // 결과 요약 로그 출력
        logExecutionSummary(summary);
        
        // 필요시 외부 알림 시스템 연동 (이메일, 슬랙 등)
        sendNotificationIfNeeded(summary);
        
        log.info("=".repeat(80));
        log.info("배치 작업 완료 알림이 종료되었습니다.");
        log.info("=".repeat(80));
        
        return RepeatStatus.FINISHED;
    }

    private JobExecutionSummary createJobExecutionSummary(JobExecution jobExecution) {
        LocalDateTime startTime = jobExecution.getStartTime();
        LocalDateTime endTime = jobExecution.getEndTime();
        Duration duration = Duration.between(startTime, endTime);
        
        Collection<StepExecution> stepExecutions = jobExecution.getStepExecutions();
        
        return JobExecutionSummary.builder()
                .jobName(jobExecution.getJobInstance().getJobName())
                .jobId(jobExecution.getJobId())
                .status(jobExecution.getStatus())
                .startTime(startTime)
                .endTime(endTime)
                .duration(duration)
                .stepExecutions(stepExecutions)
                .exitCode(jobExecution.getExitStatus().getExitCode())
                .exitDescription(jobExecution.getExitStatus().getExitDescription())
                .build();
    }

    private void logExecutionSummary(JobExecutionSummary summary) {
        log.info("📊 Job 실행 요약 정보");
        log.info("  - Job 이름: {}", summary.getJobName());
        log.info("  - Job ID: {}", summary.getJobId());
        log.info("  - 실행 상태: {}", summary.getStatus());
        log.info("  - 시작 시간: {}", summary.getStartTime());
        log.info("  - 종료 시간: {}", summary.getEndTime());
        log.info("  - 실행 시간: {}초", summary.getDuration().toSeconds());
        log.info("  - 종료 코드: {}", summary.getExitCode());
        
        if (summary.getExitDescription() != null && !summary.getExitDescription().isEmpty()) {
            log.info("  - 종료 설명: {}", summary.getExitDescription());
        }
        
        log.info("📈 Step 실행 상세 정보");
        summary.getStepExecutions().forEach(stepExecution -> {
            log.info("  Step: {} | 상태: {} | 읽음: {} | 쓰임: {} | 건너뜀: {}",
                    stepExecution.getStepName(),
                    stepExecution.getStatus(),
                    stepExecution.getReadCount(),
                    stepExecution.getWriteCount(),
                    stepExecution.getSkipCount());
            
            // 실행 컨텍스트에서 추가 정보 수집
            var executionContext = stepExecution.getExecutionContext();
            if (executionContext.containsKey("archivedFileCount")) {
                log.info("    - 아카이브된 파일 수: {}", executionContext.getInt("archivedFileCount"));
            }
        });
    }

    private void sendNotificationIfNeeded(JobExecutionSummary summary) {
        // 실패한 경우에만 알림 발송 (실제 구현에서는 설정으로 제어)
        if (summary.getStatus() == BatchStatus.FAILED) {
            log.warn("🚨 배치 작업이 실패했습니다. 관리자에게 알림을 발송합니다.");
            // TODO: 실제 알림 시스템 연동 (이메일, 슬랙, SMS 등)
            sendFailureAlert(summary);
        } else if (summary.getStatus() == BatchStatus.COMPLETED) {
            log.info("✅ 배치 작업이 성공적으로 완료되었습니다.");
            // TODO: 성공 알림이 필요한 경우 구현
        }
    }

    private void sendFailureAlert(JobExecutionSummary summary) {
        // 실제 환경에서는 이메일, 슬랙, 모니터링 시스템 등과 연동
        log.error("배치 실패 알림 - Job: {}, 실행 시간: {}초, 오류: {}", 
                summary.getJobName(), 
                summary.getDuration().toSeconds(),
                summary.getExitDescription());
    }

    // 내부 클래스로 요약 정보를 담는 DTO
    private static class JobExecutionSummary {
        private final String jobName;
        private final Long jobId;
        private final BatchStatus status;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private final Duration duration;
        private final Collection<StepExecution> stepExecutions;
        private final String exitCode;
        private final String exitDescription;

        private JobExecutionSummary(Builder builder) {
            this.jobName = builder.jobName;
            this.jobId = builder.jobId;
            this.status = builder.status;
            this.startTime = builder.startTime;
            this.endTime = builder.endTime;
            this.duration = builder.duration;
            this.stepExecutions = builder.stepExecutions;
            this.exitCode = builder.exitCode;
            this.exitDescription = builder.exitDescription;
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public String getJobName() { return jobName; }
        public Long getJobId() { return jobId; }
        public BatchStatus getStatus() { return status; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public Duration getDuration() { return duration; }
        public Collection<StepExecution> getStepExecutions() { return stepExecutions; }
        public String getExitCode() { return exitCode; }
        public String getExitDescription() { return exitDescription; }

        private static class Builder {
            private String jobName;
            private Long jobId;
            private BatchStatus status;
            private LocalDateTime startTime;
            private LocalDateTime endTime;
            private Duration duration;
            private Collection<StepExecution> stepExecutions;
            private String exitCode;
            private String exitDescription;

            public Builder jobName(String jobName) { this.jobName = jobName; return this; }
            public Builder jobId(Long jobId) { this.jobId = jobId; return this; }
            public Builder status(BatchStatus status) { this.status = status; return this; }
            public Builder startTime(LocalDateTime startTime) { this.startTime = startTime; return this; }
            public Builder endTime(LocalDateTime endTime) { this.endTime = endTime; return this; }
            public Builder duration(Duration duration) { this.duration = duration; return this; }
            public Builder stepExecutions(Collection<StepExecution> stepExecutions) { this.stepExecutions = stepExecutions; return this; }
            public Builder exitCode(String exitCode) { this.exitCode = exitCode; return this; }
            public Builder exitDescription(String exitDescription) { this.exitDescription = exitDescription; return this; }

            public JobExecutionSummary build() {
                return new JobExecutionSummary(this);
            }
        }
    }
}