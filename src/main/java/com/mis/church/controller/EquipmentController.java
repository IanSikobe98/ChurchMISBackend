package com.mis.church.controller;

import com.google.gson.Gson;
import com.mis.church.dto.*;
import com.mis.church.service.EquipmentService;
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
@RequestMapping("/api/v1/equipment")
@RequiredArgsConstructor
@Slf4j
public class EquipmentController {
    private final EquipmentService equipmentService;

    @PostMapping(value = "/createRequest", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse createRequest(@Valid @RequestBody EquipmentRequest request, HttpServletResponse httpServletResponse){
        log.info("OTP REQUEST :: {}", new Gson().toJson(request));
        ApiResponse response = equipmentService.createEquipmentRequest(httpServletResponse,request);
        log.info("RESPONSE: {}", response);
        return  response;
    }

    @PostMapping("/getEquipmentRequests")
    public ApiResponse fetchEquipmentRequests(HttpServletResponse httpServletResponse , @RequestBody @Valid ReportRequest request){
        log.info("GET fetch Equipment Requests REQUEST :: {}", new Gson().toJson(request));
        ReportResponse response = equipmentService.getEquipmentRequests(request ,httpServletResponse);
        log.info("RESPONSE: {}", response);
        return  response;
    }
}
