package com.inventory.msp.dto;

import lombok.Data;

@Data
public class CharteredBikeLoginData {
    private int userId;
    private String firstName;
    private String emailId;
    private String token;
    private String refreshToken;
    private int cityId;
    private String userRole;
    private int tokenExpiryTime;
}
