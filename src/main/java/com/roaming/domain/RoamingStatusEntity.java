package com.roaming.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "roaming_status")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoamingStatusEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "device_id", nullable = false, length = 50)
    private String deviceId;
    
    @Column(name = "location", length = 100)
    private String location;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RoamingStatus status;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public enum RoamingStatus {
        CONNECTED, DISCONNECTED, ROAMING
    }
}