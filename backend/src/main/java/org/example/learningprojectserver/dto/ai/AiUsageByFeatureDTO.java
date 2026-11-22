package org.example.learningprojectserver.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiUsageByFeatureDTO {
    private String feature;
    private Long requests;
    private Long promptTokens;
    private Long completionTokens;
    private Double costUsd;
}
