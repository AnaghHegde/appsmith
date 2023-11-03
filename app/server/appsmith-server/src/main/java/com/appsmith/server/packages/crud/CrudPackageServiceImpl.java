package com.appsmith.server.packages.crud;

import com.appsmith.external.models.Policy;
import com.appsmith.server.acl.PolicyGenerator;
import com.appsmith.server.annotations.FeatureFlagged;
import com.appsmith.server.constants.ApplicationConstants;
import com.appsmith.server.constants.FieldName;
import com.appsmith.server.constants.ResourceModes;
import com.appsmith.server.domains.Package;
import com.appsmith.server.domains.QPackage;
import com.appsmith.server.domains.User;
import com.appsmith.server.domains.Workspace;
import com.appsmith.server.dtos.ModuleDTO;
import com.appsmith.server.dtos.PackageDTO;
import com.appsmith.server.dtos.PackageDetailsDTO;
import com.appsmith.server.exceptions.AppsmithError;
import com.appsmith.server.exceptions.AppsmithException;
import com.appsmith.server.featureflags.FeatureFlagEnum;
import com.appsmith.server.helpers.ObjectUtils;
import com.appsmith.server.helpers.ValidationUtils;
import com.appsmith.server.modules.crud.CrudModuleService;
import com.appsmith.server.packages.permissions.PackagePermission;
import com.appsmith.server.repositories.PackageRepository;
import com.appsmith.server.services.SessionUserService;
import com.appsmith.server.services.WorkspaceService;
import com.appsmith.server.solutions.WorkspacePermission;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.appsmith.server.repositories.ce.BaseAppsmithRepositoryCEImpl.fieldName;

@Service
public class CrudPackageServiceImpl extends CrudPackageServiceCECompatibleImpl implements CrudPackageService {

    private final PackageRepository packageRepository;
    private final WorkspaceService workspaceService;
    private final WorkspacePermission workspacePermission;
    private final PolicyGenerator policyGenerator;
    private final PackagePermission packagePermission;
    private final CrudModuleService crudModuleService;
    private final SessionUserService sessionUserService;

    public CrudPackageServiceImpl(
            PackageRepository packageRepository,
            WorkspaceService workspaceService,
            WorkspacePermission workspacePermission,
            PolicyGenerator policyGenerator,
            PackagePermission packagePermission,
            CrudModuleService crudModuleService,
            SessionUserService sessionUserService) {
        super(packageRepository);
        this.packageRepository = packageRepository;
        this.workspaceService = workspaceService;
        this.workspacePermission = workspacePermission;
        this.policyGenerator = policyGenerator;
        this.packagePermission = packagePermission;
        this.crudModuleService = crudModuleService;
        this.sessionUserService = sessionUserService;
    }

    @Override
    @FeatureFlagged(featureFlagName = FeatureFlagEnum.release_query_module_enabled)
    public Mono<PackageDTO> createPackage(PackageDTO packageToBeCreated, String workspaceId) {
        if (ValidationUtils.isEmptyParam(packageToBeCreated.getName())) {
            return Mono.error(new AppsmithException(AppsmithError.INVALID_PARAMETER, FieldName.NAME));
        }
        if (ValidationUtils.isEmptyParam(workspaceId)) {
            return Mono.error(new AppsmithException(AppsmithError.INVALID_PARAMETER, FieldName.WORKSPACE_ID));
        }

        Mono<User> userMono = sessionUserService.getCurrentUser().cache();

        return workspaceService
                .findById(workspaceId, workspacePermission.getPackageCreatePermission())
                .switchIfEmpty(Mono.error(new AppsmithException(
                        AppsmithError.ACL_NO_RESOURCE_FOUND, FieldName.WORKSPACE_ID, workspaceId)))
                .zipWith(userMono)
                .flatMap(tuple2 -> {
                    Workspace workspace = tuple2.getT1();
                    User currentUser = tuple2.getT2();
                    Package newPackage = new Package();
                    newPackage.setWorkspaceId(workspace.getId());
                    newPackage.setPackageUUID(new ObjectId().toString());

                    newPackage.setUnpublishedPackage(packageToBeCreated);
                    newPackage.setPublishedPackage(new PackageDTO());

                    Set<Policy> policies = policyGenerator.getAllChildPolicies(
                            workspace.getPolicies(), Workspace.class, Package.class);
                    newPackage.setPolicies(policies);
                    newPackage.setModifiedBy(currentUser.getUsername());

                    return createSuffixedPackage(newPackage, packageToBeCreated.getName(), 0)
                            .flatMap(createdPackage -> setTransientFieldsFromPackageToPackageDTO(
                                    createdPackage, createdPackage.getUnpublishedPackage()));
                });
    }

    @Override
    @FeatureFlagged(featureFlagName = FeatureFlagEnum.release_query_module_enabled)
    public Mono<List<PackageDTO>> getAllPackages() {
        return packageRepository
                .findAllUserPackages(packagePermission.getEditPermission())
                .flatMap(aPackage -> generatePackageByViewMode(aPackage, ResourceModes.EDIT))
                .collectList();
    }

    @Override
    public Mono<PackageDTO> generatePackageByViewMode(Package aPackage, ResourceModes resourceMode) {
        PackageDTO packageDTO;
        if (aPackage.getDeletedAt() != null) {
            return Mono.empty();
        }

        if (resourceMode.equals(ResourceModes.EDIT)) {
            packageDTO = aPackage.getUnpublishedPackage();
        } else {
            packageDTO = aPackage.getPublishedPackage();
        }
        return setTransientFieldsFromPackageToPackageDTO(aPackage, packageDTO);
    }

    @Override
    @FeatureFlagged(featureFlagName = FeatureFlagEnum.release_query_module_enabled)
    public Mono<PackageDetailsDTO> getPackageDetails(String packageId) {

        Mono<PackageDTO> packageDataMono = packageRepository
                .findById(packageId, packagePermission.getReadPermission())
                .flatMap(aPackage -> this.generatePackageByViewMode(aPackage, ResourceModes.EDIT))
                .switchIfEmpty(Mono.error(
                        new AppsmithException(AppsmithError.ACL_NO_RESOURCE_FOUND, FieldName.PACKAGE_ID, packageId)));

        Mono<List<ModuleDTO>> modulesMono = crudModuleService.getAllModuleDTOs(packageId, ResourceModes.EDIT);

        return packageDataMono.flatMap(packageDTO -> {
            final PackageDetailsDTO packageDetailsDTO = new PackageDetailsDTO();

            packageDetailsDTO.setPackageData(packageDTO);

            return modulesMono.flatMap(moduleDTOS -> {
                packageDetailsDTO.setModules(moduleDTOS);

                return Mono.just(packageDetailsDTO);
            });
        });
    }

    @Override
    @FeatureFlagged(featureFlagName = FeatureFlagEnum.release_query_module_enabled)
    public Mono<PackageDTO> updatePackage(PackageDTO packageResource, String packageId) {
        Mono<User> userMono = sessionUserService.getCurrentUser().cache();

        return packageRepository
                .findById(packageId, packagePermission.getEditPermission())
                .switchIfEmpty(Mono.error(
                        new AppsmithException(AppsmithError.ACL_NO_RESOURCE_FOUND, FieldName.PACKAGE_ID, packageId)))
                .zipWith(userMono)
                .flatMap(tuple2 -> {
                    Package dbPackage = tuple2.getT1();
                    User currentUser = tuple2.getT2();
                    Update updateObj = prepareUpdatableFieldsForPackage(packageResource);

                    if (updateObj.getUpdateObject().isEmpty()) {
                        return setTransientFieldsFromPackageToPackageDTO(dbPackage, dbPackage.getUnpublishedPackage());
                    }
                    updateObj.set(fieldName(QPackage.package$.updatedAt), Instant.now());
                    updateObj.set(fieldName(QPackage.package$.modifiedBy), currentUser.getUsername());

                    return packageRepository
                            .updateAndReturn(packageId, updateObj, Optional.of(packagePermission.getEditPermission()))
                            .flatMap(updatedPackage -> packageRepository.setUserPermissionsInObject(updatedPackage))
                            .flatMap(updatedPackage -> setTransientFieldsFromPackageToPackageDTO(
                                    updatedPackage, updatedPackage.getUnpublishedPackage()));
                });
    }

    private Update prepareUpdatableFieldsForPackage(PackageDTO packageResource) {
        Update updateObj = new Update();
        String iconPath = fieldName(QPackage.package$.unpublishedPackage) + "."
                + fieldName(QPackage.package$.unpublishedPackage.icon);
        String colorPath = fieldName(QPackage.package$.unpublishedPackage) + "."
                + fieldName(QPackage.package$.unpublishedPackage.color);
        String namePath = fieldName(QPackage.package$.unpublishedPackage) + "."
                + fieldName(QPackage.package$.unpublishedPackage.name);

        ObjectUtils.setIfNotEmpty(updateObj, iconPath, packageResource.getIcon());
        ObjectUtils.setIfNotEmpty(updateObj, colorPath, packageResource.getColor());
        ObjectUtils.setIfNotEmpty(updateObj, namePath, packageResource.getName());

        return updateObj;
    }

    private Mono<PackageDTO> setTransientFieldsFromPackageToPackageDTO(Package aPackage, PackageDTO packageDTO) {
        packageDTO.setWorkspaceId(aPackage.getWorkspaceId());
        packageDTO.setId(aPackage.getId());
        packageDTO.setPackageUUID(aPackage.getPackageUUID());
        packageDTO.setUserPermissions(aPackage.getUserPermissions());
        packageDTO.setModifiedAt(aPackage.getLastUpdateTime());
        packageDTO.setModifiedBy(aPackage.getModifiedBy());
        packageDTO.setLastPublishedAt(aPackage.getLastPublishedTime());

        return Mono.just(packageDTO);
    }

    private Mono<Package> createSuffixedPackage(Package requestedPackage, String name, int suffix) {
        final String actualName = name + (suffix == 0 ? "" : " (" + suffix + ")");
        requestedPackage.getUnpublishedPackage().setName(actualName);

        if (!StringUtils.hasLength(requestedPackage.getUnpublishedPackage().getColor())) {
            requestedPackage.getUnpublishedPackage().setColor(getRandomPackageCardColor());
        }

        return packageRepository
                .save(requestedPackage)
                .flatMap(packageRepository::setUserPermissionsInObject)
                .onErrorResume(DuplicateKeyException.class, error -> {
                    if (error.getMessage() != null && error.getMessage().contains("ws_pkg_name_deleted_at_uindex")) {
                        if (suffix > 5) {
                            return Mono.error(new AppsmithException(AppsmithError.DUPLICATE_KEY_PAGE_RELOAD, name));
                        } else {
                            return createSuffixedPackage(requestedPackage, name, suffix + 1);
                        }
                    }
                    throw error;
                });
    }

    public String getRandomPackageCardColor() {
        int randomColorIndex = (int) (System.currentTimeMillis() % ApplicationConstants.APP_CARD_COLORS.length);
        return ApplicationConstants.APP_CARD_COLORS[randomColorIndex];
    }
}
