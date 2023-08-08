package cn.org.wangchangjiu.jpa.extend;

import cn.hutool.core.util.StrUtil;
import org.springframework.data.jpa.repository.query.JpaParameters;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @Classname ExpressionQueryResolver
 * @Description
 * @Date 2023/7/31 17:00
 * @Created by wangchangjiu
 */

public final class ExpressionQueryResolverStrategy {

    /**
     *  表达式查询解析器 接口
     */
    interface ExpressionQueryResolver {

        String PLACEHOLDER_PREFIX = "?{";
        String PLACEHOLDER_SUFFIX = "}";

        String BLANK_STR = "";

        /**
         *  占位符 表达式 正则 匹配
         *  ?{and v.pay_channel_code = :payChannel}
         */
        Pattern PLACEHOLDER_EXPRESSION_PARAMETER = Pattern.compile("\\?\\{(.+?)\\}");

        /**
         *  position 表达式 匹配 ?1
         */
        Pattern POSITION_EXPRESSION_PARAMETER = Pattern.compile("\\?[1-9+]");

        /**
         *  name 表达式 匹配 :name
         */
        Pattern NAME_EXPRESSION_PARAMETER = Pattern.compile(":[a-zA-Z0-9]+");

        /**
         *  非占位符 POSITION 表达式 正则 匹配
         *  and v.pay_channel_code = ?1
         */
        Pattern NO_PLACEHOLDER_POSITION_EXPRESSION_PARAMETER = Pattern.compile("(where|WHERE|and|AND|or|OR)\\s+[a-zA-Z._]+\\s+=\\s+\\?[1-9+]");

        /**
         *  非占位符 name 表达式 匹配
         *
         *  and v.pay_channel_code = :payChannel
         */
        Pattern NO_PLACEHOLDER_NAME_EXPRESSION_PARAMETER = Pattern.compile("(where|WHERE|and|AND|or|OR)\\s+[a-zA-Z._]+\\s+=\\s+:[a-zA-Z0-9]+");

        boolean match(String queryString, boolean expressionQuery);

        QueryResolveResult resolve(String queryString, JpaParameters parameters, Object[] values);

        default String nameParameterProcessor(String queryString, Map<String, Object> allQueryParams, List<String> removeParams, String matchExpression){

            Matcher parameterExpressionMatcher = NAME_EXPRESSION_PARAMETER.matcher(matchExpression);

            if(parameterExpressionMatcher.find()){

                String parameterName = parameterExpressionMatcher.group().replace(":", BLANK_STR);

                Object parameterValue = allQueryParams.get(parameterName);

                if(parameterValue == null){
                    queryString = queryString.replace(matchExpression, BLANK_STR);
                    removeParams.add(parameterName);
                }
            }
            if(removeParams.size() == allQueryParams.size()){
                // 参数全部为空 ， 去掉 where 关键字
                queryString = StrUtil.replace(queryString, "where", BLANK_STR, true);
            }

            return queryString;
        }

        default String positionParameterProcessor(String queryString, Object[] values, List<Integer> removeParamIndex, String parameterExpression){

            Matcher positionExpressionMatcher = POSITION_EXPRESSION_PARAMETER.matcher(parameterExpression);


            if(positionExpressionMatcher.find()){

                // ?1
                String paramExpression = positionExpressionMatcher.group();
                Integer index = Integer.valueOf(paramExpression.replace("?", BLANK_STR));

                Integer position = index - 1;
                Object paramValue = values[position];

                if(paramValue == null){
                    // 参数为空
                    queryString = queryString.replace(parameterExpression, BLANK_STR);
                    removeParamIndex.add(position);
                } else {
                    // 参数不为空 修改为 :paramName
                    int count = removeParamIndex.stream().filter(item -> index > item).collect(Collectors.toList()).size();
                    Integer newIndex = index - count;

                    String newParameter = parameterExpression.replace("?" + index, "?" + newIndex);
                    queryString = queryString.replace(parameterExpression, newParameter);
                }
            }

            if(removeParamIndex.size() == values.length){
                // 参数全部为空 ， 去掉 where 关键字
                queryString = StrUtil.replace(queryString, "where", BLANK_STR, true);
            }

            return queryString;
        }

    }

    /**
     *  表达式查询解析器 枚举
     */
    enum ExpressionQueryResolverEnum implements ExpressionQueryResolver {

        EmptyExpressionQueryResolver(){
            @Override
            public boolean match(String queryString,  boolean expressionQuery) {
                return !expressionQuery;
            }

            @Override
            public QueryResolveResult resolve(String queryString, JpaParameters parameters, Object[] values) {
                Matcher positionExpressionParameter = POSITION_EXPRESSION_PARAMETER.matcher(queryString);
                boolean positionParam = false;
                if(positionExpressionParameter.find()){
                    positionParam = true;
                }

                return new QueryResolveResult.EmptyQueryResolveResult(queryString, positionParam, parameters, values);
            }
        },

        /**
         *  占位符 Position 表达式 查询处理器
         *
         */
        PlaceholderPositionExpressionQueryResolver() {

            @Override
            public boolean match(String queryString, boolean expressionQuery) {
                if(!expressionQuery){
                    return false;
                }
                Matcher  expressionParameter = PLACEHOLDER_EXPRESSION_PARAMETER.matcher(queryString);
                Matcher positionExpressionParameter = POSITION_EXPRESSION_PARAMETER.matcher(queryString);
                return expressionParameter.find() && positionExpressionParameter.find();
            }

            @Override
            public QueryResolveResult resolve(String queryString, JpaParameters parameters, Object[] values) {
                // 是否包含 ?1
                Matcher  expressionParameter = PLACEHOLDER_EXPRESSION_PARAMETER.matcher(queryString);

                // 使用 ? 注入参数的
                List<Integer> removeParamIndex = new ArrayList<>();

                while (expressionParameter.find()) {

                    // and t.name = ?1
                    String parameter = expressionParameter.group(1);

                    queryString = super.positionParameterProcessor(queryString, values, removeParamIndex, parameter);

                }

                String afterParseSQL = queryString.replace(PLACEHOLDER_PREFIX, BLANK_STR).replace(PLACEHOLDER_SUFFIX, BLANK_STR);
                return new QueryResolveResult.PositionExpressionQueryResolveResult(afterParseSQL, removeParamIndex, JpaExtendQueryUtils.toPositionMap(values));
            }
        },

        /**
         *  占位符 Name 表达式 查询处理器
         *
         */
        PlaceholderNameExpressionQueryResolver(){

            @Override
            public boolean match(String queryString, boolean expressionQuery) {
                if(!expressionQuery){
                    return false;
                }
                Matcher  expressionParameter = PLACEHOLDER_EXPRESSION_PARAMETER.matcher(queryString);
                Matcher nameExpressionParameter = NAME_EXPRESSION_PARAMETER.matcher(queryString);
                return expressionParameter.find() && nameExpressionParameter.find();
            }

            @Override
            public QueryResolveResult resolve(String queryString, JpaParameters parameters, Object[] values) {

                // 解析参数 所有参数 包括 null的
                Map<String, Object> allQueryParams = JpaExtendQueryUtils.getParams(parameters, values);

                List<String> removeParams = new ArrayList<>();

                Matcher expressionParameter = PLACEHOLDER_EXPRESSION_PARAMETER.matcher(queryString);

                while (expressionParameter.find()) {

                    // and t.name = :name
                    String matchExpression = expressionParameter.group();

                    queryString = super.nameParameterProcessor(queryString, allQueryParams, removeParams, matchExpression);
                }

                String afterParseSQL = queryString.replace(PLACEHOLDER_PREFIX, BLANK_STR).replace(PLACEHOLDER_SUFFIX, BLANK_STR);
                return new QueryResolveResult.NameExpressionQueryResolveResult(afterParseSQL, removeParams, allQueryParams);
            }
        },

        PositionExpressionQueryResolver() {

            @Override
            public boolean match(String queryString,  boolean expressionQuery) {
                if(!expressionQuery){
                    return false;
                }
                return !PLACEHOLDER_EXPRESSION_PARAMETER.matcher(queryString).find() && NO_PLACEHOLDER_POSITION_EXPRESSION_PARAMETER.matcher(queryString).find();
            }

            @Override
            public QueryResolveResult resolve(String queryString, JpaParameters parameters, Object[] values) {

                Matcher expressionParameter = NO_PLACEHOLDER_POSITION_EXPRESSION_PARAMETER.matcher(queryString);

                // 使用 ? 注入参数的
                List<Integer> removeParamIndex = new ArrayList<>();

                while (expressionParameter.find()) {

                    String parameter = expressionParameter.group();

                    queryString = super.positionParameterProcessor(queryString, values, removeParamIndex, parameter);
                }

                return new QueryResolveResult.PositionExpressionQueryResolveResult(queryString, removeParamIndex, JpaExtendQueryUtils.toPositionMap(values));
            }
        },


        NameExpressionQueryResolver() {

            @Override
            public boolean match(String queryString, boolean expressionQuery) {
                if(!expressionQuery){
                    return false;
                }
                return !PLACEHOLDER_EXPRESSION_PARAMETER.matcher(queryString).find() && NO_PLACEHOLDER_NAME_EXPRESSION_PARAMETER.matcher(queryString).find();
            }

            @Override
            public QueryResolveResult resolve(String queryString, JpaParameters parameters, Object[] values) {

                Map<String, Object> allQueryParams = JpaExtendQueryUtils.getParams(parameters, values);

                List<String> removeParams = new ArrayList<>();

                Matcher expressionParameter = NO_PLACEHOLDER_NAME_EXPRESSION_PARAMETER.matcher(queryString);

                while (expressionParameter.find()) {

                    // and t.name = :name
                    String matchExpression = expressionParameter.group();

                    queryString = super.nameParameterProcessor(queryString, allQueryParams, removeParams, matchExpression);
                }

                return new QueryResolveResult.NameExpressionQueryResolveResult(queryString, removeParams, allQueryParams);
            }
        }

    }


    public static QueryResolveResult resolve(String queryString, boolean expressionQuery, JpaParameters parameters, Object[] values){

        Optional<ExpressionQueryResolverEnum> resolverEnumOptional = Arrays.stream(ExpressionQueryResolverEnum.values()).filter(item -> item.match(queryString, expressionQuery)).findFirst();
        if(!resolverEnumOptional.isPresent()){
            throw new RuntimeException("没有找到SQL解析策略");
        }
        return resolverEnumOptional.get().resolve(queryString, parameters, values);
    }



}

