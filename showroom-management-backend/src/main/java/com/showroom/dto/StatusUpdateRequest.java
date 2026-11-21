package com.showroom.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Generic body for PATCH .../status endpoints, e.g. {"status": "COMPLETED", "note": "..."} */
@Data
public class StatusUpdateRequest {

    @NotBlank(message = "Status is required")
    private String status;

    private String note;
}
