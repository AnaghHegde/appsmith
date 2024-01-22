package com.appsmith.server.repositories.ce;

import com.appsmith.external.models.CreatorContextType;
import com.appsmith.server.acl.AclPermission;
import com.appsmith.server.domains.ActionCollection;
import com.appsmith.server.repositories.BaseAppsmithRepositoryImpl;
import com.appsmith.server.repositories.CacheableRepositoryHelper;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CustomActionCollectionRepositoryCEImpl extends BaseAppsmithRepositoryImpl<ActionCollection>
        implements CustomActionCollectionRepositoryCE {

    public CustomActionCollectionRepositoryCEImpl(
            ReactiveMongoOperations mongoOperations,
            MongoConverter mongoConverter,
            CacheableRepositoryHelper cacheableRepositoryHelper) {
        super(mongoOperations, mongoConverter, cacheableRepositoryHelper);
    }

    @Override
    @Deprecated
    public List<ActionCollection> findByApplicationId(String applicationId, AclPermission aclPermission, Sort sort) {
        return Collections.emptyList(); /*

        Criteria applicationCriteria = where(fieldName(QActionCollection.actionCollection.applicationId))
                .is(applicationId);

        return queryAll(List.of(applicationCriteria), aclPermission, sort);*/
    }

    @Override
    public List<ActionCollection> findByApplicationId(
            String applicationId, Optional<AclPermission> aclPermission, Optional<Sort> sort) {
        return Collections.emptyList(); /*

        Criteria applicationCriteria = where(fieldName(QActionCollection.actionCollection.applicationId)).is(applicationId);

        return queryAll(List.of(applicationCriteria), aclPermission, sort);*/
    }

    protected List<Criteria> getCriteriaForFindByApplicationIdAndViewMode(String applicationId, boolean viewMode) {
        return Collections.emptyList(); /*
        List<Criteria> criteria = new ArrayList<>();

        Criteria applicationCriterion = where(fieldName(QActionCollection.actionCollection.applicationId))
                .is(applicationId);
        criteria.add(applicationCriterion);

        if (Boolean.FALSE.equals(viewMode)) {
            // In case an action has been deleted in edit mode, but still exists in deployed mode, NewAction object
            // would exist. To handle this, only fetch non-deleted actions
            Criteria deletedCriterion = where(fieldName(QActionCollection.actionCollection.unpublishedCollection) + "."
-                            + fieldName(QActionCollection.actionCollection.unpublishedCollection.deletedAt))
                    .is(null);
            criteria.add(deletedCriterion);
        }
        return criteria;*/
    }

    @Override
    public List<ActionCollection> findByApplicationIdAndViewMode(
            String applicationId, boolean viewMode, AclPermission aclPermission) {
        return Collections.emptyList(); /*

        List<Criteria> criteria = this.getCriteriaForFindByApplicationIdAndViewMode(applicationId, viewMode);

        return queryAll(criteria, aclPermission);*/
    }

    protected List<Criteria> getCriteriaForFindAllActionCollectionsByNameDefaultPageIdsViewModeAndBranch(
            String branchName, boolean viewMode, String name, List<String> pageIds) {
        return Collections.emptyList(); /*
        /**
         * TODO : This function is called by get(params) to get all actions by params and hence
         * only covers criteria of few fields like page id, name, etc. Make this generic to cover
         * all possible fields
         * /
        List<Criteria> criteriaList = new ArrayList<>();

        if (!StringUtils.isEmpty(branchName)) {
            criteriaList.add(where(FieldName.DEFAULT_RESOURCES + "." + FieldName.BRANCH_NAME)
                    .is(branchName));
        }

        // Fetch published actions
        if (Boolean.TRUE.equals(viewMode)) {

            if (name != null) {
                Criteria nameCriteria = where(fieldName(QActionCollection.actionCollection.publishedCollection) + "."
-                            + fieldName(QActionCollection.actionCollection.publishedCollection.name))
                        .is(name);
                criteriaList.add(nameCriteria);
            }

            if (pageIds != null && !pageIds.isEmpty()) {
                String pageIdFieldPath = String.format(
                        "%s.%s.%s",
                        fieldName(QActionCollection.actionCollection.publishedCollection),
                        fieldName(QActionCollection.actionCollection.publishedCollection.defaultResources),
                        fieldName(QActionCollection.actionCollection.publishedCollection.pageId));
                Criteria pageCriteria = where(pageIdFieldPath).in(pageIds);
                criteriaList.add(pageCriteria);
            }
        }
        // Fetch unpublished actions
        else {

            if (name != null) {
                Criteria nameCriteria = where(fieldName(QActionCollection.actionCollection.unpublishedCollection) + "."
-                            + fieldName(QActionCollection.actionCollection.unpublishedCollection.name))
                        .is(name);
                criteriaList.add(nameCriteria);
            }

            if (pageIds != null && !pageIds.isEmpty()) {
                String pageIdFieldPath = String.format(
                        "%s.%s.%s",
                        fieldName(QActionCollection.actionCollection.unpublishedCollection),
                        fieldName(QActionCollection.actionCollection.unpublishedCollection.defaultResources),
                        fieldName(QActionCollection.actionCollection.unpublishedCollection.pageId));
                Criteria pageCriteria = where(pageIdFieldPath).in(pageIds);
                criteriaList.add(pageCriteria);
            }

            // In case an action has been deleted in edit mode, but still exists in deployed mode, NewAction object
            // would exist. To handle this, only fetch non-deleted actions
            Criteria deletedCriteria = where(fieldName(QActionCollection.actionCollection.unpublishedCollection) + "."
-                            + fieldName(QActionCollection.actionCollection.unpublishedCollection.deletedAt))
                    .is(null);
            criteriaList.add(deletedCriteria);
        }
        return criteriaList;*/
    }

    @Override
    public List<ActionCollection> findAllActionCollectionsByNameDefaultPageIdsViewModeAndBranch(
            String name,
            List<String> pageIds,
            boolean viewMode,
            String branchName,
            AclPermission aclPermission,
            Sort sort) {
        return Collections.emptyList(); /*
        List<Criteria> criteriaList = this.getCriteriaForFindAllActionCollectionsByNameDefaultPageIdsViewModeAndBranch(
                branchName, viewMode, name, pageIds);

        return queryAll(criteriaList, aclPermission, sort);*/
    }

    @Override
    public List<ActionCollection> findByPageId(String pageId, AclPermission aclPermission) {
        return Collections.emptyList(); /*
        String unpublishedPage = "unpublishedCollection" + "."
                + "pageId";
        String publishedPage = "publishedCollection" + "."
                + "pageId";

        Criteria pageCriteria = new Criteria()
                .orOperator(
                        where(unpublishedPage).is(pageId), where(publishedPage).is(pageId));

        return queryAll(List.of(pageCriteria), aclPermission);*/
    }

    @Override
    public List<ActionCollection> findByPageId(String pageId) {
        return this.findByPageId(pageId, null);
    }

    @Override
    public Optional<ActionCollection> findByBranchNameAndDefaultCollectionId(
            String branchName, String defaultCollectionId, AclPermission permission) {
        return Optional.empty(); /*
        final String defaultResources = fieldName(QActionCollection.actionCollection.defaultResources);
        Criteria defaultCollectionIdCriteria =
                where(defaultResources + "." + FieldName.COLLECTION_ID).is(defaultCollectionId);
        Criteria branchCriteria =
                where(defaultResources + "." + FieldName.BRANCH_NAME).is(branchName);
        return queryOne(List.of(defaultCollectionIdCriteria, branchCriteria), permission);*/
    }

    @Override
    public Optional<ActionCollection> findByGitSyncIdAndDefaultApplicationId(
            String defaultApplicationId, String gitSyncId, AclPermission permission) {
        return findByGitSyncIdAndDefaultApplicationId(defaultApplicationId, gitSyncId, Optional.ofNullable(permission));
    }

    @Override
    public Optional<ActionCollection> findByGitSyncIdAndDefaultApplicationId(
            String defaultApplicationId, String gitSyncId, Optional<AclPermission> permission) {
        return Optional.empty(); /*
        final String defaultResources = fieldName(QActionCollection.actionCollection.defaultResources);
        Criteria defaultAppIdCriteria =
                where(defaultResources + "." + FieldName.APPLICATION_ID).is(defaultApplicationId);
        Criteria gitSyncIdCriteria = where(FieldName.GIT_SYNC_ID).is(gitSyncId);
        return queryFirst(List.of(defaultAppIdCriteria, gitSyncIdCriteria), permission);*/
    }

    @Override
    public List<ActionCollection> findByDefaultApplicationId(
            String defaultApplicationId, Optional<AclPermission> permission) {
        return Collections.emptyList(); /*
        final String defaultResources = fieldName(QActionCollection.actionCollection.defaultResources);
        Criteria defaultAppIdCriteria =
                where(defaultResources + "." + FieldName.APPLICATION_ID).is(defaultApplicationId);
        return queryAll(List.of(defaultAppIdCriteria), permission);*/
    }

    @Override
    public List<ActionCollection> findByPageIds(List<String> pageIds, AclPermission permission) {
        return Collections.emptyList(); /*
        Criteria pageIdCriteria = where(fieldName(QActionCollection.actionCollection.unpublishedCollection) + "."
                             + fieldName(QActionCollection.actionCollection.unpublishedCollection.pageId))
                .in(pageIds);
        return queryAll(List.of(pageIdCriteria), permission);*/
    }

    @Override
    public List<ActionCollection> findByPageIds(List<String> pageIds, Optional<AclPermission> permission) {
        return Collections.emptyList(); /*
        Criteria pageIdCriteria = where(fieldName(QActionCollection.actionCollection.unpublishedCollection) + "."
                        + fieldName(QActionCollection.actionCollection.unpublishedCollection.pageId))
                .in(pageIds);
        return queryAll(List.of(pageIdCriteria), permission);*/
    }

    @Override
    public List<ActionCollection> findAllByApplicationIds(List<String> applicationIds, List<String> includeFields) {
        return Collections.emptyList(); /*
        Criteria applicationCriteria = Criteria.where(FieldName.APPLICATION_ID).in(applicationIds);
        return queryAll(List.of(applicationCriteria), includeFields, null, null, NO_RECORD_LIMIT);*/
    }

    @Override
    public List<ActionCollection> findAllUnpublishedActionCollectionsByContextIdAndContextType(
            String contextId, CreatorContextType contextType, AclPermission permission) {
        return Collections.emptyList(); /*
        String contextIdPath = "unpublishedCollection" + "." + "pageId";
        String contextTypePath = "unpublishedCollection" + "." + "contextType";
        Criteria contextIdAndContextTypeCriteria =
                where(contextIdPath).is(contextId).and(contextTypePath).is(contextType);
        return queryAll(List.of(contextIdAndContextTypeCriteria), Optional.ofNullable(permission));*/
    }

    @Override
    public List<ActionCollection> findAllPublishedActionCollectionsByContextIdAndContextType(
            String contextId, CreatorContextType contextType, AclPermission permission) {
        return Collections.emptyList(); /*
        String contextIdPath = "publishedCollection" + "." + "pageId";
        String contextTypePath = "publishedCollection" + "." + "contextType";
        Criteria contextIdAndContextTypeCriteria =
                where(contextIdPath).is(contextId).and(contextTypePath).is(contextType);
        return queryAll(List.of(contextIdAndContextTypeCriteria), Optional.ofNullable(permission));*/
    }

    @Override
    public Flux<ActionCollection> findByPageIdAndViewMode(String pageId, boolean viewMode, AclPermission permission) {
        List<Criteria> criteria = new ArrayList<>();

        Criteria pageCriterion;

        // Fetch published action collections
        if (Boolean.TRUE.equals(viewMode)) {
            pageCriterion = where(completeFieldName(QActionCollection.actionCollection.publishedCollection.pageId))
                    .is(pageId);
            criteria.add(pageCriterion);
        }
        // Fetch unpublished action collections
        else {
            pageCriterion = where(completeFieldName(QActionCollection.actionCollection.unpublishedCollection.pageId))
                    .is(pageId);
            criteria.add(pageCriterion);

            // In case an action collection has been deleted in edit mode, but still exists in deployed mode,
            // ActionCollection object
            // would exist. To handle this, only fetch non-deleted actions
            Criteria deletedCriteria = where(
                            completeFieldName(QActionCollection.actionCollection.unpublishedCollection.deletedAt))
                    .is(null);
            criteria.add(deletedCriteria);
        }
        return queryAll(criteria, permission);
    }
}
