package com.appsmith.server.services;

import com.appsmith.server.repositories.cakes.ConfigRepositoryCake;
import com.appsmith.server.services.ce.ConfigServiceCEImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ConfigServiceImpl extends ConfigServiceCEImpl implements ConfigService {

    public ConfigServiceImpl(ConfigRepositoryCake repository) {
        super(repository);
    }
}
