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
public class ApiResponse {

    private ApiResponseCode responseCode;
    private String responseMessage;
    private Object entity;
    private String token;
    private String error;
    private String message;
}
