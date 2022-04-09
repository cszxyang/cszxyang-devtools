package interceptor;
import annotation.EraseDefaultTime;
import annotation.EraseTarget;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.*;
import org.springframework.stereotype.Component;
import util.json.JsonUtils;

import java.lang.reflect.Field;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 出于查询性能考虑，需指定字段为 NOT NULL 并提供默认值
 * 对于日期类型数据，如果实际设值不在插入记录时设值，默认值记为 1970-01-01 00:00:00
 * 在查询的时候进行拦截处理，如果为默认日期则将之置空，避免给用户造成困惑
 * 被拦截的实体类使用 @EraseTarget 标记，被拦截的字段使用 @EraseDefaultTime 标记
 *
 * @author yangzhaoxiong
 * @since  2022-03-23
 */
@Intercepts(@Signature(method = "handleResultSets", type = ResultSetHandler.class, args = {Statement.class}))
@Component
@Slf4j
public class DefaultTimeEraser implements Interceptor {
    /**
     * 默认时间戳
     */
    private static final long DEFAULT_TIME_MILLIS;
    /**
     * 出于反射性能考虑，缓存被拦截的实体类信息
     */
    private final Map<Class<?>, List<Field>> targetFieldCache = new ConcurrentHashMap<>();

    static {
        String defaultTimeStr = "1970-01-01 00:00:00";
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime defaultDateTime = LocalDateTime.parse(defaultTimeStr, dateTimeFormatter);
        Date defaultDate = Date.from(defaultDateTime.atZone(ZoneId.systemDefault()).toInstant());
        DEFAULT_TIME_MILLIS = defaultDate.getTime();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object intercept(Invocation invocation) throws Throwable {
        if (log.isDebugEnabled()) {
            log.debug("DefaultTimeEraser intercepting...targetFieldCache={}", JsonUtils.toJson(targetFieldCache));
        }
        Object object = invocation.proceed();
        try {
            if (null == object) {
                return null;
            }
            if (object instanceof List) {
                return replaceDefaultDate((List<Object>) object);
            }
        } catch (Throwable throwable) {
            log.error("DefaultTimeEraser intercept error", throwable);
        }
        return object;
    }

    /**
     * 覆盖默认时间
     *
     * @param dataList MyBatis 查询出来的结果集
     * @return 处理后的结果集
     * @throws IllegalAccessException 反射异常
     */
    private List<Object> replaceDefaultDate(List<Object> dataList) throws IllegalAccessException {
        if (CollectionUtils.isEmpty(dataList)) {
            return dataList;
        }
        // 结果集数据不会异构，只需要判断一次
        boolean isDomain = isEraseTargetDomain(dataList.get(0));
        List<Object> resultList = new ArrayList<>();
        for (Object data : dataList) {
            if (null == data) {
                resultList.add(null);
                continue;
            }
            if (isDomain) {
                Object result = handleDomainProperty(data);
                resultList.add(result);
            } else if (data instanceof Date) {
                // 有可能只查单列
                Date dateValue = (Date) data;
                if (isDefaultDate(dateValue)) {
                    resultList.add(null);
                } else {
                    resultList.add(data);
                }
            } else {
                resultList.add(data);
            }
        }
        return resultList;
    }

    /**
     * 对于实体对象，遍历其属性集
     *
     * @param data 实体对象
     * @return 处理后的实体对象
     * @throws IllegalAccessException 反射异常
     */
    private Object handleDomainProperty(Object data) throws IllegalAccessException {
        Class<?> dataClass = data.getClass();
        List<Field> fields = targetFieldCache.get(dataClass);
        if (CollectionUtils.isEmpty(fields)) {
            Field[] declaredFields = dataClass.getDeclaredFields();
            for (Field declaredField : declaredFields) {
                if (Date.class.equals(declaredField.getType()) && Objects.nonNull(declaredField.getAnnotation(EraseDefaultTime.class))) {
                    fields.add(declaredField);
                }
            }
        }
        for (Field field : fields) {
            field.setAccessible(true);
            Date dateValue = (Date) field.get(data);
            if (isDefaultDate(dateValue)) {
                field.set(data, null);
            }
        }
        return data;
    }

    /**
     * 判断是否为数据库实体类
     *
     * @param object 实体对象
     * @return 是否为数据库实体类
     */
    private boolean isEraseTargetDomain(Object object) {
        Class<?> dataClass = object.getClass();
        if (targetFieldCache.containsKey(dataClass)) {
            return true;
        }
        EraseTarget eraseTarget = dataClass.getAnnotation(EraseTarget.class);
        if (Objects.nonNull(eraseTarget)) {
            targetFieldCache.put(dataClass, new ArrayList<>());
            return true;
        }
        return false;
    }

    /**
     * 判断是否为默认时间
     *
     * @param dateValue 数据库时间值
     * @return 是否为默认时间
     */
    private boolean isDefaultDate(Date dateValue) {
        if (null == dateValue) {
            return false;
        }
        return dateValue.getTime() == DEFAULT_TIME_MILLIS;
    }

    @Override
    public Object plugin(Object o) {
        return Plugin.wrap(o, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}