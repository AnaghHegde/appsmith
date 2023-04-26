package com.appsmith.server.services;

import com.appsmith.server.configurations.CloudServicesConfig;
import com.appsmith.server.dtos.ChatGenerationDTO;
import com.appsmith.server.dtos.ChatGenerationRequestDTO;
import com.appsmith.server.dtos.ChatGenerationResponseDTO;
import com.appsmith.server.dtos.LicenseValidationRequestDTO;
import com.appsmith.server.dtos.ResponseDTO;
import com.appsmith.server.enums.ChatGenerationType;
import com.appsmith.server.exceptions.AppsmithError;
import com.appsmith.server.exceptions.AppsmithException;
import com.appsmith.server.featureflags.FeatureFlagEnum;
import com.appsmith.server.solutions.LicenseValidator;
import com.appsmith.util.WebClientUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.codec.DecodingException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final FeatureFlagService featureFlagService;
    private final CloudServicesConfig cloudServicesConfig;
    private final TenantService tenantService;
    private final ConfigService configService;
    private final SessionUserService sessionUserService;

    private final LicenseValidator licenseValidator;

    @Override
    public Mono<ChatGenerationResponseDTO> generateCode(ChatGenerationDTO chatGenerationDTO, ChatGenerationType type) {
        Mono<Boolean> featureFlagMono = this.featureFlagService.check(FeatureFlagEnum.CHAT_AI)
                .filter(isAllowed -> isAllowed)
                .switchIfEmpty(Mono.error(new AppsmithException(AppsmithError.UNAUTHORIZED_ACCESS)));

        Mono<LicenseValidationRequestDTO> requestDTOMono = tenantService.getDefaultTenant()
                .flatMap(licenseValidator::populateLicenseValidationRequest);

        // TODO: Change this to get current tenant when multitenancy is introduced
        return Mono.zip(
                        featureFlagMono,
                        configService.getInstanceId(),
                        sessionUserService.getCurrentUser(),
                        requestDTOMono
                )
                .flatMap(tuple -> {
                    String instanceId = tuple.getT2();
                    String userId = tuple.getT3().getId();
                    LicenseValidationRequestDTO licenseValidationRequestDTO = tuple.getT4();
                    ChatGenerationRequestDTO chatGenerationRequestDTO =
                            new ChatGenerationRequestDTO(chatGenerationDTO, userId, instanceId, licenseValidationRequestDTO);
                    return WebClientUtils
                            .create(cloudServicesConfig.getBaseUrl() + "/api/v1/chat/chat-generation")
                            .post()
                            .uri(builder -> builder.queryParam("type", type).build())
                            .body(BodyInserters.fromValue(chatGenerationRequestDTO))
                            .exchangeToMono(clientResponse -> {
                                if (clientResponse.statusCode().is2xxSuccessful()) {
                                    return clientResponse.bodyToMono(new ParameterizedTypeReference<ResponseDTO<ChatGenerationResponseDTO>>() {
                                    });
                                } else {
                                    return clientResponse.createError();
                                }
                            })
                            .map(ResponseDTO::getData)
                            .onErrorMap(
                                    WebClientResponseException.class,
                                    e -> {
                                        ResponseDTO<ChatGenerationResponseDTO> responseDTO;
                                        try {
                                            responseDTO = e.getResponseBodyAs(new ParameterizedTypeReference<>() {
                                            });
                                        } catch (DecodingException | IllegalStateException e2) {
                                            return e;
                                        }
                                        if (responseDTO != null &&
                                                responseDTO.getResponseMeta() != null &&
                                                responseDTO.getResponseMeta().getError() != null) {
                                            return new AppsmithException(
                                                    AppsmithError.OPEN_AI_ERROR,
                                                    responseDTO.getResponseMeta().getError().getMessage());
                                        }
                                        return e;
                                    })
                            .onErrorMap(
                                    // Only map errors if we haven't already wrapped them into an AppsmithException
                                    e -> !(e instanceof AppsmithException),
                                    e -> new AppsmithException(AppsmithError.CLOUD_SERVICES_ERROR, e.getMessage())
                            );
                });
    }
}
