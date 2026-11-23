package org.example.learningprojectserver.mappers;

import org.example.learningprojectserver.dto.PortfolioDTOs;
import org.example.learningprojectserver.entities.PortfolioProjectEntity;
import org.springframework.stereotype.Component;

@Component
public class PortfolioProjectEntityToDTOMapper implements Mapper<PortfolioProjectEntity, PortfolioDTOs.Project> {

    @Override
    public PortfolioDTOs.Project apply(PortfolioProjectEntity p) {
        return new PortfolioDTOs.Project(p.getId(), p.getTitle(), p.getDescription(), p.getGradeName(), p.getCreatedAt());
    }
}
