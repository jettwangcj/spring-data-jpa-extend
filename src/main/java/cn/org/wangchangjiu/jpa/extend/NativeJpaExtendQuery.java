package cn.org.wangchangjiu.jpa.extend;

import org.hibernate.query.internal.NativeQueryImpl;
import org.springframework.data.jpa.repository.query.AbstractJpaQuery;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;

import javax.persistence.EntityManager;
import javax.persistence.Query;

/**
 * @Classname NativeJpaExtendQuery
 * @Description
 * @Date 2023/8/7 18:17
 * @Created by wangchangjiu
 */
public class NativeJpaExtendQuery extends AbstractJpaExtendQuery {
    /**
     * Creates a new {@link AbstractJpaQuery} from the given {@link JpaQueryMethod}.
     *
     * @param method
     * @param em
     * @param myQuery
     */
    public NativeJpaExtendQuery(JpaQueryMethod method, EntityManager em, MyQuery myQuery) {
        super(method, em, myQuery);
    }

    @Override
    protected Query createJpaQuery(String sortedQueryString) {
        Class<?> objectType = getQueryMethod().getReturnedObjectType();

        Query query;
        if (getQueryMethod().isQueryForEntity()) {
            query = em.createNativeQuery(sortedQueryString, objectType);
        } else {
            query = em.createNativeQuery(sortedQueryString);
            query.unwrap(NativeQueryImpl.class).setResultTransformer(new JpaExtendResultTransformer(objectType));
        }

        return query;
    }

    @Override
    protected Query createJpaCountQuery(String queryString) {
        Query query = em.createNativeQuery(JpaExtendQueryUtils.toCountQuery(queryString));
        return query;
    }


}
