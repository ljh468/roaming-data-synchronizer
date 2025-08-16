package com.roaming.job.processor;

import com.roaming.domain.RoamingData;
import com.roaming.domain.RoamingStatusEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoamingDataProcessorTest {

    private RoamingDataProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new RoamingDataProcessor();
    }

    @Test
    @DisplayName("정상적인 로밍 데이터를 성공적으로 처리해야 한다")
    void should_ProcessNormalData_when_ValidInputProvided() throws Exception {
        // Given
        RoamingData input = new RoamingData(
            1001L, 
            "DEV001", 
            "Seoul", 
            "2024-01-15T10:30:00", 
            "CONNECTED"
        );

        // When
        RoamingStatusEntity result = processor.process(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1001L);
        assertThat(result.getDeviceId()).isEqualTo("DEV001");
        assertThat(result.getLocation()).isEqualTo("Seoul");
        assertThat(result.getTimestamp()).isEqualTo(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
        assertThat(result.getStatus()).isEqualTo(RoamingStatusEntity.RoamingStatus.CONNECTED);
    }

    @Test
    @DisplayName("DEV003 디바이스에 대해 IllegalArgumentException을 발생시켜야 한다")
    void should_ThrowIllegalArgumentException_when_DEV003DeviceProvided() {
        // Given
        RoamingData input = new RoamingData(
            1003L, 
            "DEV003", 
            "Incheon", 
            "2024-01-15T10:40:00", 
            "DISCONNECTED"
        );

        // When & Then
        assertThatThrownBy(() -> processor.process(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid device ID: DEV003");
    }

    @Test
    @DisplayName("DEV007 디바이스에 대해 지연 후 정상 처리해야 한다")
    void should_ProcessWithDelay_when_DEV007DeviceProvided() throws Exception {
        // Given
        RoamingData input = new RoamingData(
            1007L, 
            "DEV007", 
            "Ulsan", 
            "2024-01-15T11:00:00", 
            "ROAMING"
        );

        // When
        long startTime = System.currentTimeMillis();
        RoamingStatusEntity result = processor.process(input);
        long endTime = System.currentTimeMillis();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDeviceId()).isEqualTo("DEV007");
        assertThat(result.getStatus()).isEqualTo(RoamingStatusEntity.RoamingStatus.ROAMING);
        // 지연이 발생했는지 확인 (최소 100ms)
        assertThat(endTime - startTime).isGreaterThanOrEqualTo(100);
    }

    @Test
    @DisplayName("다양한 로밍 상태값을 올바르게 변환해야 한다")
    void should_ConvertStatusCorrectly_when_DifferentStatusProvided() throws Exception {
        // Given
        RoamingData roamingInput = new RoamingData(1005L, "DEV005", "Gwangju", "2024-01-15T10:50:00", "ROAMING");
        RoamingData disconnectedInput = new RoamingData(1008L, "DEV008", "Sejong", "2024-01-15T11:05:00", "DISCONNECTED");

        // When
        RoamingStatusEntity roamingResult = processor.process(roamingInput);
        RoamingStatusEntity disconnectedResult = processor.process(disconnectedInput);

        // Then
        assertThat(roamingResult.getStatus()).isEqualTo(RoamingStatusEntity.RoamingStatus.ROAMING);
        assertThat(disconnectedResult.getStatus()).isEqualTo(RoamingStatusEntity.RoamingStatus.DISCONNECTED);
    }
}