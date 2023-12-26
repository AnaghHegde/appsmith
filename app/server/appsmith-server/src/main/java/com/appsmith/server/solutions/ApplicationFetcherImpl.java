package com.appsmith.server.solutions;

import com.appsmith.server.helpers.ResponseUtils;
import com.appsmith.server.newpages.base.NewPageService;
import com.appsmith.server.repositories.cakes.ApplicationRepositoryCake;
import com.appsmith.server.services.SessionUserService;
import com.appsmith.server.services.UserDataService;
import com.appsmith.server.services.UserService;
import com.appsmith.server.services.UserWorkspaceService;
import com.appsmith.server.services.WorkspaceService;
import com.appsmith.server.solutions.ce.ApplicationFetcherCEImpl;
import org.springframework.stereotype.Component;

@Component
public class ApplicationFetcherImpl extends ApplicationFetcherCEImpl implements ApplicationFetcher {

    public ApplicationFetcherImpl(
            SessionUserService sessionUserService,
            UserService userService,
            UserDataService userDataService,
            WorkspaceService workspaceService,
            ApplicationRepositoryCake applicationRepository,
            ReleaseNotesService releaseNotesService,
            ResponseUtils responseUtils,
            NewPageService newPageService,
            UserWorkspaceService userWorkspaceService,
            WorkspacePermission workspacePermission,
            ApplicationPermission applicationPermission,
            PagePermission pagePermission) {

        super(
                sessionUserService,
                userService,
                userDataService,
                workspaceService,
                applicationRepository,
                releaseNotesService,
                responseUtils,
                newPageService,
                userWorkspaceService,
                workspacePermission,
                applicationPermission,
                pagePermission);
    }
}
