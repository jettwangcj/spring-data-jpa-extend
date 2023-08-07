package cn.org.wangchangjiu.jpa.extend;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.query.*;


/**
 * @Classname MyExtendJpaQuery
 * @Description
 * @Date 2023/7/31 15:09
 * @Created by wangchangjiu
 */

@Slf4j
public abstract class AbstractJpaExtendQuery extends AbstractJpaQuery {


    protected DeclaredQuery declaredQuery;

    protected EntityManager em;



    /**
     * Creates a new {@link AbstractJpaQuery} from the given {@link JpaQueryMethod}.
     *
     * @param method
     * @param em
     */
    public AbstractJpaExtendQuery(JpaQueryMethod method, EntityManager em, MyQuery myQuery) {
        super(method, em);
        if(QueryUtils.hasConstructorExpression(myQuery.value())){
            throw new InvalidJpaQueryMethodException("SQL Cannot has constructor expression , SQL: " + myQuery.value());
        }
        this.declaredQuery = new DeclaredQuery(myQuery.value(), myQuery.countQuery(),
                StringUtils.isEmpty(myQuery.countProjection()) ? null : myQuery.countProjection(), myQuery.nativeQuery());
        this.em = em;

    }

    @Override
    protected Query doCreateQuery(JpaParametersParameterAccessor accessor) {

        JpaParameters parameters = getQueryMethod().getParameters();
        Object[] values = accessor.getValues();

        RemovePageSortParametersParameterAccessor removePageSortParametersParameterAccessor = new RemovePageSortParametersParameterAccessor(parameters, values);

        QueryResolveResult resolveResult = ExpressionQueryResolverStrategy.resolve(declaredQuery.getQueryString(), parameters, removePageSortParametersParameterAccessor.getRemovePageSortParameters());

        String nativeQuery = resolveResult.getAfterParseSQL();

        log.info("MyExtendJpaQuery before sql :{} resolve sql:{}", declaredQuery.getQueryString(), nativeQuery);

        String sortedQueryString = QueryUtils
                .applySorting(nativeQuery, removePageSortParametersParameterAccessor.getSort(), QueryUtils.detectAlias(nativeQuery));

        Query query = createJpaQuery(sortedQueryString);

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

    protected abstract Query createJpaQuery(String sortedQueryString);


    @Override
    protected Query doCreateCountQuery(JpaParametersParameterAccessor accessor) {

        JpaParameters parameters = getQueryMethod().getParameters();
        Object[] values = accessor.getValues();

        // 移除 page / sort 参数
        RemovePageSortParametersParameterAccessor removePageSortParametersParameterAccessor = new RemovePageSortParametersParameterAccessor(parameters, values);

        // 获取原 count sql
        String countQueryString = StringUtils.isEmpty(declaredQuery.getCountQueryString()) ? declaredQuery.getQueryString() : declaredQuery.getCountQueryString();

        // 解析 动态 参数后的 count sql
        QueryResolveResult resolveResult = ExpressionQueryResolverStrategy.resolve(countQueryString, parameters, removePageSortParametersParameterAccessor.getRemovePageSortParameters());
        String queryString = resolveResult.getAfterParseSQL();

        Query query = createJpaCountQuery(queryString);

        resolveResult.setQueryParams(query);
        return query;
    }

    protected abstract Query createJpaCountQuery(String queryString);

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeclaredQuery {

        private String queryString;

        private String countQueryString;

        private String countProjection;

        private boolean nativeQuery;

    }
}
