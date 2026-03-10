package com.mis.church.service;


import com.mis.church.entity.Status;
import com.mis.church.entity.User;
import com.mis.church.repository.UserRepo;
import com.mis.church.util.ConstantUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DatabaseService {

        private final UserRepo userRepo;


    public User getUserByUsername(String username, ConstantUtil constantUtil) {
        List<Status> statuses = Arrays.asList(constantUtil.ACTIVE);
        return userRepo.findDistinctByUsernameEqualsIgnoreCaseAndStatusIn(username,statuses);
    }
}
