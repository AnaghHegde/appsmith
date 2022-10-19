package com.appsmith.server.controllers;


import com.appsmith.server.constants.Url;
import com.appsmith.server.controllers.ce.EnvironmentControllerCE;
import com.appsmith.server.dtos.EnvironmentDTO;
import com.appsmith.server.dtos.ResponseDTO;
import com.appsmith.server.domains.Environment;
import com.appsmith.server.services.EnvironmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@Slf4j
@RequestMapping(Url.ENVIRONMENT_URL)
public class EnvironmentController extends EnvironmentControllerCE {

    private final EnvironmentService environmentService;
    @Autowired
    public EnvironmentController (EnvironmentService environmentService) {
        super(environmentService);
        this.environmentService = environmentService;
    }
    @GetMapping("/{envId}")
    public Mono<ResponseDTO<EnvironmentDTO>> getEnvironmentById(@PathVariable String envId) {
        log.debug("Going to fetch environment from environment controller with environment id {}", envId);

        return environmentService.findEnvironmentByEnvironmentId(envId).map(environmentDTO -> {
            return new ResponseDTO<>(HttpStatus.OK.value(), environmentDTO, null);
        });
    }


    @GetMapping("/workspace/{workspaceId}")
    public Mono<ResponseDTO<List<EnvironmentDTO>>> getEnvironmentByWorkspaceId(@PathVariable String workspaceId) {
        log.debug("Going to fetch environments from environment controller with workspace id {}", workspaceId);

        return environmentService.findEnvironmentByWorkspaceId(workspaceId).collectList().map(environmentDTOList -> {
            return new ResponseDTO<>(HttpStatus.OK.value(), environmentDTOList, null);
        });
    }

}
