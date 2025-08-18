package com.roaming.config;

import com.roaming.domain.RoamingData;
import com.roaming.domain.RoamingStatusEntity;
import com.roaming.job.listener.JobCompletionListener;
import com.roaming.job.listener.StepCompletionListener;
import com.roaming.job.processor.RoamingDataProcessor;
import com.roaming.job.processor.BasicRoamingDataProcessor;
import com.roaming.job.tasklet.FileArchiveTasklet;
import com.roaming.job.tasklet.CompletionNotificationTasklet;
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
import org.springframework.dao.TransientDataAccessException;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import com.roaming.job.partitioner.LineRangePartitioner;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;

import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final RoamingDataProcessor roamingDataProcessor;
    private final BasicRoamingDataProcessor basicRoamingDataProcessor;
    private final JobCompletionListener jobCompletionListener;
    private final StepCompletionListener stepCompletionListener;
    private final FileArchiveTasklet fileArchiveTasklet;
    private final CompletionNotificationTasklet completionNotificationTasklet;

    @Bean
    public Job chunkSyncJob() {
        return new JobBuilder("chunkSyncJob", jobRepository)
                .start(chunkReadAndSaveStep())
                .build();
    }

    @Bean
    public Job robustSyncJob() {
        return new JobBuilder("robustSyncJob", jobRepository)
                .listener(jobCompletionListener)
                .start(robustReadAndSaveStep())
                .build();
    }

    @Bean
    public Job partitioningSyncJob() {
        return new JobBuilder("partitioningSyncJob", jobRepository)
                .listener(jobCompletionListener)
                .start(partitionedStep())
                .build();
    }

    @Bean
    public Job fullSyncJob() {
        return new JobBuilder("fullSyncJob", jobRepository)
                .listener(jobCompletionListener)
                .start(fileArchiveStep())
                .next(partitionedStep())
                .next(completionNotificationStep())
                .build();
    }

    @Bean
    public Step fileArchiveStep() {
        return new StepBuilder("fileArchiveStep", jobRepository)
                .tasklet(fileArchiveTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step completionNotificationStep() {
        return new StepBuilder("completionNotificationStep", jobRepository)
                .tasklet(completionNotificationTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step chunkReadAndSaveStep() {
        return new StepBuilder("chunkReadAndSaveStep", jobRepository)
                .<RoamingData, RoamingStatusEntity>chunk(10, transactionManager)
                .reader(csvItemReader())
                .processor(basicRoamingDataProcessor)
                .writer(jpaItemWriter())
                .build();
    }

    @Bean
    public Step robustReadAndSaveStep() {
        return new StepBuilder("robustReadAndSaveStep", jobRepository)
                .<RoamingData, RoamingStatusEntity>chunk(10, transactionManager)
                .reader(csvItemReader())
                .processor(roamingDataProcessor)
                .writer(jpaItemWriter())
                .faultTolerant()
                .skip(IllegalArgumentException.class)
                .skipLimit(5)
                .retry(TransientDataAccessException.class)
                .retryLimit(3)
                .listener(stepCompletionListener)
                .build();
    }

    @Bean
    public Step partitionedStep() {
        return new StepBuilder("partitionedStep", jobRepository)
                .partitioner("workerStep", partitioner())
                .partitionHandler(partitionHandler())
                .build();
    }

    @Bean
    public Step workerStep() {
        return new StepBuilder("workerStep", jobRepository)
                .<RoamingData, RoamingStatusEntity>chunk(10, transactionManager)
                .reader(partitionedCsvReader(null, null))
                .processor(roamingDataProcessor)
                .writer(jpaItemWriter())
                .faultTolerant()
                .skip(IllegalArgumentException.class)
                .skipLimit(5)
                .retry(TransientDataAccessException.class)
                .retryLimit(3)
                .listener(stepCompletionListener)
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
    @StepScope
    public FlatFileItemReader<RoamingData> partitionedCsvReader(
            @Value("#{stepExecutionContext[startLine]}") Integer startLine,
            @Value("#{stepExecutionContext[endLine]}") Integer endLine) {
        
        FlatFileItemReader<RoamingData> reader = new FlatFileItemReader<>();
        reader.setName("partitionedCsvReader");
        reader.setResource(new ClassPathResource("data/roaming-data-sample.csv"));
        reader.setLineMapper(lineMapper());
        
        if (startLine != null && endLine != null) {
            reader.setLinesToSkip(startLine - 1);
            reader.setMaxItemCount(endLine - startLine + 1);
        }
        
        return reader;
    }

    private DefaultLineMapper<RoamingData> lineMapper() {
        DefaultLineMapper<RoamingData> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("userId", "deviceId", "location", "timestamp", "status");
        lineMapper.setLineTokenizer(tokenizer);
        
        BeanWrapperFieldSetMapper<RoamingData> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(RoamingData.class);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        
        return lineMapper;
    }

    @Bean
    public Partitioner partitioner() {
        Resource resource = new ClassPathResource("data/roaming-data-sample.csv");
        return new LineRangePartitioner(resource, 4);
    }

    @Bean
    public PartitionHandler partitionHandler() {
        TaskExecutorPartitionHandler partitionHandler = new TaskExecutorPartitionHandler();
        partitionHandler.setStep(workerStep());
        partitionHandler.setTaskExecutor(taskExecutor());
        partitionHandler.setGridSize(4);
        return partitionHandler;
    }

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("partition-");
        executor.initialize();
        return executor;
    }

    @Bean
    public JpaItemWriter<RoamingStatusEntity> jpaItemWriter() {
        return new JpaItemWriterBuilder<RoamingStatusEntity>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
}