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
public class BasicRoamingDataProcessor implements ItemProcessor<RoamingData, RoamingStatusEntity> {

    @Override
    public RoamingStatusEntity process(RoamingData item) throws Exception {
        log.debug("Processing item: {}", item);
        
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