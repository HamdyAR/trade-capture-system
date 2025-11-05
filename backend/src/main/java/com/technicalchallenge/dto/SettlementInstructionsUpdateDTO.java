package com.technicalchallenge.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class SettlementInstructionsUpdateDTO {

    @Size(min = 10, max = 500, message = "Settlement instructions must be between 10 and 500 characters")
    @Pattern(
        regexp = "^[\\p{L}\\p{N}\\s.,:'\\-]+$",
        message = "Settlement instructions cannot contain invalid characters"
    )
    private String instructions;
}
