package org.example.personalblogsystem.auth;

import lombok.Value;

@Value
public class AdminAuthPrincipal {

    Long userId;
    String userName;
    Long roleId;
    String roleCode;
}
