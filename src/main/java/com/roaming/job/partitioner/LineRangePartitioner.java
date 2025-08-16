package com.roaming.job.partitioner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class LineRangePartitioner implements Partitioner {

    private final Resource resource;
    private final int gridSize;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> partitions = new HashMap<>();
        
        try {
            int totalLines = countLines();
            log.info("Total lines in file: {}", totalLines);
            
            if (totalLines <= 1) {
                log.warn("File has no data lines to process");
                return partitions;
            }
            
            int dataLines = totalLines - 1;
            int linesPerPartition = Math.max(1, dataLines / gridSize);
            
            log.info("Creating {} partitions with approximately {} lines each", gridSize, linesPerPartition);
            
            for (int i = 0; i < gridSize; i++) {
                ExecutionContext context = new ExecutionContext();
                
                int startLine = (i * linesPerPartition) + 2;
                
                int endLine;
                if (i == gridSize - 1) {
                    endLine = totalLines;
                } else {
                    endLine = startLine + linesPerPartition - 1;
                }
                
                if (startLine > totalLines) {
                    continue;
                }
                
                if (endLine > totalLines) {
                    endLine = totalLines;
                }
                
                context.putInt("startLine", startLine);
                context.putInt("endLine", endLine);
                context.putString("partitionNumber", String.valueOf(i));
                
                partitions.put("partition" + i, context);
                
                log.debug("Partition {}: lines {} to {} ({} lines)", 
                    i, startLine, endLine, endLine - startLine + 1);
            }
            
            log.info("Created {} partitions for processing", partitions.size());
            
        } catch (IOException e) {
            log.error("Error reading file for partitioning: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to partition file", e);
        }
        
        return partitions;
    }

    private int countLines() throws IOException {
        int lineCount = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            while (reader.readLine() != null) {
                lineCount++;
            }
        }
        return lineCount;
    }
}