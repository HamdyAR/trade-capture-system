package com.technicalchallenge.rsql;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import io.github.perplexhub.rsql.RSQLJPASupport;

@Component
public class RsqlSpecificationBuilder<Trade> {

    public Specification<Trade> createSpecification(String rsqlQuery){
        if(rsqlQuery == null || rsqlQuery.trim().isEmpty()){
            return Specification.where(null);
        }

        try{
            return RSQLJPASupport.toSpecification(rsqlQuery);
        }
        catch(IllegalArgumentException e){
            throw new IllegalArgumentException("Inavlid RSQL query: " + rsqlQuery + "Error: " + e.getMessage());
        }
        catch(Exception e){
            throw new IllegalArgumentException("Invalid RSQL query " + rsqlQuery + "Error: " + e.getMessage());
        }

    }
    
}
