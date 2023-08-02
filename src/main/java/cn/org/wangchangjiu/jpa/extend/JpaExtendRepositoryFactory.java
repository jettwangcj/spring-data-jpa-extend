package cn.org.wangchangjiu.jpa.extend;

import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.repository.query.EscapeCharacter;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.lang.Nullable;

import javax.persistence.EntityManager;
import java.util.Optional;

/**
 * @Classname MyJpaRepositoryFactory
 * @Description
 * @Date 2023/7/31 21:01
 * @Created by wangchangjiu
 */
public class JpaExtendRepositoryFactory extends JpaRepositoryFactory {

    private final EntityManager entityManager;

    private final PersistenceProvider extractor;

    private EscapeCharacter escapeCharacter = EscapeCharacter.DEFAULT;


    /**
     * Creates a new {@link JpaRepositoryFactory}.
     *
     * @param entityManager must not be {@literal null}
     */
    JpaExtendRepositoryFactory(EntityManager entityManager) {
        super(entityManager);
        this.entityManager = entityManager;
        this.extractor = PersistenceProvider.fromEntityManager(entityManager);

    }


    @Override
    protected Optional<QueryLookupStrategy> getQueryLookupStrategy(@Nullable QueryLookupStrategy.Key key,
                                                                   QueryMethodEvaluationContextProvider evaluationContextProvider) {
        return Optional
                .of(JpaExtendQueryLookupStrategy.create(entityManager, key, extractor, evaluationContextProvider, escapeCharacter));
    }





}
