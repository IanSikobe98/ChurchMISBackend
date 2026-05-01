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
import java.util.stream.Collectors;

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
    private final ReturnRequestRepo returnRequestRepo;
    private final RequestEquipmentRepo requestEquipmentRepo;
    private final DptRoleMapRepo dptRoleMapRepo;

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
                log.info("Their is an existing pending request for  Ids {}", request.getEquipmentRequestDTOList());
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("You have existing request awaiting approval");
                return response;
            }

            List<EquipmentRequestDTO> equipmentRequests = request.getEquipmentRequestDTOList();

            List<RequestEquipment> requestEquipments = new ArrayList<>();
            List<RequestApproval> requestApprovals = new ArrayList<>();
            HashMap<String,String> errors = new HashMap<>();
            HashMap<Long,List<EquipmentItem>> equipmentItemsMap = new HashMap<>();
            String error = "";

            ApprovalWorkflow approvalWorkflow = constantUtil.EQUIPMENT_WORKFLOW;

            if (approvalWorkflow == null) {
                log.info("The selected equipment has not fully configured approval workflows ");
                error = "The selected equipment has not fully configured approval workflows.Kindly contact Admin";
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage(error);
                return response;
            }

            //TODO --> CHECK FOR QUALITY OF ITEMS

            //CHECK IF ALL WORKFLOW STEPS HAVE BEEN CREATED

            List<WorkflowStep> workflowSteps = workflowStepRepo.findByWorkflow(approvalWorkflow);

            if (workflowSteps.isEmpty()) {
                log.info("The selected equipment has not fully configured approval workflows for equipment ");
                error ="The selected equipment has not fully configured approval workflows.Kindly contact Admin";
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage(error);
                return response;
            }



            //Validations for equipment
            equipmentRequests.forEach(equipmentRequest -> {
                //check if equipment exists for that quantity
                List<Status> statuses = Arrays.asList(constantUtil.EXCELLENT,constantUtil.GOOD,constantUtil.FAIR);
                List<EquipmentItem> equipmentItems = equipmentItemRepo.findByEquipment_IdAndAvailabilityStatusAndConditionStatusIn(equipmentRequest.getEquipmentId(), constantUtil.AVAILABLE,statuses);
                String errorMessage = "";
                if (equipmentItems.isEmpty()) {
                    log.info("The selected equipment is not available at the moment for Id {}", equipmentRequest.getEquipmentId());
                    errorMessage = "The selected equipment is not available at the moment.Please try again later";
                    errors.put(String.valueOf(equipmentRequest.getEquipmentId()),errorMessage);
                    return;
                }
                if (equipmentItems.size() < equipmentRequest.getQuantity()) {
                    log.info("At the moment only {} {}s are available for equipment Id {}", equipmentItems.size(), equipmentItems.getFirst().getEquipment().getName(), equipmentRequest.getEquipmentId());
                    errorMessage = String.format("At the moment only  %s %ss are available", equipmentItems.size(), equipmentItems.getFirst().getEquipment().getName());
                    errors.put(String.valueOf(equipmentRequest.getEquipmentId()),errorMessage);
                    return;

                }

                equipmentItemsMap.put(equipmentRequest.getEquipmentId(),equipmentItems);
            });


            if(!errors.isEmpty()){
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("Errors occurred while creating equipment request");
                response.setEntity(errors);
                return response;
            }

//            String returnDateStr = request.getReturnDate(); // e.g., "2026-03-20"
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//            LocalDate returnDate = LocalDate.parse(returnDateStr, formatter);
//            Date legacyDate = Date.from(returnDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

            Request equiRequest = Request.builder()
                    .trxId(generateRequestId())
                    .event(request.getEvent())
                    .purpose(request.getPurpose())
                    .venue(request.getVenue())
                    .workflow(approvalWorkflow)
                    .currentApprovalLevel(0)
//                    .returnDate(legacyDate)
                    .status(constantUtil.PENDING_APPROVAL)
                    .createdBy(user.getUsername())
                    .updatedBy(user.getUsername())
                    .build();


            requestRepo.saveAndFlush(equiRequest);
            log.info("Equipment Request sucessfully staged`");



            try {
                int skipCounter = 0;
                for(WorkflowStep workflowStep : workflowSteps){
                    Role role = workflowStep.getRoleId();
                    if (role == null) {
                        //CRiteria for the various heads
                        Optional<DptRoleMap> dptRoleMap = dptRoleMapRepo.findByRole_RoleId(user.getRole().getRoleId());
                        if(dptRoleMap.isPresent()){
                            DptRoleMap dptRoleMapEntity = dptRoleMap.get();
                            Department department = dptRoleMapEntity.getDepartment();
                            if(department != null && department.getDepartmentHead() != null) {
                                role = department.getDepartmentHead();
                            }
                            else{
                                //Skips this approval step
                                log.info("The department head could not be found");
                                skipCounter = skipCounter+1;
                                continue;
                            }
                        }
                        else{
                            //Skips this approval step
                            log.info("The department head could not be found");
                            skipCounter = skipCounter+1;
                            continue;
                        }
                    }

                    RequestApproval requestApproval = RequestApproval.builder()
                            .request(equiRequest)
                            .approverRole(role)
                            .stepLevel(workflowStep.getStepLevel()-skipCounter)
                            .isAllocater(workflowStep.getIsAllocater())
                            .status(constantUtil.PENDING_APPROVAL)
                            .createdAt(new Date())
                            .build();

                    requestApprovals.add(requestApproval);
                }


                requestApprovalRepo.saveAll(requestApprovals);
                log.info("Workflow approval steps successfully requested id:{}", equiRequest.getId());



                //Set equipments for request
                List<EquipmentAllocation> equipmentAllocations = new ArrayList<>();
                List<EquipmentItem> updatedEquipmentItemsList = new  ArrayList<>();
                equipmentRequests.forEach(equipmentRequest -> {

                    List<EquipmentItem> equipmentItemList = equipmentItemsMap.getOrDefault(equipmentRequest.getEquipmentId(),new ArrayList<>());

                    if(!equipmentItemList.isEmpty()) {
                        RequestEquipment requestEquipment = RequestEquipment.builder()
                                .request(equiRequest)
                                .equipment(equipmentItemList.getFirst().getEquipment())
                                .quantityRequested(equipmentRequest.getQuantity())
                                .status(constantUtil.PENDING_APPROVAL)
                                .build();
                        requestEquipmentRepo.saveAndFlush(requestEquipment);

                        List<EquipmentItem> excellentItems = new ArrayList<>(equipmentItemList.stream()
                                .filter(equipmentItem -> equipmentItem.getConditionStatus().equals(constantUtil.EXCELLENT))
                                .toList());

                        List<EquipmentItem> goodItems = new ArrayList<>(equipmentItemList.stream()
                                .filter(equipmentItem -> equipmentItem.getConditionStatus().equals(constantUtil.GOOD))
                                .toList());

                        List<EquipmentItem> fairItems = new ArrayList<>(equipmentItemList.stream()
                                .filter(equipmentItem -> equipmentItem.getConditionStatus().equals(constantUtil.FAIR))
                                .toList());




                        //Allocates excellent items first then good and fair ones next
                        for(int i=0;i<equipmentRequest.getQuantity();i++){
                            if(!excellentItems.isEmpty()){
                                EquipmentItem excellentItem = excellentItems.getFirst();
                                //Reserve equipment so that another person does not make a booking for an  booked item awaiting approval
                                excellentItem.setAvailabilityStatus(constantUtil.RESERVRED);
//                                updatedEquipmentItemsList.add(excellentItem);

                                EquipmentAllocation equipmentAllocation = EquipmentAllocation.builder()
                                        .request(requestEquipment)
                                        .equipmentItem(excellentItem)
                                        .conditionBefore(excellentItem.getConditionStatus())
                                        .status(constantUtil.PENDING_APPROVAL)
                                        .createdBy(user.getUsername())
                                        .updatedBy(user.getUsername())
                                        .build();
                                equipmentAllocations.add(equipmentAllocation);


                                excellentItems.remove(excellentItem);
                            }
                            else if(!goodItems.isEmpty()){
                                EquipmentItem goodItem = goodItems.getFirst();
                                //Reserve equipment so that another person does not make a booking for an  booked item awaiting approval
                                goodItem.setAvailabilityStatus(constantUtil.RESERVRED);
//                                updatedEquipmentItemsList.add(goodItem);


                                EquipmentAllocation equipmentAllocation = EquipmentAllocation.builder()
                                        .request(requestEquipment)
                                        .equipmentItem(goodItem)
                                        .conditionBefore(goodItem.getConditionStatus())
                                        .status(constantUtil.PENDING_APPROVAL)
                                        .createdBy(user.getUsername())
                                        .updatedBy(user.getUsername())
                                        .build();
                                equipmentAllocations.add(equipmentAllocation);
                                goodItems.remove(goodItem);
                            }

                            else if(!fairItems.isEmpty()){
                                EquipmentItem fairItem = fairItems.getFirst();
                                //Reserve equipment so that another person does not make a booking for an  booked item awaiting approval
                                fairItem.setAvailabilityStatus(constantUtil.RESERVRED);
//                                updatedEquipmentItemsList.add(fairItem);


                                EquipmentAllocation equipmentAllocation = EquipmentAllocation.builder()
                                        .request(requestEquipment)
                                        .equipmentItem(fairItem)
                                        .conditionBefore(fairItem.getConditionStatus())
                                        .status(constantUtil.PENDING_APPROVAL)
                                        .createdBy(user.getUsername())
                                        .updatedBy(user.getUsername())
                                        .build();
                                equipmentAllocations.add(equipmentAllocation);
                                fairItems.remove(fairItem);

                            }
                        }
                    }
                });
//                equipmentItemRepo.saveAll(updatedEquipmentItemsList);
                equipmentAllocationRepo.saveAll(equipmentAllocations);
                log.info("Request Equipment and allocations successfully created for  id:{}", equiRequest.getId());


            }
            catch (Exception e ){
                e.printStackTrace();
                equiRequest.setStatus(constantUtil.FAILED);
                requestRepo.saveAndFlush(equiRequest);
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("Error occurred while processing approvals.Kindly Try again later");
                return response;
            }
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
                List<Status> statuses = Arrays.asList(constantUtil.EXCELLENT,constantUtil.GOOD,constantUtil.FAIR);
                List<EquipmentItem> equipmentItemList  = equipmentItemRepo.findByEquipment_IdAndAvailabilityStatusAndConditionStatusIn(equipment.getId(),
                        constantUtil.AVAILABLE,statuses);

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

            List<RequestEquipment> requestEquipments = requestEquipmentRepo.findByRequest_Id(Long.valueOf(request.getId()));
            List<Long> equipmentIds = new ArrayList<>();
            HashSet<Long> duplicateCheckMap = new HashSet<>();

            requestEquipments.forEach(requestEquipment -> {
                if(duplicateCheckMap.add(requestEquipment.getEquipment().getId())) {
                    equipmentIds.add(requestEquipment.getEquipment().getId());
                }
            });

            if (request.getStatuses() != null  && !request.getStatuses().isEmpty()) {
                equipmentConditionSummaries = equipmentConditionSummaryRepo.findByEquipmentIdInAndStatusIdIn(equipmentIds,request.getStatuses());
            } else {
                equipmentConditionSummaries = equipmentConditionSummaryRepo.findByEquipmentIdIn(equipmentIds);
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
                    List<RequestEquipment> requestEquipments = requestEquipmentRepo.findByRequest(existingRequest);


                    if(!requestEquipments.isEmpty()){
                        requestEquipments.forEach(requestEquipment -> {
                            List<EquipmentAllocation> equipmentAllocations = equipmentAllocationRepo.findByRequestAndStatus(
                                    requestEquipment, constantUtil.PENDING_APPROVAL);

                            if(!equipmentAllocations.isEmpty()){
                                equipmentAllocations.forEach(equipmentAllocation -> {
                                    EquipmentItem equipmentItem = equipmentAllocation.getEquipmentItem();
                                    equipmentItem.setAvailabilityStatus(constantUtil.ALLOCATED);
                                    equipmentItemRepo.save(equipmentItem);

                                    equipmentAllocation.setStatus(constantUtil.ALLOCATED);
                                    equipmentAllocation.setAllocatedAt(new  Date());
                                    equipmentAllocation.setUpdatedBy(loggedInUser.getUsername());
                                    equipmentAllocationRepo.save(equipmentAllocation);
                                });
                            }

                            requestEquipment.setStatus(constantUtil.ACTIVE);
                            requestEquipmentRepo.save(requestEquipment);
                    });

                        log.info("Equipment allocations updated successfully allocated for request {}", existingRequest.getId());
                    }

                    existingRequest.setStatus(constantUtil.ACTIVE);
                    requestRepo.save(existingRequest);
                    log.info("Equipment request fully  successfully approved {}",existingRequestApproval.getRequest());
                }


                response.setResponseMessage("Request successfully Approved.");
                response.setResponseCode(ApiResponseCode.SUCCESS);

            }
            else if(request.getAction().equals(REJECT.getValue())){

                Request existingRequest = existingRequestApproval.getRequest();
                List<RequestEquipment> requestEquipments = requestEquipmentRepo.findByRequest(existingRequest);

                //update status of request equipment
                if(!requestEquipments.isEmpty()){
                    requestEquipments.forEach(requestEquipment -> {
                        List<EquipmentAllocation> equipmentAllocations = equipmentAllocationRepo.findByRequestAndStatus(
                                requestEquipment, constantUtil.PENDING_APPROVAL);

                        if(!equipmentAllocations.isEmpty()){
                            equipmentAllocations.forEach(equipmentAllocation -> {
                                EquipmentItem equipmentItem = equipmentAllocation.getEquipmentItem();
                                //make equipment available for  another person to book it
                                equipmentItem.setAvailabilityStatus(constantUtil.AVAILABLE);
                                equipmentItemRepo.save(equipmentItem);

                                equipmentAllocation.setStatus(constantUtil.REJECTED);
                                equipmentAllocation.setAllocatedAt(new  Date());
                                equipmentAllocation.setUpdatedBy(loggedInUser.getUsername());
                                equipmentAllocationRepo.save(equipmentAllocation);
                            });
                        }

                        requestEquipment.setStatus(constantUtil.ACTIVE);
                        requestEquipmentRepo.save(requestEquipment);
                    });

                    log.info("Equipment allocations updated successfully allocated for request {}", existingRequest.getId());
                }


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




//    public ApiResponse createEquipmentReturnRequests(HttpServletResponse httpServletResponse, EquipmentRequest request) {
//        ApiResponse response = new ApiResponse();
//        try{
//            log.info("Creating equipment Return using request: {}", request);
//            User user = getauthenticatedAPIUser();
//
//
//            //check for existing request
//            List<ReturnRequest> existingPendingRequest = returnRequestRepo.findByCreatedByAndStatus(user.getUsername(),constantUtil.PENDING_APPROVAL);
//            if(!existingPendingRequest.isEmpty()){
//                log.info("Their is an existing pending request for  Id {}", request.getEquipmentId());
//                response.setResponseCode(ApiResponseCode.FAIL);
//                response.setResponseMessage("You have existing request awaiting approval");
//                return response;
//            }
//
//            //check if equipment exists for that quantity
//            List<EquipmentItem> equipmentItems =  equipmentItemRepo.findByEquipment_IdAndAvailabilityStatus(request.getEquipmentId(),constantUtil.AVAILABLE);
//            if(equipmentItems.isEmpty()){
//                log.info("The selected equipment is not available at the moment for Id {}", request.getEquipmentId());
//                response.setResponseCode(ApiResponseCode.FAIL);
//                response.setResponseMessage("The selected equipment is not available at the moment.Please try again later");
//                return response;
//            }
//            if(equipmentItems.size()<request.getQuantity()){
//                log.info("At the moment only {} {}s are available for equipment Id {}", request.getQuantity(), equipmentItems.getFirst().getEquipment().getName(),request.getEquipmentId());
//                response.setResponseCode(ApiResponseCode.FAIL);
//                response.setResponseMessage(String.format("At the moment only  %s %ss are available", request.getQuantity(), equipmentItems.getFirst().getEquipment().getName()));
//                return response;
//            }
//
//
//            //TODO --> CHECK FOR QUALITY OF ITEMS
//
//            //CHECK IF ALL WORKFLOW STEPS HAVE BEEN CREATED
//            Equipment equipment = equipmentItems.getFirst().getEquipment();
//            ApprovalWorkflow approvalWorkflow = equipment.getWorkflow();
//
//            if(approvalWorkflow==null){
//                log.info("The selected equipment has not fully configured approval workflows for equipment Id {}", request.getEquipmentId());
//                response.setResponseCode(ApiResponseCode.FAIL);
//                response.setResponseMessage("The selected equipment has not fully configured approval workflows.Kindly contact Admin");
//                return response;
//            }
//            List<WorkflowStep> workflowSteps = workflowStepRepo.findByWorkflow(approvalWorkflow);
//
//            if(workflowSteps.isEmpty()){
//                log.info("The selected equipment has not fully configured approval workflows for equipment Id {}", request.getEquipmentId());
//                response.setResponseCode(ApiResponseCode.FAIL);
//                response.setResponseMessage("The selected equipment has not fully configured approval workflows.Kindly contact Admin");
//                return response;
//            }
//
//            String returnDateStr = request.getReturnDate(); // e.g., "2026-03-20"
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//            LocalDate returnDate = LocalDate.parse(returnDateStr, formatter);
//            Date legacyDate = Date.from(returnDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
//
//            //Create the equipment request
//            Request equipmentRequest = Request.builder()
//                    .trxId(generateRequestId(request.getEquipmentId()))
//                    .event(request.getEvent())
//                    .purpose(request.getPurpose())
//                    .venue(request.getVenue())
//                    .workflow(approvalWorkflow)
//                    .equipment(equipment)
//                    .quantity(request.getQuantity())
//                    .currentApprovalLevel(0)
//                    .returnDate(legacyDate)
//                    .status(constantUtil.PENDING_APPROVAL)
//                    .createdBy(user.getUsername())
//                    .updatedBy(user.getUsername())
//                    .build();
//
//
//            requestRepo.saveAndFlush(equipmentRequest);
//            log.info("Request successfully requested id:{}",equipmentRequest.getId());
//
//
//            List<RequestApproval> requestApprovals = new ArrayList<>();
//            workflowSteps.forEach(workflowStep -> {
//                RequestApproval requestApproval = RequestApproval.builder()
//                        .request(equipmentRequest)
//                        .approverRole(workflowStep.getRoleId())
//                        .stepLevel(workflowStep.getStepLevel())
//                        .isAllocater(workflowStep.getIsAllocater())
//                        .status(constantUtil.PENDING_APPROVAL)
//                        .createdAt(new Date())
//                        .build();
//
//                requestApprovals.add(requestApproval);
//            });
//
//            requestApprovalRepo.saveAll(requestApprovals);
//            log.info("Workflow approval steps successfully requested id:{}",equipmentRequest.getId());
//            response.setResponseCode(ApiResponseCode.SUCCESS);
//            response.setResponseMessage("Equipment request successfully created");
//
//        }
//        catch (Exception e){
//            log.error("ERROR OCCURRED DURING CREATION OF EQUIPMENT REQUEST :: {}" ,e.getMessage());
//            e.printStackTrace();
//            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
//            response.setResponseCode(ApiResponseCode.FAIL);
//            response.setResponseMessage("Sorry, an error occurred while creating equipment request! Please Try again later");
//        }
//        return response;
//    }


}
