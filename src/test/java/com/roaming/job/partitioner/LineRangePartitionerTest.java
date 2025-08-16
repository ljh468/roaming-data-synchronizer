package com.roaming.job.partitioner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LineRangePartitionerTest {

    @Mock
    private Resource resource;

    private LineRangePartitioner partitioner;

    @BeforeEach
    void setUp() {
        partitioner = new LineRangePartitioner(resource, 2);
    }

    @Test
    @DisplayName("정상적인 CSV 파일에 대해 파티션을 생성해야 한다")
    void should_CreatePartitions_when_ValidCsvFile() throws IOException {
        // Given
        String csvContent = "userId,deviceId,location,timestamp,status\n" +
                           "USER001,DEV001,Seoul,2023-10-01T10:00:00,CONNECTED\n" +
                           "USER002,DEV002,Busan,2023-10-01T11:00:00,DISCONNECTED\n" +
                           "USER003,DEV003,Incheon,2023-10-01T12:00:00,ROAMING\n" +
                           "USER004,DEV004,Daegu,2023-10-01T13:00:00,CONNECTED\n";
        
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        when(resource.getInputStream()).thenReturn(inputStream);

        // When
        Map<String, ExecutionContext> partitions = partitioner.partition(2);

        // Then
        assertThat(partitions).hasSize(2);
        
        ExecutionContext partition0 = partitions.get("partition0");
        assertThat(partition0.getInt("startLine")).isEqualTo(2);
        assertThat(partition0.getInt("endLine")).isEqualTo(3);
        assertThat(partition0.getString("partitionNumber")).isEqualTo("0");
        
        ExecutionContext partition1 = partitions.get("partition1");
        assertThat(partition1.getInt("startLine")).isEqualTo(4);
        assertThat(partition1.getInt("endLine")).isEqualTo(5);
        assertThat(partition1.getString("partitionNumber")).isEqualTo("1");
    }

    @Test
    @DisplayName("빈 파일에 대해 빈 파티션 맵을 반환해야 한다")
    void should_ReturnEmptyPartitions_when_EmptyFile() throws IOException {
        // Given
        String csvContent = "";
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        when(resource.getInputStream()).thenReturn(inputStream);

        // When
        Map<String, ExecutionContext> partitions = partitioner.partition(2);

        // Then
        assertThat(partitions).isEmpty();
    }

    @Test
    @DisplayName("헤더만 있는 파일에 대해 빈 파티션 맵을 반환해야 한다")
    void should_ReturnEmptyPartitions_when_OnlyHeaderFile() throws IOException {
        // Given
        String csvContent = "userId,deviceId,location,timestamp,status\n";
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        when(resource.getInputStream()).thenReturn(inputStream);

        // When
        Map<String, ExecutionContext> partitions = partitioner.partition(2);

        // Then
        assertThat(partitions).isEmpty();
    }

    @Test
    @DisplayName("IOException 발생 시 RuntimeException을 던져야 한다")
    void should_ThrowRuntimeException_when_IOExceptionOccurs() throws IOException {
        // Given
        when(resource.getInputStream()).thenThrow(new IOException("File read error"));

        // When & Then
        assertThatThrownBy(() -> partitioner.partition(2))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to partition file")
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("단일 데이터 라인에 대해 하나의 파티션을 생성해야 한다")
    void should_CreateSinglePartition_when_SingleDataLine() throws IOException {
        // Given
        String csvContent = "userId,deviceId,location,timestamp,status\n" +
                           "USER001,DEV001,Seoul,2023-10-01T10:00:00,CONNECTED\n";
        
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        when(resource.getInputStream()).thenReturn(inputStream);

        // When
        Map<String, ExecutionContext> partitions = partitioner.partition(4);

        // Then
        assertThat(partitions).hasSize(1);
        
        ExecutionContext partition0 = partitions.get("partition0");
        assertThat(partition0.getInt("startLine")).isEqualTo(2);
        assertThat(partition0.getInt("endLine")).isEqualTo(2);
        assertThat(partition0.getString("partitionNumber")).isEqualTo("0");
    }
}