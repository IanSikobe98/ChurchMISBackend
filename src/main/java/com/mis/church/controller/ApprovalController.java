package com.mis.church.controller;


import com.google.gson.Gson;
import com.mis.church.dto.ApiResponse;
import com.mis.church.dto.ApprovalRequest;
import com.mis.church.service.ApprovalService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/approvals")
@RequiredArgsConstructor
@Slf4j
public class ApprovalController {

    private final ApprovalService approvalService;

    @PostMapping("/approve")
    public ApiResponse approveOrReject(HttpServletResponse httpServletResponse , @RequestBody @Valid ApprovalRequest request){
        log.info("APPROVAL   REQUEST :: {}", new Gson().toJson(request));
        ApiResponse response = approvalService.approveOrReject(request,httpServletResponse);
        log.info("RESPONSE: {}", response);
        return  response;
    }
}
