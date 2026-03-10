package com.mis.church.controller;

import com.google.gson.Gson;

import com.mis.church.dto.AuthRequest;
import com.mis.church.dto.AuthResponse;
import com.mis.church.dto.OtpRequest;
import com.mis.church.dto.OtpResponse;
import com.mis.church.service.AuthenticationService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @PostMapping(value = "/authenticate", produces = MediaType.APPLICATION_JSON_VALUE)
    public AuthResponse authenticate(@Valid @RequestBody AuthRequest authRequest, HttpServletResponse httpServletResponse){
        log.info("AUTHENTICATION REQUEST :: {}", new Gson().toJson(authRequest));
        AuthResponse authResponse = authenticationService.authenticate(authRequest, httpServletResponse);
        log.info("RESPONSE: {}", authResponse);
        return  authResponse;
    }

    @PostMapping(value = "/validateOtp", produces = MediaType.APPLICATION_JSON_VALUE)
    public OtpResponse validateOtp(@Valid @RequestBody OtpRequest otpRequest, HttpServletResponse httpServletResponse){
        log.info("OTP REQUEST :: {}", new Gson().toJson(otpRequest));
        OtpResponse otpResponse = authenticationService.validateOtp(otpRequest, httpServletResponse);
        log.info("RESPONSE: {}", otpResponse);
        return  otpResponse;
    }



}
