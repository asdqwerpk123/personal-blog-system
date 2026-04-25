package org.example.personalblogsystem.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminPasswordChangeRequest {

    private String oldPassword;
    private String newPassword;
}
