package com.appsmith.server.controllers;

import com.appsmith.server.constants.Url;
import com.appsmith.server.controllers.ce.TenantControllerCE;
import com.appsmith.server.domains.License;
import com.appsmith.server.domains.Tenant;
import com.appsmith.server.dtos.ResponseDTO;
import com.appsmith.server.services.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;

@RestController
@Slf4j
@RequestMapping(Url.TENANT_URL)
public class TenantController extends TenantControllerCE {

    private final TenantService service;
    public TenantController(TenantService service) {
        super(service);
        this.service = service;
    }

    @PutMapping("license")
    public Mono<ResponseDTO<Tenant>> updateTenantLicenseKey(@RequestBody @Valid License license) {
        return service.updateTenantLicenseKey(license.getKey())
                .map(tenant -> new ResponseDTO<>(HttpStatus.OK.value(), tenant, null));
    }

    @PostMapping("license")
    public Mono<ResponseDTO<String>> addLicenseKeyAndGetRedirectUrl(@RequestBody @Valid License license, ServerWebExchange exchange) {
        return service.addLicenseKeyAndGetRedirectUrl(license.getKey(), exchange)
                .map(tenant -> new ResponseDTO<>(HttpStatus.OK.value(), tenant, null));
    }

    /**
     * API to refresh the current license status from the Cloud Services and return the latest status
     * @return Mono of ResponseDTO of Tenant
     */
    @GetMapping("license")
    public Mono<ResponseDTO<Tenant>> getLicense() {
        return service.refreshAndGetCurrentLicense()
                .map(tenant -> new ResponseDTO<>(HttpStatus.OK.value(), tenant, null));
    }
}