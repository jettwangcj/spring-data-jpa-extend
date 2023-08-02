package cn.org.wangchangjiu.jpa.extend;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.query.sql.internal.NativeQueryImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.query.*;

/**
 * @Classname MyExtendJpaQuery
 * @Description
 * @Date 2023/7/31 15:09
 * @Created by wangchangjiu
 */

@Slf4j
public class JpaExtendQuery extends AbstractJpaQuery {


    private DeclaredQuery declaredQuery;

    private EntityManager em;



    /**
     * Creates a new {@link AbstractJpaQuery} from the given {@link JpaQueryMethod}.
     *
     * @param method
     * @param em
     */
    public JpaExtendQuery(JpaQueryMethod method, EntityManager em, String queryString, boolean nativeQuery) {
        super(method, em);
        this.declaredQuery = new DeclaredQuery(queryString, nativeQuery);
        this.em = em;
    }

    @Override
    protected Query doCreateQuery(JpaParametersParameterAccessor accessor) {

        JpaParameters parameters = getQueryMethod().getParameters();
        Object[] values = accessor.getValues();

        QueryResolveResult resolveResult = ExpressionQueryResolverStrategy.resolve(declaredQuery.getQueryString(), parameters, values);

        String nativeQuery = resolveResult.getAfterParseSQL();

        log.info("MyExtendJpaQuery before sql :{} resolve sql:{}", declaredQuery.getQueryString(), nativeQuery);

      //  ParameterAccessor accessor = new ParametersParameterAccessor(parameters, values);
        String sortedQueryString = QueryUtils
                .applySorting(nativeQuery, accessor.getSort(), QueryUtils.detectAlias(nativeQuery));

        Query query = createJpaNativeQuery(sortedQueryString);

        resolveResult.setQueryParams(query);

        if (parameters.hasPageableParameter()) {
            Pageable pageable = (Pageable) (values[parameters.getPageableIndex()]);
            if (pageable != null) {
                query.setFirstResult((int) pageable.getOffset());
                query.setMaxResults(pageable.getPageSize());
            }
        }
        return query;
    }


    @Override
    protected Query doCreateCountQuery(JpaParametersParameterAccessor accessor) {

        JpaParameters parameters = getQueryMethod().getParameters();
        Object[] values = accessor.getValues();

        QueryResolveResult resolveResult = ExpressionQueryResolverStrategy.resolve(declaredQuery.getQueryString(), parameters, values);
        String nativeQuery = resolveResult.getAfterParseSQL();

        Query query = em.createNativeQuery(JpaExtendQueryUtils.toCountQuery(nativeQuery));
        resolveResult.setQueryParams(query);
        return query;
    }


    private Query createJpaNativeQuery(String queryString) {
        Class<?> objectType = getQueryMethod().getReturnedObjectType();

        Query oriProxyQuery;

        if (getQueryMethod().isQueryForEntity()) {
            oriProxyQuery = em.createNativeQuery(queryString, objectType);
        } else {
            oriProxyQuery = em.createNativeQuery(queryString);
            oriProxyQuery.unwrap(NativeQueryImpl.class).setResultTransformer(new JpaExtendResultTransformer(objectType));
        }
        return oriProxyQuery;
    }




    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeclaredQuery {

        private String queryString;

        private boolean nativeQuery;

    }
}
