package com.mis.church.dto;


import com.mis.church.entity.User;
import lombok.Data;

import java.util.List;

@Data
public class UserInfoDTO {
    private User user;
    private List<String> usersPerm;
    private String role;
}
