package com.roaming.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoamingData {
    private Long userId;
    private String deviceId;
    private String location;
    private String timestamp;
    private String status;
}