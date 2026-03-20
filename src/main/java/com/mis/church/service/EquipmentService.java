package com.mis.church.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mis.church.config.security.SecurityUser;
import com.mis.church.dto.*;
import com.mis.church.entity.*;
import com.mis.church.entity.views.EquipmentConditionSummary;
import com.mis.church.enums.ApiResponseCode;
import com.mis.church.repository.*;
import com.mis.church.util.CommonTasks;
import com.mis.church.util.ConstantUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.mis.church.enums.Action.APPROVE;
import static com.mis.church.enums.Action.REJECT;
import static com.mis.church.util.CommonTasks.generateRequestId;

@Service
@RequiredArgsConstructor
@Slf4j
public class EquipmentService {
    private final EquipmentItemRepo equipmentItemRepo;
    private final ConstantUtil constantUtil;
    private final RequestRepo requestRepo;
    private final WorkflowStepRepo workflowStepRepo;
    private final RequestApprovalRepo requestApprovalRepo;
    private final EquipmentRepo equipmentRepo;
    private final CommonTasks commonTasks;
    private final EquipmentAllocationRepo equipmentAllocationRepo;
    private final EquipmentConditionSummaryRepo equipmentConditionSummaryRepo;

    @Value("${params.admin_role}")
    private String adminRole;

    /**
     * Function to get the Authenticated user that was authenticated using JWT
     *
     * @return ApiUser: The authenticated user
     */
    private User getauthenticatedAPIUser() {
        return ((SecurityUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUser();
    }


    //TODO FINALIZE ON RETURN DATE
    public ApiResponse createEquipmentRequest(HttpServletResponse httpServletResponse, EquipmentRequest request) {
       ApiResponse response = new ApiResponse();
        try{
            log.info("Creating equipment using request: {}", request);
            User user = getauthenticatedAPIUser();


            //check for existing request
            List<Request> existingPendingRequest = requestRepo.findByCreatedByAndStatus(user.getUsername(),constantUtil.PENDING_APPROVAL);
            if(!existingPendingRequest.isEmpty()){
                log.info("Their is an existing pending request for  Id {}", request.getEquipmentId());
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("You have existing request awaiting approval");
                return response;
            }

            //check if equipment exists for that quantity
            List<EquipmentItem> equipmentItems =  equipmentItemRepo.findByEquipment_IdAndAvailabilityStatus(request.getEquipmentId(),constantUtil.AVAILABLE);
            if(equipmentItems.isEmpty()){
                log.info("The selected equipment is not available at the moment for Id {}", request.getEquipmentId());
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("The selected equipment is not available at the moment.Please try again later");
                return response;
            }
            if(equipmentItems.size()<request.getQuantity()){
                log.info("At the moment only {} {}s are available for equipment Id {}", request.getQuantity(), equipmentItems.getFirst().getEquipment().getName(),request.getEquipmentId());
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage(String.format("At the moment only  %s %ss are available", request.getQuantity(), equipmentItems.getFirst().getEquipment().getName()));
                return response;
            }


            //TODO --> CHECK FOR QUALITY OF ITEMS

            //CHECK IF ALL WORKFLOW STEPS HAVE BEEN CREATED
            Equipment equipment = equipmentItems.getFirst().getEquipment();
            ApprovalWorkflow approvalWorkflow = equipment.getWorkflow();

            if(approvalWorkflow==null){
                log.info("The selected equipment has not fully configured approval workflows for equipment Id {}", request.getEquipmentId());
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("The selected equipment has not fully configured approval workflows.Kindly contact Admin");
                return response;
            }
            List<WorkflowStep> workflowSteps = workflowStepRepo.findByWorkflow(approvalWorkflow);

            if(workflowSteps.isEmpty()){
                log.info("The selected equipment has not fully configured approval workflows for equipment Id {}", request.getEquipmentId());
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("The selected equipment has not fully configured approval workflows.Kindly contact Admin");
                return response;
            }

            String returnDateStr = request.getReturnDate(); // e.g., "2026-03-20"
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate returnDate = LocalDate.parse(returnDateStr, formatter);
            Date legacyDate = Date.from(returnDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

            //Create the equipment request
            Request equipmentRequest = Request.builder()
                    .trxId(generateRequestId(request.getEquipmentId()))
                    .event(request.getEvent())
                    .purpose(request.getPurpose())
                    .venue(request.getVenue())
                    .workflow(approvalWorkflow)
                    .equipment(equipment)
                    .quantity(request.getQuantity())
                    .currentApprovalLevel(0)
                    .returnDate(legacyDate)
                    .status(constantUtil.PENDING_APPROVAL)
                    .createdBy(user.getUsername())
                    .updatedBy(user.getUsername())
                    .build();


            requestRepo.saveAndFlush(equipmentRequest);
            log.info("Request successfully requested id:{}",equipmentRequest.getId());


            List<RequestApproval> requestApprovals = new ArrayList<>();
            workflowSteps.forEach(workflowStep -> {
                RequestApproval requestApproval = RequestApproval.builder()
                        .request(equipmentRequest)
                        .approverRole(workflowStep.getRoleId())
                        .stepLevel(workflowStep.getStepLevel())
                        .isAllocater(workflowStep.getIsAllocater())
                        .status(constantUtil.PENDING_APPROVAL)
                        .createdAt(new Date())
                        .build();

                requestApprovals.add(requestApproval);
            });

            requestApprovalRepo.saveAll(requestApprovals);
            log.info("Workflow approval steps successfully requested id:{}",equipmentRequest.getId());
            response.setResponseCode(ApiResponseCode.SUCCESS);
            response.setResponseMessage("Equipment request successfully created");

        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING CREATION OF EQUIPMENT REQUEST :: {}" ,e.getMessage());
            e.printStackTrace();
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry, an error occurred while creating equipment request! Please Try again later");
        }
        return response;
    }

//    public ApiResponse readEquipmentRequest(HttpServletResponse httpServletResponse, EquipmentRequest request) {
//        ApiResponse response = new ApiResponse();
//        try{
//            List<Request> equipmentRequests = equipmentItemRepo.findByEquipment_IdAndAvailabilityStatus();
//        }
//
//        catch (Exception e){
//            log.error("ERROR OCCURRED DURING LISTING OF EQUIPMENT REQUEST :: {}" ,e.getMessage());
//            e.printStackTrace();
//            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
//            response.setResponseCode(ApiResponseCode.FAIL);
//            response.setResponseMessage("Sorry, an error occurred while reading equipment requests! Please Try again later");
//        }
//        return response;
//    }

    public ReportResponse getEquipmentInformation(ReportRequest request, HttpServletResponse httpServletResponse){
        ReportResponse response = new ReportResponse();
        List<Equipment> equipmentList = new ArrayList<>();
        List<EquipmentInfo> equipmentInfoList = new ArrayList<>();
        int page = request.getPage();
        int size = request.getSize();
        PageRequest pageable = null;

        try{
            User loggedInUser = getauthenticatedAPIUser();


            equipmentList = equipmentRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

            if(equipmentList.isEmpty()){
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("No equipment found");
                log.info("No equipment found");
                return response;
            }

            equipmentList.forEach(equipment -> {
                EquipmentInfo equipmentInfo = new EquipmentInfo();
                List<EquipmentItem> equipmentItemList = equipmentItemRepo.findByEquipment_IdAndAvailabilityStatus(equipment.getId(),constantUtil.AVAILABLE);
                if(!equipmentItemList.isEmpty()) {
                    equipmentInfo.setEquipment(equipment);
                    equipmentInfo.setQuantity(equipmentItemList.size());
                    equipmentInfoList.add(equipmentInfo);
                }
            });


            response.setResponseCode(ApiResponseCode.SUCCESS);
            response.setResponseMessage("Equipment Information successfully fetched");

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            response.setData(mapper.readValue(mapper.writeValueAsString(equipmentInfoList), ArrayList.class));
            return response;


        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING EQUIPMENT REQUESTS DATA FETCH:: {}" ,e.getMessage());
            e.printStackTrace();
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred while fetching the equipment requests");
        }
        return response;

    }


    public ReportResponse getEquipmentRequests(ReportRequest request, HttpServletResponse httpServletResponse){
        ReportResponse response = new ReportResponse();
        List<Request> equipmentRequestsList = new ArrayList<>();
        int page = request.getPage();
        int size = request.getSize();
        PageRequest pageable = null;

        try{
            User loggedInUser = getauthenticatedAPIUser();

            if (request.getStatuses() != null  && !request.getStatuses().isEmpty()) {
                equipmentRequestsList = requestRepo.findByStatus_StatusIdInOrderByCreatedAtDesc(request.getStatuses());
            } else {
                equipmentRequestsList = requestRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
            }


            response.setResponseCode(ApiResponseCode.SUCCESS);
            response.setResponseMessage("Equipment Requests successfully fetched");

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            response.setData(mapper.readValue(mapper.writeValueAsString(equipmentRequestsList), ArrayList.class));
            return response;


        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING EQUIPMENT REQUESTS DATA FETCH:: {}" ,e.getMessage());
            e.printStackTrace();
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred while fetching the equipment requests");
        }
        return response;

    }

    public ReportResponse getEquipmentConditionReport(ReportRequest request, HttpServletResponse httpServletResponse){
        ReportResponse response = new ReportResponse();
        List<EquipmentConditionSummary> equipmentConditionSummaries = new ArrayList<>();
        int page = request.getPage();
        int size = request.getSize();
        PageRequest pageable = null;

        try{
            User loggedInUser = getauthenticatedAPIUser();

            if (request.getStatuses() != null  && !request.getStatuses().isEmpty()) {
                equipmentConditionSummaries = equipmentConditionSummaryRepo.findByEquipmentIdAndStatusIdIn(Long.valueOf(request.getId()),request.getStatuses());
            } else {
                equipmentConditionSummaries = equipmentConditionSummaryRepo.findByEquipmentId(Long.valueOf(request.getId()));
            }


            response.setResponseCode(ApiResponseCode.SUCCESS);
            response.setResponseMessage("Equipment Condition Report successfully fetched");

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            response.setData(mapper.readValue(mapper.writeValueAsString(equipmentConditionSummaries), ArrayList.class));
            return response;


        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING EQUIPMENT REPORTS DATA FETCH:: {}" ,e.getMessage());
            e.printStackTrace();
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred while fetching the equipment condition report");
        }
        return response;

    }


    public ReportResponse getRequestApprovalsByRole(ReportRequest request, HttpServletResponse httpServletResponse){
        ReportResponse response = new ReportResponse();
        List<RequestApproval> requestApprovals = new ArrayList<>();
        int page = request.getPage();
        int size = request.getSize();
        PageRequest pageable = null;

        try{
            User loggedInUser = getauthenticatedAPIUser();

            if(loggedInUser.getRole().equals(adminRole)) {
                if (request.getStatuses() != null && !request.getStatuses().isEmpty()) {
                    requestApprovals = requestApprovalRepo.findByStatus_statusIdInOrderByCreatedAtDesc(request.getStatuses());
                } else {
                    requestApprovals = requestApprovalRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
                }

            }
            else {
                if (request.getStatuses() != null && !request.getStatuses().isEmpty()) {
                    requestApprovals = requestApprovalRepo.findByApproverRoleAndStatus_statusIdInOrderByCreatedAtDesc(loggedInUser.getRole(),request.getStatuses());
                } else {
                    requestApprovals = requestApprovalRepo.findByApproverRoleOrderByCreatedAtDesc(loggedInUser.getRole());
                }

            }
            List<RequestApproval> filteredRequestApprovals = new ArrayList<>();
            requestApprovals.forEach(requestApproval -> {
                //note 0 - 1st approval
                //note 1 - 2nd approval
                //note 2 - 3rd approval
                Integer approvalLevel = requestApproval.getStepLevel()-1;
                if(Objects.equals(approvalLevel, requestApproval.getRequest().getCurrentApprovalLevel())){
                    filteredRequestApprovals.add(requestApproval);
                }
            });
            response.setResponseCode(ApiResponseCode.SUCCESS);
            response.setResponseMessage("Request Approvals successfully fetched");

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            response.setData(mapper.readValue(mapper.writeValueAsString(filteredRequestApprovals), ArrayList.class));
            return response;


        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING REQUEST APPROVALS DATA FETCH:: {}" ,e.getMessage());
            e.printStackTrace();
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred while fetching the request approvals");
        }
        return response;

    }

    public ReportResponse getRequestApprovalsByRequest(ReportRequest request, HttpServletResponse httpServletResponse){
        ReportResponse response = new ReportResponse();
        List<RequestApproval> requestApprovals = new ArrayList<>();
        int page = request.getPage();
        int size = request.getSize();
        PageRequest pageable = null;

        try{
            User loggedInUser = getauthenticatedAPIUser();

            requestApprovals = requestApprovalRepo.findByRequest_IdOrderByStepLevelAsc(Long.valueOf(request.getId()));

            response.setResponseCode(ApiResponseCode.SUCCESS);
            response.setResponseMessage("Request Approvals successfully fetched");

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            response.setData(mapper.readValue(mapper.writeValueAsString(requestApprovals), ArrayList.class));
            return response;


        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING REQUEST APPROVALS DATA FETCH:: {}" ,e.getMessage());
            e.printStackTrace();
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred while fetching the request approvals");
        }
        return response;

    }


    public ReportResponse getEquipmentByRequest(ReportRequest request, HttpServletResponse httpServletResponse){
        ReportResponse response = new ReportResponse();
        List<EquipmentAllocation> equipmentAllocated = new ArrayList<>();
        int page = request.getPage();
        int size = request.getSize();
        PageRequest pageable = null;

        try{
            User loggedInUser = getauthenticatedAPIUser();

            equipmentAllocated = equipmentAllocationRepo.findByRequest_Id(Long.valueOf(request.getId()));

            response.setResponseCode(ApiResponseCode.SUCCESS);
            response.setResponseMessage("Equipment fetched successfully fetched");

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            response.setData(mapper.readValue(mapper.writeValueAsString(equipmentAllocated), ArrayList.class));
            return response;


        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING REQUEST EQUIPMENT DATA FETCH:: {}" ,e.getMessage());
            e.printStackTrace();
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred while fetching the equipment");
        }
        return response;

    }


    public ReportResponse getRequestApprovals(ReportRequest request, HttpServletResponse httpServletResponse){
        ReportResponse response = new ReportResponse();
        List<Request> equipmentRequestsList = new ArrayList<>();
        int page = request.getPage();
        int size = request.getSize();
        PageRequest pageable = null;

        try{
            User loggedInUser = getauthenticatedAPIUser();

            if (request.getStatuses() != null  && !request.getStatuses().isEmpty()) {
                equipmentRequestsList = requestRepo.findByStatus_StatusIdInOrderByCreatedAtDesc(request.getStatuses());
            } else {
                equipmentRequestsList = requestRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
            }


            response.setResponseCode(ApiResponseCode.SUCCESS);
            response.setResponseMessage("Equipment Requests successfully fetched");

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            response.setData(mapper.readValue(mapper.writeValueAsString(equipmentRequestsList), ArrayList.class));
            return response;


        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING EQUIPMENT REQUESTS DATA FETCH:: {}" ,e.getMessage());
            e.printStackTrace();
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred while fetching the equipment requests");
        }
        return response;

    }



    public ApiResponse approveOrRejectEquipmentRequests(ApprovalRequest request, User loggedInUser, Integer id){
        ApiResponse response = new ApiResponse();
        log.info("Approving user of id {}...",id);

        try {
            Optional<RequestApproval> existingRequestApprovalsOptional = requestApprovalRepo.findByIdAndStatus(Long.valueOf(id),constantUtil.PENDING_APPROVAL);
            if (existingRequestApprovalsOptional.isEmpty()) {
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("Request  with id  "+ id+ " does not exist");
                return response;
            }


            RequestApproval existingRequestApproval = existingRequestApprovalsOptional.get();

            Role userRole = loggedInUser.getRole();


            if(!existingRequestApproval.getApproverRole().equals(userRole)){
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("You are not allowed to approve this request");
                return response;
            }


            //TODO ADD CHECK TO SEE IF IT IS PARALLEL IT SHOULD NOT APPROVE RECORD AT LEVEL 2 BEFORE RECORD AT LEVEL 1 IS APPROVED
            Integer approvalLevel = existingRequestApproval.getStepLevel()-1;
            if(!Objects.equals(approvalLevel, existingRequestApproval.getRequest().getCurrentApprovalLevel())){
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("You are not allowed to approve this request at this level");
                return response;
            }

            if(request.getAction().equals(APPROVE.getValue())){
                if(existingRequestApproval.getIsAllocater() && request.getApprovalEquipment()!=null && !request.getApprovalEquipment().isEmpty()){
                    List<ApprovalEquipment> approvalEquipmentList = request.getApprovalEquipment();
                    approvalEquipmentList.forEach(approvalEquipment -> {
                        if(approvalEquipment.getQuantity()>=1) {
                            Optional<Equipment> equipmentOptional = equipmentRepo.findById(approvalEquipment.getEquipmentId());
                            //TODO CHECK IF EQUIPMENT DOES NOT EXIST DO WE FAIL THE WHOLE REQUEST??
                            if (equipmentOptional.isPresent()) {
                                Equipment equipment = equipmentOptional.get();

                                //fetch first  accounts
                                Pageable pageable = PageRequest.of(0, approvalEquipment.getQuantity());
                                List<EquipmentItem> equipmentItems = equipmentItemRepo.findByEquipment_IdAndAvailabilityStatusAndConditionStatus_StatusId(equipment.getId(), constantUtil.AVAILABLE, approvalEquipment.getStatusId(), pageable);
                                List<EquipmentAllocation> equipmentAllocations = new ArrayList<>();
                                equipmentItems.forEach(equipmentItem -> {
                                    EquipmentAllocation equipmentAllocation = EquipmentAllocation.builder()
                                            .request(existingRequestApproval.getRequest())
                                            .equipmentItem(equipmentItem)
                                            .conditionBefore(commonTasks.getStatus(approvalEquipment.getStatusId()))
                                            .status(constantUtil.PENDING_APPROVAL)
                                            .createdBy(loggedInUser.getUsername())
                                            .updatedBy(loggedInUser.getUsername())
                                            .build();
                                    equipmentAllocations.add(equipmentAllocation);
                                });
                                equipmentAllocationRepo.saveAll(equipmentAllocations);
                                log.info("Equipment allocations created successfully for equipment {}", equipment.getName());
                            }
                        }
                    });
                }



                existingRequestApproval.setStatus(constantUtil.ACTIVE);
                existingRequestApproval.setActionBy(loggedInUser);
                existingRequestApproval.setActionAt(new  Date());
                existingRequestApproval.setComments(request.getDescription());
                requestApprovalRepo.save(existingRequestApproval);

                //Added to increase the current approval level
                Request  pendingRequest= existingRequestApproval.getRequest();

                pendingRequest.setCurrentApprovalLevel(pendingRequest.getCurrentApprovalLevel()+1);
                requestRepo.save(pendingRequest);
                log.info("update Approval Level");



                log.info("request Approval successfully approved {}",existingRequestApproval.getRequest());



                //check if there are remaining approvals
                List<RequestApproval> requestApprovals = requestApprovalRepo.findByRequestAndStatusAndIdNot(existingRequestApproval.getRequest()
                        ,constantUtil.PENDING_APPROVAL,existingRequestApproval.getId());



                //This means all parties have approved
                if(requestApprovals.isEmpty()){
                    Request existingRequest = existingRequestApproval.getRequest();
                    List<EquipmentAllocation> equipmentAllocations =  equipmentAllocationRepo.findByRequestAndStatus(existingRequest,constantUtil.PENDING_APPROVAL);

                    if(!equipmentAllocations.isEmpty()){
                        equipmentAllocations.stream().forEach(equipmentAllocation -> {
                           equipmentAllocation.setStatus(constantUtil.ALLOCATED);
                           equipmentAllocation.setAllocatedAt(new  Date());
                           equipmentAllocation.setUpdatedBy(loggedInUser.getUsername());
                           equipmentAllocationRepo.save(equipmentAllocation);

                           EquipmentItem equipmentItem = equipmentAllocation.getEquipmentItem();
                           equipmentItem.setAvailabilityStatus(constantUtil.ALLOCATED);
                           equipmentItem.setUpdatedBy(loggedInUser.getUsername());
                           equipmentItemRepo.save(equipmentItem);


                        });
                        log.info("Equipment successfully allocated");
                    }


                    existingRequest.setStatus(constantUtil.ACTIVE);
                    requestRepo.save(existingRequest);
                    log.info("Equipment request fully  successfully approved {}",existingRequestApproval.getRequest());
                }


                response.setResponseMessage("Request successfully Approved.");
                response.setResponseCode(ApiResponseCode.SUCCESS);

            }
            else if(request.getAction().equals(REJECT.getValue())){
                existingRequestApproval.setActionBy(loggedInUser);
                existingRequestApproval.setActionAt(new  Date());
                existingRequestApproval.setComments(request.getDescription());
                existingRequestApproval.setStatus(constantUtil.REJECTED);
                requestApprovalRepo.save(existingRequestApproval);
                log.info("request Approval successfully Rejected {}",existingRequestApproval.getRequest());

                //check if there are remaining approvals
                List<RequestApproval> requestApprovals = requestApprovalRepo.findByRequestAndStatusAndIdNot(existingRequestApproval.getRequest()
                        ,constantUtil.PENDING_APPROVAL,existingRequestApproval.getId());

                requestApprovals.stream().forEach(requestApproval -> {
                    requestApproval.setStatus(constantUtil.REJECTED);
                    requestApproval.setActionBy(loggedInUser);
                    requestApproval.setActionAt(new  Date());
                    requestApproval.setComments(request.getDescription());
                    requestApprovalRepo.save(requestApproval);
                });


                log.info("Equipment Request {} successfully  rejected",existingRequestApproval.getId());
                response.setResponseMessage("Equipment Request record successfully Rejected.");
                response.setResponseCode(ApiResponseCode.SUCCESS);
            }
            else{
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("approval action is invalid");
                return response;
            }
        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING APPROVAL OF ORDER: {}" ,e.getMessage());
            e.printStackTrace();
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred during approval of order");
        }
        return response;
    }


}
