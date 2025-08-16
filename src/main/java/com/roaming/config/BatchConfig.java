package com.roaming.config;

import com.roaming.domain.RoamingData;
import com.roaming.domain.RoamingStatusEntity;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job chunkSyncJob() {
        return new JobBuilder("chunkSyncJob", jobRepository)
                .start(chunkReadAndSaveStep())
                .build();
    }

    @Bean
    public Step chunkReadAndSaveStep() {
        return new StepBuilder("chunkReadAndSaveStep", jobRepository)
                .<RoamingData, RoamingStatusEntity>chunk(10, transactionManager)
                .reader(csvItemReader())
                .processor(roamingDataProcessor())
                .writer(jpaItemWriter())
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<RoamingData> csvItemReader() {
        return new FlatFileItemReaderBuilder<RoamingData>()
                .name("csvItemReader")
                .resource(new ClassPathResource("data/roaming-data-sample.csv"))
                .delimited()
                .names("userId", "deviceId", "location", "timestamp", "status")
                .linesToSkip(1)
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(RoamingData.class);
                }})
                .build();
    }

    @Bean
    public ItemProcessor<RoamingData, RoamingStatusEntity> roamingDataProcessor() {
        return item -> {
            log.debug("Processing item: {}", item);
            
            LocalDateTime parsedTimestamp = LocalDateTime.parse(
                item.getTimestamp(), 
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
            );
            
            return RoamingStatusEntity.builder()
                    .userId(item.getUserId())
                    .deviceId(item.getDeviceId())
                    .location(item.getLocation())
                    .timestamp(parsedTimestamp)
                    .status(RoamingStatusEntity.RoamingStatus.valueOf(item.getStatus()))
                    .build();
        };
    }

    @Bean
    public JpaItemWriter<RoamingStatusEntity> jpaItemWriter() {
        return new JpaItemWriterBuilder<RoamingStatusEntity>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
}