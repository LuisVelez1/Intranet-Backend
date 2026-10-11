package com.backendintranet.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AbsenceTypeResponse {

    private Long id;
    private String name;
    private String description;
    private Boolean active;
}
