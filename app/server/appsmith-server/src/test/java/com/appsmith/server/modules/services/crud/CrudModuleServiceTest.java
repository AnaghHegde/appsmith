package com.appsmith.server.modules.services.crud;

import com.appsmith.external.models.ModuleInput;
import com.appsmith.external.models.ModuleType;
import com.appsmith.server.configurations.CommonConfig;
import com.appsmith.server.domains.Module;
import com.appsmith.server.domains.User;
import com.appsmith.server.domains.Workspace;
import com.appsmith.server.dtos.ModuleActionDTO;
import com.appsmith.server.dtos.ModuleDTO;
import com.appsmith.server.dtos.PackageDTO;
import com.appsmith.server.featureflags.FeatureFlagEnum;
import com.appsmith.server.helpers.MockPluginExecutor;
import com.appsmith.server.helpers.ModuleConsumable;
import com.appsmith.server.helpers.PluginExecutorHelper;
import com.appsmith.server.moduleinstances.services.permissions.ModuleInstancePermissionChecker;
import com.appsmith.server.modules.crud.CrudModuleService;
import com.appsmith.server.modules.crud.entity.CrudModuleEntityService;
import com.appsmith.server.packages.crud.CrudPackageService;
import com.appsmith.server.packages.permissions.PackagePermissionChecker;
import com.appsmith.server.services.FeatureFlagService;
import com.appsmith.server.services.SessionUserService;
import com.appsmith.server.services.UserService;
import com.appsmith.server.services.WorkspaceService;
import com.appsmith.server.solutions.EnvironmentPermission;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
@DirtiesContext
public class CrudModuleServiceTest {

    @Autowired
    PackagePermissionChecker packagePermissionChecker;

    @Autowired
    CrudPackageService crudPackageService;

    @Autowired
    CrudModuleService crudModuleService;

    @Autowired
    CrudModuleEntityService crudModuleEntityService;

    @SpyBean
    UserService userService;

    @Autowired
    WorkspaceService workspaceService;

    @MockBean
    PluginExecutorHelper pluginExecutorHelper;

    @Autowired
    SessionUserService sessionUserService;

    @Autowired
    EnvironmentPermission environmentPermission;

    @SpyBean
    FeatureFlagService featureFlagService;

    @SpyBean
    CommonConfig commonConfig;

    @SpyBean
    ModuleInstancePermissionChecker moduleInstancePermissionChecker;

    static String workspaceId;
    static String defaultEnvironmentId;
    static String packageId;
    static PackageDTO testPackage = null;

    @BeforeEach
    @WithUserDetails(value = "api_user")
    public void setup() {

        User currentUser = sessionUserService.getCurrentUser().block();
        if (!currentUser.getEmail().equals("api_user")) {
            // Don't do any setups
            return;
        }
        Mockito.when(featureFlagService.check(eq(FeatureFlagEnum.license_audit_logs_enabled)))
                .thenReturn(Mono.just(TRUE));

        doReturn(FALSE).when(commonConfig).isCloudHosting();

        Mockito.when(featureFlagService.check(eq(FeatureFlagEnum.release_query_module_enabled)))
                .thenReturn(Mono.just(TRUE));

        Mockito.when(pluginExecutorHelper.getPluginExecutor(Mockito.any()))
                .thenReturn(Mono.just(new MockPluginExecutor()));
        User apiUser = userService.findByEmail("api_user").block();

        Workspace toCreate = new Workspace();
        toCreate.setName("ApplicationServiceTest");

        if (workspaceId == null) {
            Workspace workspace =
                    workspaceService.create(toCreate, apiUser, Boolean.FALSE).block();
            workspaceId = workspace.getId();

            defaultEnvironmentId = workspaceService
                    .getDefaultEnvironmentId(workspaceId, environmentPermission.getExecutePermission())
                    .block();
        }
        setupTestPackage();
    }

    public void setupTestPackage() {
        if (testPackage == null) {
            PackageDTO newPackage = new PackageDTO();
            newPackage.setName(UUID.randomUUID().toString());
            newPackage.setColor("#C2DAF0");
            newPackage.setIcon("rupee");

            testPackage =
                    crudPackageService.createPackage(newPackage, workspaceId).block();
            packageId = testPackage.getId();
        }
    }

    @WithUserDetails(value = "api_user")
    @Test
    public void testCreateModuleWithValidInput() {
        ModuleDTO moduleDTO = new ModuleDTO();
        moduleDTO.setName("GetUsersModule");
        moduleDTO.setType(ModuleType.QUERY_MODULE);
        moduleDTO.setPackageId(packageId);

        ModuleActionDTO moduleActionDTO = new ModuleActionDTO();

        moduleDTO.setEntity(moduleActionDTO);

        Mono<ModuleDTO> moduleMono = crudModuleService.createModule(moduleDTO);

        StepVerifier.create(moduleMono)
                .assertNext(createdModule -> {
                    assertThat(createdModule.getId()).isNotEmpty();
                    assertThat(createdModule.getName()).isEqualTo(moduleDTO.getName());
                })
                .verifyComplete();
    }

    @WithUserDetails(value = "api_user")
    @Test
    public void testUpdateModuleWithValidInput() {
        ModuleDTO moduleDTO = new ModuleDTO();
        moduleDTO.setName("GetUsersModuleV2");
        moduleDTO.setType(ModuleType.QUERY_MODULE);
        moduleDTO.setPackageId(packageId);

        ModuleActionDTO moduleActionDTO = new ModuleActionDTO();

        moduleDTO.setEntity(moduleActionDTO);

        Mono<ModuleDTO> moduleMono = crudModuleService.createModule(moduleDTO);
        AtomicReference<String> moduleRef = new AtomicReference<>();

        StepVerifier.create(moduleMono)
                .assertNext(createdModule -> {
                    assertThat(createdModule.getId()).isNotEmpty();
                    moduleRef.set(createdModule.getId());
                    assertThat(createdModule.getName()).isEqualTo(moduleDTO.getName());
                })
                .verifyComplete();

        // Update name and inputs
        moduleDTO.setName("GetAllUsersModule");
        moduleDTO.setInputs(Map.of(
                "minAge", new ModuleInput("minAge", "0"),
                "gender", new ModuleInput("gender", "all")));
        Mono<ModuleDTO> updateModuleMono = crudModuleService.updateModule(moduleDTO, moduleRef.get());

        StepVerifier.create(updateModuleMono)
                .assertNext(updatedModule -> {
                    assertThat(updatedModule.getName()).isEqualTo(moduleDTO.getName());
                    assertThat(updatedModule.getInputs()).isNotEmpty();
                    assertThat(updatedModule.getInputs().size()).isEqualTo(2);
                })
                .verifyComplete();
    }

    @WithUserDetails(value = "api_user")
    @Test
    public void testDeleteModuleWithNoModuleInstancesShouldDeleteModule() {
        ModuleDTO moduleDTO = new ModuleDTO();
        moduleDTO.setName("DeleteModuleTest");
        moduleDTO.setType(ModuleType.QUERY_MODULE);
        moduleDTO.setPackageId(packageId);

        ModuleActionDTO moduleActionDTO = new ModuleActionDTO();

        moduleDTO.setEntity(moduleActionDTO);

        Mono<ModuleDTO> moduleMono = crudModuleService.createModule(moduleDTO);
        AtomicReference<String> moduleIdRef = new AtomicReference<>();

        StepVerifier.create(moduleMono)
                .assertNext(createdModule -> {
                    assertThat(createdModule.getId()).isNotEmpty();
                    moduleIdRef.set(createdModule.getId());
                    assertThat(createdModule.getName()).isEqualTo(moduleDTO.getName());
                })
                .verifyComplete();

        Mono<ModuleDTO> deletedModuleMono = crudModuleService.deleteModule(moduleIdRef.get());

        StepVerifier.create(deletedModuleMono)
                .assertNext(deletedModule -> {
                    assertThat(deletedModule.getId()).isNotEmpty();
                })
                .verifyComplete();
    }

    @WithUserDetails(value = "api_user")
    @Test
    public void testDeleteModuleWithModuleInstancesShouldNotDeleteModule() {

        ModuleDTO moduleDTO = new ModuleDTO();
        moduleDTO.setName("CannotDeleteModuleTest");
        moduleDTO.setType(ModuleType.QUERY_MODULE);
        moduleDTO.setPackageId(packageId);

        ModuleActionDTO moduleActionDTO = new ModuleActionDTO();

        moduleDTO.setEntity(moduleActionDTO);

        Mono<ModuleDTO> moduleMono = crudModuleService.createModule(moduleDTO);
        AtomicReference<String> moduleIdRef = new AtomicReference<>();

        StepVerifier.create(moduleMono)
                .assertNext(createdModule -> {
                    assertThat(createdModule.getId()).isNotEmpty();
                    moduleIdRef.set(createdModule.getId());
                    assertThat(createdModule.getName()).isEqualTo(moduleDTO.getName());
                })
                .verifyComplete();

        Mockito.doReturn(Mono.just(Long.valueOf(1)))
                .when(moduleInstancePermissionChecker)
                .getModuleInstanceCountByModuleId(moduleIdRef.get());

        Mono<ModuleDTO> deletedModuleMono = crudModuleService.deleteModule(moduleIdRef.get());

        StepVerifier.create(deletedModuleMono)
                .expectErrorMatches(throwable -> {
                    assertThat(throwable.getMessage())
                            .isEqualTo("Module cannot be deleted since it has 1 module instance(s) using it.");
                    return true;
                })
                .verify();
    }

    @WithUserDetails(value = "api_user")
    @Test
    public void testGetEntitiesShouldReturnAtLeastOneEntity() {

        ModuleDTO moduleDTO = new ModuleDTO();
        moduleDTO.setName("PublicEntityTestModule");
        moduleDTO.setType(ModuleType.QUERY_MODULE);
        moduleDTO.setPackageId(packageId);

        ModuleActionDTO moduleActionDTO = new ModuleActionDTO();

        moduleDTO.setEntity(moduleActionDTO);

        Mono<ModuleDTO> moduleMono = crudModuleService.createModule(moduleDTO);
        AtomicReference<String> moduleIdRef = new AtomicReference<>();

        StepVerifier.create(moduleMono)
                .assertNext(createdModule -> {
                    assertThat(createdModule.getId()).isNotEmpty();
                    moduleIdRef.set(createdModule.getId());
                    assertThat(createdModule.getName()).isEqualTo(moduleDTO.getName());
                })
                .verifyComplete();

        Mono<List<ModuleConsumable>> moduleActionsMono = crudModuleEntityService.getModuleActions(moduleIdRef.get());

        StepVerifier.create(moduleActionsMono)
                .assertNext(moduleConsumables -> {
                    assertThat(moduleConsumables).isNotNull();
                    assertThat(moduleConsumables).size().isGreaterThanOrEqualTo(1);
                })
                .verifyComplete();
    }

    @WithUserDetails(value = "api_user")
    @Test
    public void testDeletePackageShouldPassWhenNoModuleHasAnyInstances() {
        final PackageDTO aPackage = new PackageDTO();
        aPackage.setName("PackagePublishableEntitiesNegativeTest");
        aPackage.setColor("#C2DAF0");
        aPackage.setIcon("rupee");

        AtomicReference<String> packageId = new AtomicReference<>();
        AtomicReference<PackageDTO> testPackageRef = new AtomicReference<>();

        // create package
        Mono<PackageDTO> firstPackageMono = crudPackageService.createPackage(aPackage, workspaceId);

        StepVerifier.create(firstPackageMono)
                .assertNext(createdPackage -> {
                    assertThat(createdPackage.getId()).isNotEmpty();
                    packageId.set(createdPackage.getId());
                    testPackageRef.set(createdPackage);
                    assertThat(createdPackage.getName()).isEqualTo(aPackage.getName());
                })
                .verifyComplete();

        ModuleDTO moduleDTO = new ModuleDTO();
        moduleDTO.setName("Module1");
        moduleDTO.setType(ModuleType.QUERY_MODULE);
        moduleDTO.setPackageId(packageId.get());

        ModuleActionDTO moduleActionDTO = new ModuleActionDTO();

        moduleDTO.setEntity(moduleActionDTO);

        Mono<ModuleDTO> moduleMono = crudModuleService.createModule(moduleDTO);
        AtomicReference<String> moduleIdRef = new AtomicReference<>();

        StepVerifier.create(moduleMono)
                .assertNext(createdModule -> {
                    assertThat(createdModule.getId()).isNotEmpty();
                    moduleIdRef.set(createdModule.getId());
                    assertThat(createdModule.getName()).isEqualTo(moduleDTO.getName());
                })
                .verifyComplete();

        ModuleDTO anotherModuleDTO = new ModuleDTO();
        anotherModuleDTO.setName("Module2");
        anotherModuleDTO.setType(ModuleType.QUERY_MODULE);
        anotherModuleDTO.setPackageId(packageId.get());

        ModuleActionDTO anotherModuleActionDTO = new ModuleActionDTO();

        anotherModuleDTO.setEntity(anotherModuleActionDTO);

        Mono<ModuleDTO> anotherModuleMono = crudModuleService.createModule(anotherModuleDTO);

        StepVerifier.create(anotherModuleMono)
                .assertNext(createdModule -> {
                    assertThat(createdModule.getId()).isNotEmpty();
                    assertThat(createdModule.getName()).isEqualTo(anotherModuleDTO.getName());
                })
                .verifyComplete();

        Mono<PackageDTO> deletePackageMono = crudPackageService.deletePackage(packageId.get());

        StepVerifier.create(deletePackageMono)
                .assertNext(deletedPackage -> {
                    assertThat(deletedPackage.getId()).isNotNull();
                })
                .verifyComplete();

        Mono<List<Module>> allModulesMono =
                crudModuleService.getAllModules(packageId.get()).collectList();

        StepVerifier.create(allModulesMono)
                .assertNext(allModules -> {
                    assertThat(allModules.size()).isEqualTo(0);
                })
                .verifyComplete();
    }

    @WithUserDetails(value = "api_user")
    @Test
    public void testDeletePackageShouldFailWhenModuleHasInstances() {
        final PackageDTO aPackage = new PackageDTO();
        aPackage.setName("PackagePublishableEntitiesNegativeTest");
        aPackage.setColor("#C2DAF0");
        aPackage.setIcon("rupee");

        AtomicReference<String> packageId = new AtomicReference<>();
        AtomicReference<PackageDTO> testPackageRef = new AtomicReference<>();

        // create package
        Mono<PackageDTO> firstPackageMono = crudPackageService.createPackage(aPackage, workspaceId);

        StepVerifier.create(firstPackageMono)
                .assertNext(createdPackage -> {
                    assertThat(createdPackage.getId()).isNotEmpty();
                    packageId.set(createdPackage.getId());
                    testPackageRef.set(createdPackage);
                    assertThat(createdPackage.getName()).isEqualTo(aPackage.getName());
                })
                .verifyComplete();

        ModuleDTO moduleDTO = new ModuleDTO();
        moduleDTO.setName("Module1");
        moduleDTO.setType(ModuleType.QUERY_MODULE);
        moduleDTO.setPackageId(packageId.get());

        ModuleActionDTO moduleActionDTO = new ModuleActionDTO();

        moduleDTO.setEntity(moduleActionDTO);

        Mono<ModuleDTO> moduleMono = crudModuleService.createModule(moduleDTO);
        AtomicReference<String> moduleIdRef = new AtomicReference<>();

        StepVerifier.create(moduleMono)
                .assertNext(createdModule -> {
                    assertThat(createdModule.getId()).isNotEmpty();
                    moduleIdRef.set(createdModule.getId());
                    assertThat(createdModule.getName()).isEqualTo(moduleDTO.getName());
                })
                .verifyComplete();

        ModuleDTO anotherModuleDTO = new ModuleDTO();
        anotherModuleDTO.setName("Module2");
        anotherModuleDTO.setType(ModuleType.QUERY_MODULE);
        anotherModuleDTO.setPackageId(packageId.get());

        ModuleActionDTO anotherModuleActionDTO = new ModuleActionDTO();

        anotherModuleDTO.setEntity(anotherModuleActionDTO);

        Mono<ModuleDTO> anotherModuleMono = crudModuleService.createModule(anotherModuleDTO);

        StepVerifier.create(anotherModuleMono)
                .assertNext(createdModule -> {
                    assertThat(createdModule.getId()).isNotEmpty();
                    assertThat(createdModule.getName()).isEqualTo(anotherModuleDTO.getName());
                })
                .verifyComplete();

        Mockito.doReturn(Mono.just(Long.valueOf(1)))
                .when(moduleInstancePermissionChecker)
                .getModuleInstanceCountByModuleId(moduleIdRef.get());

        Mono<PackageDTO> deletePackageMono = crudPackageService.deletePackage(packageId.get());

        StepVerifier.create(deletePackageMono)
                .expectErrorMatches(throwable -> {
                    assertThat(throwable.getMessage())
                            .isEqualTo("Module cannot be deleted since it has 1 module instance(s) using it.");
                    return true;
                })
                .verify();

        Mono<List<Module>> allModulesMono =
                crudModuleService.getAllModules(packageId.get()).collectList();

        StepVerifier.create(allModulesMono)
                .assertNext(allModules -> {
                    assertThat(allModules.size()).isEqualTo(2);
                })
                .verifyComplete();
    }
}
