package com.appsmith.server.repositories;

import com.appsmith.external.models.BaseDomain;
import com.appsmith.server.blasphemy.DBConnection;
import jakarta.persistence.EntityManager;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.repository.support.SimpleReactiveMongoRepository;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * This repository implementation is the base class that will be used by Spring Data running all the default JPA queries.
 * We override the default implementation {@link SimpleReactiveMongoRepository} to filter out records marked with
 * deleted=true.
 * To enable this base implementation, it MUST be set in the annotation @EnableReactiveMongoRepositories.repositoryBaseClass.
 * This is currently defined in {@link com.appsmith.server.configurations.MongoConfig} (liable to change in the future).
 * <p>
 * An implementation like this can also be used to set default query parameters based on the user's role and permissions
 * to filter out data that they are allowed to see. This is will be implemented with ACL.
 *
 * @param <T>  The domain class that extends {@link BaseDomain}. This is required because we use default fields in
 *             {@link BaseDomain} such as `deleted`
 * @param <ID> The ID field that extends Serializable interface
 *             <p>
 *             In case you are wondering why we have two different repository implementation classes i.e.
 *             BaseRepositoryImpl.java and BaseAppsmithRepositoryCEImpl.java, Arpit's comments on this might be helpful:
 *             ```
 *             BaseRepository is required for running any JPA queries. This doesn’t invoke any ACL permissions. This is used when
 *             we wish to fetch data from the DB without ACL. For eg, Fetching a user by username during login
 *             Usage example:
 *             ActionCollectionRepositoryCE extends BaseRepository to power JPA queries using the ReactiveMongoRepository.
 *             AppsmithRepository is the one that we should use by default (unless the use case demands that we don’t need ACL).
 *             It is implemented by BaseAppsmithRepositoryCEImpl and BaseAppsmithRepositoryImpl. This interface allows us to
 *             define custom Mongo queries by including the delete functionality & ACL permissions.
 *             Usage example:
 *             CustomActionCollectionRepositoryCE extends AppsmithRepository and then implements the functions defined there.
 *             I agree that the naming is a little confusing. Open to hearing better naming suggestions so that we can improve
 *             the understanding of these interfaces.
 *             ```
 *             Ref: https://theappsmith.slack.com/archives/CPQNLFHTN/p1669100205502599?thread_ts=1668753437.497369&cid=CPQNLFHTN
 */
@Slf4j
public class BaseRepositoryImpl<T extends BaseDomain, ID extends Serializable> extends SimpleJpaRepository<T, ID>
/*implements BaseRepository<T, ID>*/ {

    protected final @NonNull JpaEntityInformation<T, ID> entityInformation;
    protected final @NonNull EntityManager entityManager;

    protected final DBConnection dbConnection;

    public BaseRepositoryImpl(
            @NonNull JpaEntityInformation<T, ID> entityInformation, @NonNull EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityInformation = entityInformation;
        this.entityManager = entityManager;
        this.dbConnection = DBConnection.getInstance();
    }

    private Criteria getIdCriteria(Object id) {
        return null; /*
                     return where(entityInformation.getIdAttribute()).is(id); //*/
    }

    @Override
    public Optional<T> findById(ID id) {
        throw new ex.Marker("findById"); /*
        Assert.notNull(id, "The given id must not be null!");
        Query query = new Query(getIdCriteria(id));
        query.addCriteria(notDeleted());

        return mongoOperations
                .query(entityInformation.getJavaType())
                .inCollection(entityInformation.getCollectionName())
                .matching(query)
                .one(); //*/
    }

    @Override
    public List<T> findAll(Example example, Sort sort) {
        throw new ex.Marker("findAll"); /*
        Assert.notNull(example, "Sample must not be null!");
        Assert.notNull(sort, "Sort must not be null!");

        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .map(auth -> auth.getPrincipal())
                .flatMapMany(principal -> {
                    Criteria criteria = new Criteria()
                            .andOperator(
                                    // Older check for deleted
                                    new Criteria()
                                            .orOperator(
                                                    where(FieldName.DELETED).exists(false),
                                                    where(FieldName.DELETED).is(false)),
                                    // New check for deleted
                                    new Criteria()
                                            .orOperator(
                                                    where(FieldName.DELETED_AT).exists(false),
                                                    where(FieldName.DELETED_AT).is(null)),
                                    // Set the criteria as the example
                                    new Criteria().alike(example));

                    Query query = new Query(criteria)
                            .collation(entityInformation.getCollation()) //
                            .with(sort);

                    return mongoOperations.find(query, example.getProbeType(), entityInformation.getCollectionName());
                }); //*/
    }
}
