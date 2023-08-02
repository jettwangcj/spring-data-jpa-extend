package cn.org.wangchangjiu.jpa.extend;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.property.access.internal.PropertyAccessStrategyBasicImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyChainedImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyFieldImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyMapImpl;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.transform.AliasedTupleSubsetResultTransformer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @Classname MyResultTransformer
 * @Description 自定义转换
 * @Date 2021/6/9 15:15
 * @Created by wangchangjiu
 */
@Slf4j
public class JpaExtendResultTransformer extends AliasedTupleSubsetResultTransformer {

    // IMPL NOTE : due to the delayed population of setters (setters cached
    // 		for performance), we really cannot properly define equality for
    // 		this transformer
    private final Class resultClass;
    private boolean isInitialized;
    private String[] aliases;
    private Setter[] setters;

    private static Map<Class<?>, Function> typeConversionMap = new HashMap<>();

    static {
        typeConversionMap.put(Long.class, v -> Long.valueOf(v.toString()));
        typeConversionMap.put(long.class, v -> Long.parseLong(v.toString()));
        typeConversionMap.put(Double.class, v -> Double.valueOf(v.toString()));
        typeConversionMap.put(double.class, v -> Double.parseDouble(v.toString()));
        typeConversionMap.put(Float.class, v -> Float.valueOf(v.toString()));
        typeConversionMap.put(float.class, v -> Float.parseFloat(v.toString()));
        typeConversionMap.put(Integer.class, v -> Integer.valueOf(v.toString()));
        typeConversionMap.put(int.class, v -> Integer.parseInt(v.toString()));
        typeConversionMap.put(Boolean.class, v -> parseBoolean(v.toString()) ? Boolean.TRUE : Boolean.FALSE);
        typeConversionMap.put(boolean.class, v -> parseBoolean(v.toString()) ? true : false);
        typeConversionMap.put(LocalDateTime.class, v -> {
            if (java.sql.Timestamp.class.isInstance(v)) {
                return java.sql.Timestamp.class.cast(v).toLocalDateTime();
            } else if (java.sql.Date.class.isInstance(v)) {
                return java.sql.Date.class.cast(v).toLocalDate();
            } else {
                return v;
            }
        });
    }

    /**
     * 把数据库值转为 Boolean
     *
     * @param arg
     * @return
     */
    private static Boolean parseBoolean(String arg) {
        if (StringUtils.isNumeric(arg)) {
            if ("0".equals(arg)) return false;
        } else {
            return Boolean.parseBoolean(arg);
        }
        return true;
    }

    public JpaExtendResultTransformer(Class resultClass) {
        if (resultClass == null) {
            throw new IllegalArgumentException("resultClass cannot be null");
        }
        isInitialized = false;
        this.resultClass = resultClass;
    }

    @Override
    public boolean isTransformedValueATupleElement(String[] aliases, int tupleLength) {
        return false;
    }

    @Override
    public Object transformTuple(Object[] tuple, String[] aliases) {
        try {
            if (resultClass.isAssignableFrom(Map.class)) {
                Map result = new LinkedHashMap();
                for (int i = 0; i < tuple.length; ++i) {
                    String alias = aliases[i];
                    if (alias != null) {
                        result.put(alias, tuple[i]);
                    }
                }
                return result;
            } else {
                aliases = camelAliases(aliases);

                if (!isInitialized) {
                    initialize(aliases);
                } else {
                    check(aliases);
                }

                Object result = resultClass.newInstance();

                for (int i = 0; i < aliases.length; i++) {
                    if (setters[i] != null && setters[i].getMethod() != null) {
                        Class<?> paramType = setters[i].getMethod().getParameterTypes()[0];
                        Function convertType = typeConversionMap.get(paramType);
                        Object convertedObject = (convertType == null) ? tuple[i] : (tuple[i] == null) ? null : convertType.apply(tuple[i]);
                        setters[i].set(result, convertedObject, null);
//                    setters[i].set( result, tuple[i], null );
                    }
                }
                return result;
            }
        } catch (InstantiationException e) {
            throw new HibernateException("Could not instantiate resultclass: " + resultClass.getName());
        } catch (IllegalAccessException e) {
            throw new HibernateException("Could not instantiate resultclass: " + resultClass.getName());
        }
    }

    private String[] camelAliases(String[] aliases) {
        return (aliases == null || aliases.length == 0)
                ? aliases
                : Arrays.stream(aliases).map(alias -> removeSeparatorsToCamelString(alias, '_')).toArray(String[]::new);
    }

    private void initialize(String[] aliases) {
        PropertyAccessStrategyChainedImpl propertyAccessStrategy = new PropertyAccessStrategyChainedImpl(
                PropertyAccessStrategyBasicImpl.INSTANCE,
                PropertyAccessStrategyFieldImpl.INSTANCE,
                PropertyAccessStrategyMapImpl.INSTANCE
        );
        this.aliases = new String[aliases.length];
        setters = new Setter[aliases.length];
        for (int i = 0; i < aliases.length; i++) {
            String alias = aliases[i];
            if (alias != null && checkPropertySetter(resultClass, alias)) {
                this.aliases[i] = alias;
                setters[i] = propertyAccessStrategy.buildPropertyAccess(resultClass, alias).getSetter();
            }
        }
        isInitialized = true;
    }

    /**
     * 检查属性是否有 setter方法
     *
     * @param containerJavaType
     * @param propertyName
     * @return
     */
    private boolean checkPropertySetter(Class containerJavaType, String propertyName) {
        Class propertyType = null;
        try {
            Method getterMethod = ReflectHelper.findGetterMethod(containerJavaType, propertyName);
            propertyType = getterMethod.getReturnType();
        } catch (PropertyNotFoundException ex) {
            log.debug("class :{} can not find propertyName:{} getterMethod", containerJavaType, propertyName);
            try {
                Field field = ReflectHelper.findField(containerJavaType, propertyName);
                propertyType = field.getType();
            } catch (PropertyNotFoundException e) {
                log.debug("class :{} can not find property:{}", containerJavaType, propertyName);
            }
        }
        if (propertyType == null) {
            log.debug("class :{} can not find propertyName:{} setterMethod", containerJavaType, propertyName);
            return false;
        }
        Method setterMethod = ReflectHelper.setterMethodOrNull(containerJavaType, propertyName, propertyType);
        return setterMethod != null;
    }

    private void check(String[] aliases) {
        if (!Arrays.equals(aliases, this.aliases)) {
           /* throw new IllegalStateException(
                    "aliases are different from what is cached; aliases=" + Arrays.asList(aliases) +
                            " cached=" + Arrays.asList(this.aliases));*/
            log.debug("aliases are different from what is cached; aliases=" + Arrays.asList(aliases) +
                    " cached=" + Arrays.asList(this.aliases));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        JpaExtendResultTransformer that = JpaExtendResultTransformer.class.cast(o);

        if (!resultClass.equals(that.resultClass)) {
            return false;
        }
        if (!Arrays.equals(aliases, that.aliases)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = resultClass.hashCode();
        result = 31 * result + (aliases != null ? Arrays.hashCode(aliases) : 0);
        return result;
    }

    private String removeSeparatorsToCamelString(String s, char separator) {
        StringBuilder result = new StringBuilder(s);
        for (int i = 1; i < result.length(); i++) {
            if (result.charAt(i - 1) == separator) {
                result.deleteCharAt(i - 1);
                if (i - 1 < result.length()) {
                    result.setCharAt(i - 1, Character.toUpperCase(result.charAt(i - 1)));
                }
            }
        }
        return result.toString();
    }
}
