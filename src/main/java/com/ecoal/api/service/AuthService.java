package com.ecoal.api.service;

import com.ecoal.api.dto.auth.AuthResponse;
import com.ecoal.api.dto.auth.SignInRequest;
import com.ecoal.api.dto.auth.SignUpRequest;
import com.ecoal.api.dto.auth.UserResponse;
import com.ecoal.api.entity.Company;
import com.ecoal.api.entity.UserAccount;
import com.ecoal.api.exception.ConflictException;
import com.ecoal.api.exception.NotFoundException;
import com.ecoal.api.repository.CompanyRepository;
import com.ecoal.api.repository.UserAccountRepository;
import com.ecoal.api.security.AppUserDetails;
import com.ecoal.api.security.CurrentUserProvider;
import com.ecoal.api.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final CompanyRepository companyRepository;
    private final UserAccountRepository userAccountRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(
            CompanyRepository companyRepository,
            UserAccountRepository userAccountRepository,
            JwtService jwtService,
            BCryptPasswordEncoder passwordEncoder) {
        this.companyRepository = companyRepository;
        this.userAccountRepository = userAccountRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse signUp(SignUpRequest request) {
        Company company = companyRepository.findByCnpj(request.getCompanyCnpj())
                .orElseThrow(() -> new NotFoundException("Company not found"));

        if (userAccountRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered");
        }

        UserAccount user = new UserAccount();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setCompany(company);
        user.setDepartment(request.getDepartment());

        UserAccount saved = userAccountRepository.save(user);
        return new AuthResponse(jwtService.generateToken(saved), toUserResponse(saved));
    }

    public AuthResponse signIn(SignInRequest request) {
        UserAccount user = userAccountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return new AuthResponse(jwtService.generateToken(user), toUserResponse(user));
    }

    public UserResponse me() {
        AppUserDetails current = CurrentUserProvider.get();
        UserAccount user = userAccountRepository.findByEmail(current.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found"));
        return toUserResponse(user);
    }

    private UserResponse toUserResponse(UserAccount user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setCompanyId(user.getCompany().getId());
        response.setDepartment(user.getDepartment());
        return response;
    }
}
