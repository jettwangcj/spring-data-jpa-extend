package cn.org.wangchangjiu.jpa.extend;

import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.jpa.repository.query.*;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;

/**
 * @Classname MyQueryLookupStrategy
 * @Description
 * @Date 2023/7/31 21:02
 * @Created by wangchangjiu
 */
public class JpaExtendQueryLookupStrategy implements QueryLookupStrategy {

    private final EntityManager entityManager;

    private QueryLookupStrategy jpaQueryLookupStrategy;

    private  JpaQueryMethodFactory queryMethodFactory;

    public JpaExtendQueryLookupStrategy(EntityManager em, JpaQueryMethodFactory queryMethodFactory,
                                        @Nullable Key key, QueryMethodEvaluationContextProvider evaluationContextProvider,
                                        QueryRewriterProvider queryRewriterProvider, EscapeCharacter escape) {
        this.jpaQueryLookupStrategy = JpaQueryLookupStrategy.create(em, queryMethodFactory, key, evaluationContextProvider, queryRewriterProvider, escape);
        this.entityManager = em;
        this.queryMethodFactory = queryMethodFactory;

    }

    public static QueryLookupStrategy create(EntityManager em, JpaQueryMethodFactory queryMethodFactory,
                                             @Nullable Key key, QueryMethodEvaluationContextProvider evaluationContextProvider,
                                             QueryRewriterProvider queryRewriterProvider, EscapeCharacter escape) {
        return new JpaExtendQueryLookupStrategy(em, queryMethodFactory, key,  evaluationContextProvider, queryRewriterProvider, escape);
    }

    @Override
    public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
                                        NamedQueries namedQueries) {
        if (method.getAnnotation(MyQuery.class) == null) {
            return jpaQueryLookupStrategy.resolveQuery(method, metadata, factory, namedQueries);
        } else {
            MyQuery myQuery = method.getAnnotation(MyQuery.class);
            return new JpaExtendQuery(queryMethodFactory.build(method, metadata, factory), entityManager, myQuery.value(), myQuery.nativeQuery());
        }
    }

}
