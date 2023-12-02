package cn.org.wangchangjiu.jpa.extend;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import java.io.Serializable;
import java.util.*;

/**
 * @Classname JpaExtendRepository
 * @Description
 * @Date 2023/8/2 21:35
 * @author by wangchangjiu
 */
public class JpaExtendRepositoryImpl<T, ID extends Serializable> extends SimpleJpaRepository<T, ID> implements JpaExtendRepository<T, ID>, Serializable {

    private static Logger logger = LoggerFactory.getLogger(JpaExtendRepositoryImpl.class);

    private final EntityManager entityManager;

    private  JpaEntityInformation<T, ?> entityInformation;

    private  Class<T> domainClass;
    private static final String DELETED_FIELD = "deleted";

    public JpaExtendRepositoryImpl(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
        this.entityInformation = entityInformation;
    }

    public JpaExtendRepositoryImpl(Class<T> domainClass, EntityManager em) {
        super(domainClass, em);
        this.entityManager = em;
        this.domainClass = domainClass;
    }

    @Override
    public Page<T> findAll(Object paramObj, Pageable pageable) {
        Page<T> page = this.findAll(buildSpecification(paramObj), pageable);
        return page;
    }

    @Override
    public Optional<T> findOne(Object paramObj) {
        return this.findOne(buildSpecification(paramObj));
    }

    @Override
    public List<T> findAll(Object paramObj) {
        return this.findAll(buildSpecification(paramObj));
    }

    @Override
    public List<T> findAll(Object paramObj, Sort sort) {
        return this.findAll(buildSpecification(paramObj), sort);
    }

    @Override
    public long count(Object paramObj) {
        return this.count(buildSpecification(paramObj));
    }

    private Specification<T> buildSpecification(Object paramObj) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.and();
            ReflectionUtils.doWithFields(paramObj.getClass(), field -> {
                JpaParam jpaParam = AnnotationUtils.getAnnotation(field, JpaParam.class);
                field.setAccessible(true);
                Object paramValue = field.get(paramObj);

                Class paramClazz = field.getType();
                if(Objects.isNull(jpaParam) || Objects.isNull(paramValue)){
                    return;
                }

                if (paramValue instanceof String && StringUtils.isBlank((String) paramValue)) {
                    return;
                }

                if (jpaParam.queryType() == JpaParam.QueryType.EQUAL) {
                    // 等于
                    predicate.getExpressions().add(criteriaBuilder.equal(root.get(field.getName()).as(paramClazz), paramValue));
                } else if (jpaParam.queryType() == JpaParam.QueryType.LIKE) {
                    // 模糊查询
                    predicate.getExpressions().add(criteriaBuilder.like(root.get(field.getName()).as(paramClazz), "%" + paramValue + "%"));
                } else if (jpaParam.queryType() == JpaParam.QueryType.NOT_LIKE) {

                    predicate.getExpressions().add(criteriaBuilder.notLike(root.get(field.getName()).as(paramClazz), "%" + paramValue + "%"));
                } else if (jpaParam.queryType() == JpaParam.QueryType.NOTEQUAL) {
                    // 不等于
                    predicate.getExpressions().add(criteriaBuilder.notEqual(root.get(field.getName()).as(paramClazz), paramValue));
                } else if (jpaParam.queryType() == JpaParam.QueryType.GT) {
                    if (!(paramValue instanceof Number)) {
                        throw new IllegalArgumentException("param value not Number");
                    }
                    predicate.getExpressions().add(criteriaBuilder.gt(root.get(field.getName()).as(paramClazz), (Number) paramValue));
                } else if (jpaParam.queryType() == JpaParam.QueryType.GE) {
                    if (paramValue instanceof Date) {
                        predicate.getExpressions().add(criteriaBuilder.greaterThanOrEqualTo(root.get(field.getName()), (Date) paramValue));
                    } else if (!(paramValue instanceof Number)) {
                        throw new IllegalArgumentException("param value not Number");
                    } else {
                        predicate.getExpressions().add(criteriaBuilder.ge(root.get(field.getName()).as(paramClazz), (Number) paramValue));
                    }
                } else if (jpaParam.queryType() == JpaParam.QueryType.LT) {
                    if (!(paramValue instanceof Number)) {
                        throw new IllegalArgumentException("param value not Number");
                    }
                    predicate.getExpressions().add(criteriaBuilder.lt(root.get(field.getName()).as(paramClazz), (Number) paramValue));
                } else if (jpaParam.queryType() == JpaParam.QueryType.LE) {
                    if (paramValue instanceof Date) {
                        predicate.getExpressions().add(criteriaBuilder.lessThanOrEqualTo(root.get(field.getName()), (Date) paramValue));
                    } else if (!(paramValue instanceof Number)) {
                        throw new IllegalArgumentException("param value not Number");
                    } else {
                        predicate.getExpressions().add(criteriaBuilder.le(root.get(field.getName()).as(paramClazz), (Number) paramValue));
                    }
                } else if (jpaParam.queryType() == JpaParam.QueryType.IN) {
                    Path<Object> path = root.get(field.getName());
                    CriteriaBuilder.In<Object> in = criteriaBuilder.in(path);
                    if (paramClazz == String.class && String.valueOf(paramValue).contains(",")) {
                        Arrays.asList(String.valueOf(paramValue).split(",")).stream().forEach(item -> in.value(item));
                    } else if (Collection.class.isAssignableFrom(paramClazz) && paramValue instanceof Collection) {
                        for (Object item : Collection.class.cast(paramValue)) {
                            in.value(item);
                        }
                    }
                    predicate.getExpressions().add(criteriaBuilder.and(in));
                }
            });
            return predicate;
        };
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public <S extends T> List<S> batchSaveAll(Iterable<S> entities, int batchSize) {
        Integer jdbcBatchSize = entityManager.unwrap(Session.class).getJdbcBatchSize();
        entityManager.unwrap(Session.class).setJdbcBatchSize(batchSize);
        List<S> results = saveAll(entities);
        if (jdbcBatchSize != null) {
            entityManager.unwrap(Session.class).setJdbcBatchSize(jdbcBatchSize);
        }
        return results;
    }
}
