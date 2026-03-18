package com.mis.church.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApprovalType {
    ROLE("ROLE"),
    ROLE_UPDATE("ROLE_UPDATE"),
    USER("USER"),
    USER_UPDATE("USER_UPDATE"),
    EQUIPMENT("EQUIPMENT")
    ;
    private final String value;
}
