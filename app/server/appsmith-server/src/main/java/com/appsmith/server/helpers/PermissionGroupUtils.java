package com.appsmith.server.helpers;

import com.appsmith.server.constants.Appsmith;
import com.appsmith.server.domains.Config;
import com.appsmith.server.domains.PermissionGroup;
import com.appsmith.server.repositories.ConfigRepository;
import net.minidev.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Set;

import static com.appsmith.server.constants.FieldName.DEFAULT_PERMISSION_GROUP;

@Component
public class PermissionGroupUtils {
    private Set<String> autoCreatedPermissionGroupIds = null;
    private final ConfigRepository configRepository;

    public PermissionGroupUtils(ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    private Flux<Config> getAllConfigsWithAutoCreatedPermissionGroups() {
        return this.configRepository.findAllByNameIn(Appsmith.AUTO_CREATED_PERMISSION_GROUP);
    }

    private Mono<Set<String>> getAutoCreatedPermissionGroupIds() {
        if (this.autoCreatedPermissionGroupIds != null)
            return Mono.just(this.autoCreatedPermissionGroupIds);

        Set<String> tempSet = new HashSet<>();

        return this.getAllConfigsWithAutoCreatedPermissionGroups()
                .collectList()
                .flatMap(configs -> {
                    configs.forEach(config -> {
                        JSONObject jsonObject = config.getConfig();
                        if (jsonObject.containsKey(DEFAULT_PERMISSION_GROUP)
                                && StringUtils.hasLength(jsonObject.getAsString(DEFAULT_PERMISSION_GROUP)))
                            tempSet.add(jsonObject.getAsString(DEFAULT_PERMISSION_GROUP));
                    });
                    return Mono.just(tempSet);
                })
                .doOnNext(permissionGroupIdsSet -> autoCreatedPermissionGroupIds = permissionGroupIdsSet);
    }

    public Mono<Boolean> isAutoCreated(PermissionGroup permissionGroup) {
        return getAutoCreatedPermissionGroupIds().map(autoCreatedPermissionGroupIdSet ->
                autoCreatedPermissionGroupIdSet.contains(permissionGroup.getId())
                        || StringUtils.hasLength(permissionGroup.getDefaultWorkspaceId()));
    }

}
