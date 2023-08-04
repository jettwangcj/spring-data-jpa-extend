package cn.org.wangchangjiu.jpa.extend;

import java.lang.annotation.*;

/**
 * @Classname JpaParam
 * @Description
 * @Date 2021/8/12 15:54
 * @Created by wangchangjiu
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JpaParam {

    QueryType queryType() default QueryType.EQUAL;

    enum QueryType {

        IN("in"),
        LT("lt"),
        LE("le"),
        GE("ge"),
        GT("gt"),
        NOT_LIKE("notLike"),
        NOTEQUAL("notEqual"),
        EQUAL("equal"),
        LIKE("like");

        private String queryType;

        QueryType(String queryType){
            this.queryType = queryType;
        }

        public String getQueryType() {
            return queryType;
        }

        public void setQueryType(String queryType) {
            this.queryType = queryType;
        }
    }
}
