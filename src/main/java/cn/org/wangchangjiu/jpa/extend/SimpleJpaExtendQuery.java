package cn.org.wangchangjiu.jpa.extend;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.query.internal.QueryImpl;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;
import org.springframework.data.jpa.repository.query.QueryUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;

/**
 * @Classname SimpleJpaExtendQuery
 * @Description
 * @Date 2023/8/7 18:19
 * @Created by wangchangjiu
 */
public class SimpleJpaExtendQuery extends AbstractJpaExtendQuery {
    /**
     * Creates a new {@link AbstractJpaQuery} from the given {@link JpaQueryMethod}.
     *
     * @param method
     * @param em
     * @param myQuery
     */
    public SimpleJpaExtendQuery(JpaQueryMethod method, EntityManager em, MyQuery myQuery) {
        super(method, em, myQuery);
    }

    @Override
    protected Query createJpaQuery(String sortedQueryString) {
        Class<?> objectType = getQueryMethod().getReturnedObjectType();

        Query query;

        if (getQueryMethod().isQueryForEntity()) {
            query = em.createQuery(sortedQueryString, objectType);
        } else {
            query = em.createQuery(sortedQueryString);
            query.unwrap(QueryImpl.class).setResultTransformer(new JpaExtendResultTransformer(objectType));
        }
        return query;
    }

    @Override
    protected Query createJpaCountQuery(String queryString) {
        if(StringUtils.isEmpty(declaredQuery.getCountQueryString())){
            // 没有手动指定 count 语句
            queryString = QueryUtils.createCountQueryFor(queryString, declaredQuery.getCountProjection());
        }

        Query query = em.createQuery(queryString, Long.class);
        return query;
    }
}
