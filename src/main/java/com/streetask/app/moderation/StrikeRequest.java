package com.streetask.app.moderation;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StrikeRequest {
    
    @NotBlank(message = "Reason is required")
    private String reason;
    
    private String description;
}
