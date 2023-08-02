package cn.org.wangchangjiu.jpa.extend;

import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.repository.query.DefaultJpaQueryMethodFactory;
import org.springframework.data.jpa.repository.query.EscapeCharacter;
import org.springframework.data.jpa.repository.query.JpaQueryMethodFactory;
import org.springframework.data.jpa.repository.query.QueryRewriterProvider;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.lang.Nullable;

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

    private JpaQueryMethodFactory queryMethodFactory;

    private QueryRewriterProvider queryRewriterProvider;


    /**
     * Creates a new {@link JpaRepositoryFactory}.
     *
     * @param entityManager must not be {@literal null}
     */
    JpaExtendRepositoryFactory(EntityManager entityManager) {
        super(entityManager);
        this.entityManager = entityManager;
        this.extractor = PersistenceProvider.fromEntityManager(entityManager);
        this.queryMethodFactory = new DefaultJpaQueryMethodFactory(extractor);
        this.queryRewriterProvider = QueryRewriterProvider.simple();

    }


    @Override
    protected Optional<QueryLookupStrategy> getQueryLookupStrategy(@Nullable QueryLookupStrategy.Key key,
                                                                   QueryMethodEvaluationContextProvider evaluationContextProvider) {
        return Optional
                .of(JpaExtendQueryLookupStrategy.create(entityManager, queryMethodFactory, key, evaluationContextProvider,
                        queryRewriterProvider, escapeCharacter));
    }





}
