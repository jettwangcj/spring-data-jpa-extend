package cn.org.wangchangjiu.jpa.extend;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.DynaProperty;
import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.data.jpa.repository.query.JpaParameters;
import org.springframework.data.repository.query.Parameter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Classname QueryUitl
 * @Description
 * @Date 2023/7/31 16:47
 * @Created by wangchangjiu
 */
public class JpaExtendQueryUtils {

    private static final Pattern ORDERBY_PATTERN = Pattern
            .compile("order\\s+by.+?$", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);


    private static String wrapCountQuery(String query) {
        return "select count(*) from (" + query + ") as temp";
    }

    private static String cleanOrderBy(String query) {
        Matcher matcher = ORDERBY_PATTERN.matcher(query);
        StringBuffer sb = new StringBuffer();
        int i = 0;
        while (matcher.find()) {
            String part = matcher.group(i);
            if (canClean(part)) {
                matcher.appendReplacement(sb, "");
            } else {
                matcher.appendReplacement(sb, part);
            }
            i++;
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static boolean canClean(String orderByPart) {
        return orderByPart != null && (!orderByPart.contains(")") ||
                StringUtils.countOccurrencesOf(orderByPart, ")") == StringUtils.countOccurrencesOf(orderByPart, "("));
    }

    public static Map<String, Object> getParams(JpaParameters parameters, Object[] values) {
        //gen model
        Map<String, Object> params = new HashMap<>();
        for (int i = 0; i < parameters.getNumberOfParameters(); i++) {
            Object value = values[i];
            Parameter parameter = parameters.getParameter(i);
            if(value == null){
                params.put(parameter.getName().orElse(null), null);
            }
            if (value != null && parameter.isBindable()) {
                if (!JpaExtendQueryUtils.isValidValue(value)) {
                    continue;
                }
                Class<?> clz = value.getClass();
                if (clz.isPrimitive() || String.class.isAssignableFrom(clz) || Number.class.isAssignableFrom(clz)
                        || clz.isArray() || Collection.class.isAssignableFrom(clz) || clz.isEnum()) {
                    params.put(parameter.getName().orElse(null), value);
                } else {
                    // 如果参数是对象
                    params = JpaExtendQueryUtils.toParams(value);
                }
            }
        }
        return params;
    }

    public static Map<Integer, Object> toPositionMap(Object[] values) {
        Map<Integer, Object> valueMap = new HashMap<>();
        int position = 0;
        for (Object paramValue: values) {
            if(paramValue == null){
                valueMap.put(position, null);
            } else {

                if (!JpaExtendQueryUtils.isValidValue(paramValue)) {
                    continue;
                }

                Class<?> clz = paramValue.getClass();
                if (clz.isPrimitive() || String.class.isAssignableFrom(clz) || Number.class.isAssignableFrom(clz)
                        || clz.isArray() || Collection.class.isAssignableFrom(clz) || clz.isEnum()) {
                    valueMap.put(position, paramValue);
                } else {
                    // 如果参数是对象
                    throw new RuntimeException("position param cannot Object");
                }
            }
            position ++;
        }
        return valueMap;
    }


    @SuppressWarnings("unchecked")
    public static Map<String, Object> toParams(Object beanOrMap) {
        Map<String, Object> params;
        if (beanOrMap instanceof Map) {
            params = (Map<String, Object>) beanOrMap;
        } else {
            params = objectToMap(beanOrMap);
        }
        return params;
    }

    public static boolean isValidValue(Object object) {
        if (object == null) {
            return false;
        }
        return !(object instanceof Collection && CollectionUtils.isEmpty((Collection<?>) object));
    }

    public static Map<String, Object> objectToMap(Object bean) {
        if (bean == null) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> description = new HashMap<>();
            if (bean instanceof DynaBean) {
                DynaProperty[] descriptors = DynaBean.class.cast(bean).getDynaClass().getDynaProperties();
                for (DynaProperty descriptor : descriptors) {
                    String name = descriptor.getName();
                    description.put(name, BeanUtils.getProperty(bean, name));
                }
            } else {
                PropertyDescriptor[] descriptors = PropertyUtils.getPropertyDescriptors(bean);
                for (PropertyDescriptor descriptor : descriptors) {
                    String name = descriptor.getName();
                    if (PropertyUtils.getReadMethod(descriptor) != null) {
                        description.put(name, PropertyUtils.getNestedProperty(bean, name));
                    }
                }
            }
            return description;
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    public static String toCountQuery(String query) {
        return wrapCountQuery(cleanOrderBy(query));
    }


}
