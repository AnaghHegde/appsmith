package com.appsmith.server.repositories.ce;

import com.appsmith.external.models.Datasource;
import com.appsmith.server.acl.AclPermission;
import com.appsmith.server.repositories.BaseAppsmithRepositoryImpl;
import com.appsmith.server.repositories.CacheableRepositoryHelper;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.springframework.data.mongodb.core.query.Criteria.where;

public class CustomDatasourceRepositoryCEImpl extends BaseAppsmithRepositoryImpl<Datasource>
        implements CustomDatasourceRepositoryCE {

    public CustomDatasourceRepositoryCEImpl(
            ReactiveMongoOperations mongoOperations,
            MongoConverter mongoConverter,
            CacheableRepositoryHelper cacheableRepositoryHelper) {
        super(mongoOperations, mongoConverter, cacheableRepositoryHelper);
    }

    // @Override
    @Deprecated
    public List<Datasource> findAllByWorkspaceId(String workspaceId, AclPermission permission) {
        return Collections.emptyList(); /*
        Criteria workspaceIdCriteria =
                where(fieldName(QDatasource.datasource.workspaceId)).is(workspaceId);
        Sort sort = Sort.by(fieldName(QDatasource.datasource.name));
        return queryAll()
                .criteria(workspaceIdCriteria)
                .permission(permission)
                .sort(sort)
                .submit();*/
    }

    // @Override
    public List<Datasource> findAllByWorkspaceId(Long workspaceId /*, Optional<AclPermission> permission*/) {
        return Collections.emptyList(); /*
        Criteria workspaceIdCriteria =
                where(fieldName(QDatasource.datasource.workspaceId)).is(workspaceId);
        return queryAll()
                .criteria(workspaceIdCriteria)
                .permission(permission.orElse(null))
                .sort(Sort.by(fieldName(QDatasource.datasource.name)))
                .submit();*/
    }

    @Override
    @Deprecated
    public Datasource findByNameAndWorkspaceId(String name, String workspaceId, AclPermission aclPermission) {
        return null; /*
                     Criteria nameCriteria = where("name").is(name);
                     Criteria workspaceIdCriteria =
                             where("workspaceId").is(workspaceId);
                     return queryOne(List.of(nameCriteria, workspaceIdCriteria), aclPermission);*/
    }

    @Override
    public Datasource findByNameAndWorkspaceId(String name, String workspaceId, Optional<AclPermission> aclPermission) {
        return null; /*
                     Criteria nameCriteria = where("name").is(name);
                     Criteria workspaceIdCriteria =
                             where("workspaceId").is(workspaceId);
                     return queryOne(List.of(nameCriteria, workspaceIdCriteria), null, aclPermission);*/
    }

    @Override
    public List<Datasource> findAllByIds(Set<String> ids, AclPermission permission) {
        Criteria idcriteria = where("id").in(ids);
        return queryAll().criteria(idcriteria).permission(permission).submit();
    }

    @Override
    public List<Datasource> findAllByIdsWithoutPermission(Set<String> ids, List<String> includeFields) {
        return Collections.emptyList(); /*
        Criteria idCriteria = where("id").in(ids);
        return queryAll().criteria(idCriteria).fields(includeFields).submit();*/
    }
}
