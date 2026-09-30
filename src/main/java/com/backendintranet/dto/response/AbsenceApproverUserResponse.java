package com.backendintranet.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AbsenceApproverUserResponse {

    private String id;
    private String username;
    private String name;
    private String email;
    private String position;
    private String sede;
}
