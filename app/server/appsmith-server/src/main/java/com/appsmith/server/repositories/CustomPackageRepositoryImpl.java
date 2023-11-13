package com.appsmith.server.repositories;

import com.appsmith.server.acl.AclPermission;
import com.appsmith.server.domains.Package;
import com.appsmith.server.domains.QPackage;
import com.mongodb.client.result.UpdateResult;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CustomPackageRepositoryImpl extends BaseAppsmithRepositoryImpl<Package>
        implements CustomPackageRepository {

    public CustomPackageRepositoryImpl(
            ReactiveMongoOperations mongoOperations,
            MongoConverter mongoConverter,
            CacheableRepositoryHelper cacheableRepositoryHelper) {
        super(mongoOperations, mongoConverter, cacheableRepositoryHelper);
    }

    @Override
    public Flux<Package> findAllUserPackages(AclPermission permission) {
        return queryAll(List.of(), Optional.of(permission));
    }

    @Override
    public Flux<Package> findAllConsumablePackages(String workspaceId, AclPermission permission) {
        return findAllSourcePackages(workspaceId, permission)
                .flatMap(sourcePackage -> Mono.just(Tuples.of(sourcePackage.getId(), sourcePackage.getVersion())))
                .collectList()
                .flatMapMany(allTuple2s -> {
                    if (allTuple2s.isEmpty()) {
                        return Flux.empty();
                    }
                    List<Criteria> allCriteria = allTuple2s.stream()
                            .map(tuple2 -> Criteria.where(fieldName(QPackage.package$.srcPackageId))
                                    .is(tuple2.getT1())
                                    .and(fieldName(QPackage.package$.version))
                                    .is(tuple2.getT2()))
                            .collect(Collectors.toList());

                    Criteria finalCriteria = new Criteria().orOperator(allCriteria.toArray(new Criteria[0]));

                    return queryAll(List.of(finalCriteria), Optional.of(permission));
                });
    }

    @NotNull private Flux<Package> findAllSourcePackages(String workspaceId, AclPermission permission) {
        Criteria sourcePackageCriteria = Criteria.where(fieldName(QPackage.package$.srcPackageId))
                .is(null)
                .and(fieldName(QPackage.package$.lastPublishedAt))
                .ne(null)
                .and(fieldName(QPackage.package$.workspaceId))
                .is(workspaceId);

        return queryAll(List.of(sourcePackageCriteria), Optional.of(permission));
    }

    @Override
    public Mono<UpdateResult> update(String id, Update updateObj, AclPermission permission) {
        return updateById(id, updateObj, permission);
    }
}
