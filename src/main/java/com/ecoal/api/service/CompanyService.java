package com.ecoal.api.service;

import com.ecoal.api.dto.company.CompanyRequest;
import com.ecoal.api.dto.company.CompanyResponse;
import com.ecoal.api.entity.Company;
import com.ecoal.api.exception.ConflictException;
import com.ecoal.api.exception.ForbiddenException;
import com.ecoal.api.exception.NotFoundException;
import com.ecoal.api.repository.CompanyRepository;
import com.ecoal.api.security.AppUserDetails;
import com.ecoal.api.security.CurrentUserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class CompanyService {

    @Autowired
    private CompanyRepository companyRepository;

    public CompanyResponse create(CompanyRequest request) {
        try {
            Company company = new Company();
            company.setName(request.getName());
            company.setCnpj(request.getCnpj());
            company.setGlobalScoreGoal(request.getGlobalScoreGoal());
            Company saved = companyRepository.save(company);
            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("CNPJ already registered");
        }
    }

    public CompanyResponse get(Long id) {
        Company company = findByIdOrThrow(id);
        checkAccess(company);
        return toResponse(company);
    }

    public CompanyResponse update(Long id, CompanyRequest request) {
        Company company = findByIdOrThrow(id);
        checkAccess(company);
        try {
            company.setName(request.getName());
            company.setCnpj(request.getCnpj());
            company.setGlobalScoreGoal(request.getGlobalScoreGoal());
            Company saved = companyRepository.save(company);
            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("CNPJ already registered");
        }
    }

    public void delete(Long id) {
        Company company = findByIdOrThrow(id);
        checkAccess(company);
        companyRepository.deleteById(id);
    }

    private Company findByIdOrThrow(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Company not found"));
    }

    private void checkAccess(Company company) {
        AppUserDetails current = CurrentUserProvider.get();
        if (!company.getId().equals(current.getCompanyId())) {
            throw new ForbiddenException("Access denied");
        }
    }

    private CompanyResponse toResponse(Company company) {
        CompanyResponse response = new CompanyResponse();
        response.setId(company.getId());
        response.setName(company.getName());
        response.setCnpj(company.getCnpj());
        response.setGlobalScoreGoal(company.getGlobalScoreGoal());
        response.setCreatedAt(company.getCreatedAt());
        response.setUpdatedAt(company.getUpdatedAt());
        return response;
    }
}
