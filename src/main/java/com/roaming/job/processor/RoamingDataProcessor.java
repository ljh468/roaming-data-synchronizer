package com.roaming.job.processor;

import com.roaming.domain.RoamingData;
import com.roaming.domain.RoamingStatusEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class RoamingDataProcessor implements ItemProcessor<RoamingData, RoamingStatusEntity> {

    @Override
    public RoamingStatusEntity process(RoamingData item) throws Exception {
        log.debug("Processing item: {}", item);
        
        // 의도적 예외 발생 조건 - 특정 deviceId에 대해 예외 발생
        if (item.getDeviceId() != null && item.getDeviceId().contains("DEV003")) {
            log.warn("Invalid device detected: {}", item.getDeviceId());
            throw new IllegalArgumentException("Invalid device ID: " + item.getDeviceId());
        }
        
        // 일부 데이터에 대해 처리 지연 시뮬레이션 (네트워크 지연 등)
        if (item.getDeviceId() != null && item.getDeviceId().contains("DEV007")) {
            log.debug("Simulating processing delay for device: {}", item.getDeviceId());
            Thread.sleep(100); // 짧은 지연
        }
        
        LocalDateTime parsedTimestamp = LocalDateTime.parse(
            item.getTimestamp(), 
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
        );
        
        RoamingStatusEntity entity = RoamingStatusEntity.builder()
                .userId(item.getUserId())
                .deviceId(item.getDeviceId())
                .location(item.getLocation())
                .timestamp(parsedTimestamp)
                .status(RoamingStatusEntity.RoamingStatus.valueOf(item.getStatus()))
                .build();
        
        log.debug("Successfully processed item for device: {}", item.getDeviceId());
        return entity;
    }
}