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

    @PostMapping("/getEquipmentConditionReport")
    public ApiResponse getEquipmentConditionReport(HttpServletResponse httpServletResponse , @RequestBody @Valid ReportRequest request){
        log.info("GET fetch Equipment Condition Reports Requests REQUEST :: {}", new Gson().toJson(request));
        ReportResponse response = equipmentService.getEquipmentConditionReport(request ,httpServletResponse);
        log.info("RESPONSE: {}", response);
        return  response;
    }

    @PostMapping("/getEquipmentInfo")
    public ApiResponse fetchEquipmentInfo(HttpServletResponse httpServletResponse , @RequestBody @Valid ReportRequest request){
        log.info("GET fetch Equipment Info REQUEST :: {}", new Gson().toJson(request));
        ReportResponse response = equipmentService.getEquipmentInformation(request ,httpServletResponse);
        log.info("RESPONSE: {}", response);
        return  response;
    }

    @PostMapping("/getRequestApprovalsByRole")
    public ApiResponse getRequestApprovalsByRole(HttpServletResponse httpServletResponse , @RequestBody @Valid ReportRequest request){
        log.info("GET fetch Request Approvals By Role Requests REQUEST :: {}", new Gson().toJson(request));
        ReportResponse response = equipmentService.getRequestApprovalsByRole(request ,httpServletResponse);
        log.info("RESPONSE: {}", response);
        return  response;
    }

    @PostMapping("/getRequestApprovalsByRequest")
    public ApiResponse getRequestApprovalsByRequest(HttpServletResponse httpServletResponse , @RequestBody @Valid ReportRequest request){
        log.info("GET fetch Request Approvals By Request Requests REQUEST :: {}", new Gson().toJson(request));
        ReportResponse response = equipmentService.getRequestApprovalsByRequest(request ,httpServletResponse);
        log.info("RESPONSE: {}", response);
        return  response;
    }

    @PostMapping("/getEquipmentByRequest")
    public ApiResponse f(HttpServletResponse httpServletResponse , @RequestBody @Valid ReportRequest request){
        log.info("GET fetch Equipment By Request Requests REQUEST :: {}", new Gson().toJson(request));
        ReportResponse response = equipmentService.getEquipmentByRequest(request ,httpServletResponse);
        log.info("RESPONSE: {}", response);
        return  response;
    }

}
