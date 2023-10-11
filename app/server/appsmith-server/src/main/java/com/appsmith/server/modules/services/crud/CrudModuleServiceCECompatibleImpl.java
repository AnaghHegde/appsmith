package com.appsmith.server.modules.services.crud;

import com.appsmith.server.dtos.ModuleDTO;
import com.appsmith.server.exceptions.AppsmithError;
import com.appsmith.server.exceptions.AppsmithException;
import com.appsmith.server.modules.services.base.BaseModuleServiceImpl;
import com.appsmith.server.repositories.ModuleRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class CrudModuleServiceCECompatibleImpl extends BaseModuleServiceImpl implements CrudModuleServiceCECompatible {
    public CrudModuleServiceCECompatibleImpl(ModuleRepository moduleRepository) {
        super(moduleRepository);
    }

    @Override
    public Mono<ModuleDTO> createModule(ModuleDTO moduleDTO) {
        return Mono.error(new AppsmithException(AppsmithError.UNSUPPORTED_OPERATION));
    }

    @Override
    public Mono<ModuleDTO> updateModule(ModuleDTO moduleResource, String moduleId) {
        return Mono.error(new AppsmithException(AppsmithError.UNSUPPORTED_OPERATION));
    }
}
