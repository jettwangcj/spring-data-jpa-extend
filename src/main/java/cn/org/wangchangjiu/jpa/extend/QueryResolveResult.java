package cn.org.wangchangjiu.jpa.extend;

import jakarta.persistence.Query;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Classname QueryResolveResult
 * @Description
 * @Date 2023/8/1 17:45
 * @Created by wangchangjiu
 */
@Data
public abstract class QueryResolveResult {

    protected String afterParseSQL;

    public abstract void setQueryParams(Query query);


    @Data
    public static class NameExpressionQueryResolveResult extends QueryResolveResult {

        private List<String> removeParamNames;

        private Map<String, Object> allQueryParams;

        public NameExpressionQueryResolveResult(String afterParseSQL, List<String> removeParamNames, Map<String, Object> allQueryParams){
            this.afterParseSQL = afterParseSQL;
            this.removeParamNames = removeParamNames;
            this.allQueryParams = allQueryParams;
        }

        @Override
        public void setQueryParams(Query query) {
            removeParamNames.stream().forEach(item -> allQueryParams.remove(item));
            allQueryParams.forEach(query::setParameter);
        }
    }

    @Data
    public static class PositionExpressionQueryResolveResult extends QueryResolveResult {

        private List<Integer> removeParamIndex;

        private Map<Integer, Object> allQueryParams;

        public PositionExpressionQueryResolveResult(String afterParseSQL, List<Integer> removeParamIndex, Map<Integer, Object> allQueryParams){
            this.afterParseSQL = afterParseSQL;
            this.removeParamIndex = removeParamIndex;
            this.allQueryParams = allQueryParams;
        }

        @Override
        public void setQueryParams(Query query) {
            removeParamIndex.stream().forEach(item -> allQueryParams.remove(item));
            Map<Integer, Object> newQueryParams = new HashMap<>();

            allQueryParams.forEach((k, v) -> {
                int count = removeParamIndex.stream().filter(item -> k > item).collect(Collectors.toList()).size();
                // 修改 索引 +1 是参数从1开始
                Integer newIndex = k - count + 1;
                newQueryParams.put(newIndex, v);
            });

            newQueryParams.forEach(query::setParameter);
        }
    }


}
