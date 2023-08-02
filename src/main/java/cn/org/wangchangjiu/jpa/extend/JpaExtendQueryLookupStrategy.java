package cn.org.wangchangjiu.jpa.extend;

import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.jpa.repository.query.EscapeCharacter;
import org.springframework.data.jpa.repository.query.JpaQueryLookupStrategy;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.lang.Nullable;

import javax.persistence.EntityManager;
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

    private QueryExtractor extractor;

    public JpaExtendQueryLookupStrategy(EntityManager em, @Nullable Key key, QueryExtractor extractor,
                                        QueryMethodEvaluationContextProvider evaluationContextProvider, EscapeCharacter escape) {
        this.jpaQueryLookupStrategy = JpaQueryLookupStrategy.create(em, key, extractor, evaluationContextProvider, escape);
        this.extractor = extractor;
        this.entityManager = em;
    }

    public static QueryLookupStrategy create(EntityManager em, @Nullable Key key, QueryExtractor extractor,
                                             QueryMethodEvaluationContextProvider evaluationContextProvider, EscapeCharacter escape) {
        return new JpaExtendQueryLookupStrategy(em, key, extractor, evaluationContextProvider, escape);
    }

    @Override
    public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
                                        NamedQueries namedQueries) {
        if (method.getAnnotation(MyQuery.class) == null) {
            return jpaQueryLookupStrategy.resolveQuery(method, metadata, factory, namedQueries);
        } else {
            MyQuery myQuery = method.getAnnotation(MyQuery.class);
            return new JpaExtendQuery(new JpaQueryMethod(method, metadata, factory, extractor), entityManager, myQuery.value(), myQuery.nativeQuery());
        }
    }

}
