package com.mis.church.repository;

import com.mis.church.entity.Status;
import com.mis.church.entity.UserLoginLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserLoginLogRepo extends JpaRepository<UserLoginLog,Integer> {
    UserLoginLog findDistinctByUser_UserIdAndStatus(Integer userId, Status status);
    UserLoginLog findDistinctByUser_UserIdAndStatusIn(Integer userId, List<Status> status);
}
