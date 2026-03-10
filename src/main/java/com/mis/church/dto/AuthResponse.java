package com.mis.church.dto;


import com.mis.church.enums.ApiResponseCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private ApiResponseCode responseCode;
    private String responseMessage;
    private String otpExpiry;
}
