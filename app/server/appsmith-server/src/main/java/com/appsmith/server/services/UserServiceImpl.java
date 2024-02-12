package com.appsmith.server.services;

import com.appsmith.external.services.EncryptionService;
import com.appsmith.server.configurations.CommonConfig;
import com.appsmith.server.configurations.EmailConfig;
import com.appsmith.server.helpers.UserServiceHelper;
import com.appsmith.server.helpers.UserUtils;
import com.appsmith.server.notifications.EmailSender;
import com.appsmith.server.ratelimiting.RateLimitService;
import com.appsmith.server.repositories.UserRepository;
import com.appsmith.server.repositories.cakes.ApplicationRepositoryCake;
import com.appsmith.server.repositories.cakes.EmailVerificationTokenRepositoryCake;
import com.appsmith.server.repositories.cakes.PasswordResetTokenRepositoryCake;
import com.appsmith.server.repositories.cakes.UserRepositoryCake;
import com.appsmith.server.services.ce_compatible.UserServiceCECompatibleImpl;
import com.appsmith.server.solutions.PolicySolution;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.scheduler.Scheduler;

@Slf4j
@Service
public class UserServiceImpl extends UserServiceCECompatibleImpl implements UserService {

    public UserServiceImpl(
            Scheduler scheduler,
            Validator validator,
            MongoConverter mongoConverter,
            ReactiveMongoTemplate reactiveMongoTemplate,
            UserRepository repositoryDirect,
            UserRepositoryCake repository,
            WorkspaceService workspaceService,
            AnalyticsService analyticsService,
            SessionUserService sessionUserService,
            PasswordResetTokenRepositoryCake passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            EmailSender emailSender,
            ApplicationRepositoryCake applicationRepository,
            PolicySolution policySolution,
            CommonConfig commonConfig,
            EmailConfig emailConfig,
            EncryptionService encryptionService,
            UserDataService userDataService,
            TenantService tenantService,
            PermissionGroupService permissionGroupService,
            UserUtils userUtils,
            EmailVerificationTokenRepositoryCake emailVerificationTokenRepository,
            EmailService emailService,
            RateLimitService rateLimitService,
            PACConfigurationService pacConfigurationService,
            UserServiceHelper userServiceHelper) {
        super(
                scheduler,
                validator,
                mongoConverter,
                reactiveMongoTemplate,
                repositoryDirect,
                repository,
                workspaceService,
                analyticsService,
                sessionUserService,
                passwordResetTokenRepository,
                passwordEncoder,
                commonConfig,
                encryptionService,
                userDataService,
                tenantService,
                userUtils,
                emailVerificationTokenRepository,
                emailService,
                rateLimitService,
                pacConfigurationService,
                userServiceHelper);
    }
}
