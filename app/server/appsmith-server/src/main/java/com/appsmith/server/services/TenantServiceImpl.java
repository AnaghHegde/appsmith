package com.appsmith.server.services;

import com.appsmith.external.constants.AnalyticsEvents;
import com.appsmith.external.helpers.DataTypeStringUtils;
import com.appsmith.server.acl.AclPermission;
import com.appsmith.server.constants.FieldName;
import com.appsmith.server.constants.LicenseStatus;
import com.appsmith.server.domains.Tenant;
import com.appsmith.server.domains.TenantConfiguration;
import com.appsmith.server.exceptions.AppsmithError;
import com.appsmith.server.exceptions.AppsmithException;
import com.appsmith.server.repositories.TenantRepository;
import com.appsmith.server.services.ce.TenantServiceCEImpl;
import com.appsmith.server.solutions.LicenseValidator;
import org.pf4j.util.StringUtils;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import jakarta.validation.Validator;

import java.util.Map;

@Service
public class TenantServiceImpl extends TenantServiceCEImpl implements TenantService {
    private final LicenseValidator licenseValidator;
    public TenantServiceImpl(Scheduler scheduler,
                             Validator validator,
                             MongoConverter mongoConverter,
                             ReactiveMongoTemplate reactiveMongoTemplate,
                             TenantRepository repository,
                             AnalyticsService analyticsService,
                             LicenseValidator licenseValidator,
                             ConfigService configService) {

        super(scheduler, validator, mongoConverter, reactiveMongoTemplate, repository, analyticsService, configService);
        this.licenseValidator = licenseValidator;
    }

    @Override
    public Mono<Tenant> findById(String id, AclPermission aclPermission) {
        return repository.findById(id, aclPermission);
    }

    @Override
    public Mono<Tenant> save(Tenant tenant) {
        return repository.save(tenant);
    }

    @Override
    public Mono<Tenant> getDefaultTenant() {
        // Get the default tenant object from the DB and then populate the relevant user permissions in that
        // We are doing this differently because `findBySlug` is a Mongo JPA query and not a custom Appsmith query
        return repository.findBySlug(FieldName.DEFAULT)
                .flatMap(tenant -> repository.setUserPermissionsInObject(tenant)
                        .switchIfEmpty(Mono.just(tenant)));
    }

    @Override
    public Mono<Tenant> getDefaultTenant(AclPermission aclPermission) {
        return repository.findBySlug(FieldName.DEFAULT, aclPermission);
    }

    @Override
    public Mono<Tenant> getTenantConfiguration() {
        return Mono.zip(
                        this.getDefaultTenant(),
                        super.getTenantConfiguration()
                )
                .map(tuple -> {
                    final Tenant dbTenant = tuple.getT1();
                    final Tenant clientTenant = tuple.getT2();
                    return getClientPertinentTenant(dbTenant, clientTenant);
                });
    }

    /**
     * To set a license key to the default tenant
     * Only valid license key will get added to the tenant
     * @param licenseKey License key received from client
     * @return Mono of Tenant
     */
    public Mono<Tenant> setTenantLicenseKey(String licenseKey) {
        TenantConfiguration.License license = new TenantConfiguration.License();
        license.setKey(licenseKey);
        // TODO: Update to getCurrentTenant when multi tenancy is introduced
        return repository.findBySlug(FieldName.DEFAULT, AclPermission.MANAGE_TENANT)
                .switchIfEmpty(Mono.error(new AppsmithException(AppsmithError.NO_RESOURCE_FOUND, FieldName.TENANT, FieldName.DEFAULT)))
                .flatMap(tenant -> {
                    TenantConfiguration tenantConfiguration = tenant.getTenantConfiguration();
                    boolean isActivateInstance = tenantConfiguration.getLicense() == null || StringUtils.isNullOrEmpty(tenantConfiguration.getLicense().getKey());
                    tenantConfiguration.setLicense(license);
                    tenant.setTenantConfiguration(tenantConfiguration);

                    return checkTenantLicense(tenant).zipWith(Mono.just(isActivateInstance));
                })
                .flatMap(tuple -> {
                    Tenant tenant = tuple.getT1();
                    boolean isActivateInstance = tuple.getT2();
                    TenantConfiguration.License license1 = tenant.getTenantConfiguration().getLicense();
                    AnalyticsEvents analyticsEvent = isActivateInstance ? AnalyticsEvents.ACTIVATE_NEW_INSTANCE : AnalyticsEvents.UPDATE_EXISTING_LICENSE;
                    Map<String, Object> analyticsProperties = Map.of(
                            FieldName.LICENSE_KEY, StringUtils.isNullOrEmpty(license1.getKey()) ? "" : DataTypeStringUtils.maskString(license1.getKey()),
                            FieldName.LICENSE_VALID, license1.getStatus() != null  && LicenseStatus.ACTIVE.equals(license1.getStatus()),
                            FieldName.LICENSE_TYPE, license1.getType() == null ? "" : license1.getType(),
                            FieldName.LICENSE_STATUS, license1.getStatus() == null ? "" : license1.getStatus()
                    );
                    Mono<Tenant> analyticsEventMono = analyticsService.sendObjectEvent(analyticsEvent, tenant, analyticsProperties);
                    // Update/save license only in case of a valid license key
                    if (!Boolean.TRUE.equals(tenant.getTenantConfiguration().getLicense().getActive())) {
                        return analyticsEventMono.then(Mono.error(new AppsmithException(AppsmithError.INVALID_LICENSE_KEY_ENTERED)));
                    }

                    return this.save(tenant)
                            .flatMap(analyticsEventMono::thenReturn);
                })
                .map(tenant -> getClientPertinentTenant(tenant, null));
    }

    /**
     * To refresh the current license status in the DB by making a license validation request to the Cloud Services and
     * return latest license status
     * @return Mono of Tenant
     */
    public Mono<Tenant> refreshAndGetCurrentLicense() {
        // TODO: Update to getCurrentTenant when multi tenancy is introduced
        return repository.findBySlug(FieldName.DEFAULT, AclPermission.MANAGE_TENANT)
                .switchIfEmpty(Mono.error(new AppsmithException(AppsmithError.NO_RESOURCE_FOUND, FieldName.TENANT, FieldName.DEFAULT)))
                .flatMap(this::checkTenantLicense)
                .flatMap(this::save)
                .map(tenant -> getClientPertinentTenant(tenant, null));
    }

    /**
     * To check the status of a license key associated with the tenant
     * @param tenant Tenant
     * @return Mono of Tenant
     */
    private Mono<Tenant> checkTenantLicense(Tenant tenant) {
        Mono<TenantConfiguration.License> licenseMono = licenseValidator.licenseCheck(tenant);
        return licenseMono
            .map(license -> {
                // To prevent empty License object being saved in DB for license checks with empty license key
                if (!StringUtils.isNullOrEmpty(license.getKey())) {
                    TenantConfiguration tenantConfiguration = tenant.getTenantConfiguration();
                    tenantConfiguration.setLicense(license);
                    tenant.setTenantConfiguration(tenantConfiguration);
                }
                return tenant;
            });
    }

    /**
     * To check and update the status of default tenant's license
     * This can be used for periodic license checks via scheduled jobs
     * @return Mono of Tenant
     */
    public Mono<Tenant> checkAndUpdateDefaultTenantLicense() {
        return this.getDefaultTenant()
                .flatMap(this::checkTenantLicense)
                .flatMap(this::save);
    }

    /**
     * To get the Tenant with values that are pertinent to the client
     * @param dbTenant Original tenant from the database
     * @param clientTenant Tenant object that is sent to the client, can be null
     * @return Tenant
     */
    private Tenant getClientPertinentTenant(Tenant dbTenant, Tenant clientTenant) {
        TenantConfiguration tenantConfiguration;
        if (clientTenant == null) {
            clientTenant = new Tenant();
            tenantConfiguration = new TenantConfiguration();
        } else {
            tenantConfiguration = clientTenant.getTenantConfiguration();
        }

        // Only copy the values that are pertinent to the client
        tenantConfiguration.copyNonSensitiveValues(dbTenant.getTenantConfiguration());
        clientTenant.setTenantConfiguration(tenantConfiguration);
        clientTenant.setUserPermissions(dbTenant.getUserPermissions());

        return clientTenant;
    }

    /**
     * To check whether a tenant have valid license configuration
     * @param tenant Tenant
     * @return Boolean
     */
    public Boolean isValidLicenseConfiguration(Tenant tenant) {
        return tenant.getTenantConfiguration() != null &&
                tenant.getTenantConfiguration().getLicense() != null &&
                tenant.getTenantConfiguration().getLicense().getKey() != null;
    }
}
