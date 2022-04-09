package util.bean;

import net.sf.cglib.beans.BeanCopier;
import net.sf.cglib.core.Converter;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author  cszxyang
 * @since 2020-03-27
 */
public class BeanUtils {

    // 使用 WeakHashMap 缓存,在内存不足时会自动释放
    private final static Map<String, BeanCopier> BEAN_COPIER_MAP = new WeakHashMap<>();
    private final static Map<String, Converter> CONVERTER_MAP = new WeakHashMap<>();
    private static final Object lock1 = new Object();
    private static final Object lock2 = new Object();

    private BeanUtils() {}

    /**
     * 创建 BeanCopier，并缓存
     * @param src src
     * @param target target
     * @param useConverter use converter or not
     * @return a BeanCopier instance
     */
    private static BeanCopier getBeanCopier(Object src, Object target, boolean useConverter) {
        String key = generateKey(src, target, useConverter);
        BeanCopier copier = BEAN_COPIER_MAP.get(key);
        if (null == copier) {
            synchronized (lock1) {
                copier = BEAN_COPIER_MAP.get(key);
                if (null == copier) {
                    copier = BeanCopier.create(src.getClass(), target.getClass(), useConverter);
                    BEAN_COPIER_MAP.put(key, copier);
                    System.out.println("Create BeanCopier with key:" + key);
                }
            }
        }
        return copier;
    }

    /**
     * 复制对象属性
     * @param src srcv
     * @param target target
     */
    public static void copy(Object src,Object target) {
        BeanCopier bc = getBeanCopier(src, target, false);
        bc.copy(src,target,null);
    }

    /**
     * 使用自定义的属性转换器复制对象属性
     * @param src src
     * @param target target
     * @param converter converter
     */
    public static void copy(Object src,Object target,Converter converter) {
        BeanCopier bc = getBeanCopier(src,target,true);
        bc.copy(src, target, converter);
    }

    /**
     * 对象属性复制，只复制fields中指定的属性，每个属性用逗号分隔
     * @param src src
     * @param target target
     * @param fields fields
     */
    public static void copyWithFields(Object src,Object target,final String fields) {
        BeanCopier bc = getBeanCopier(src, target,true);
        bc.copy(src, target, newConverter(src, target,fields,true));
    }

    /**
     * 对象属性复制，排除指定属性
     * @param src src
     * @param target target
     * @param fields fields
     */
    public static void copyWithoutFields(Object src, Object target, final String fields) {
        BeanCopier copier = getBeanCopier(src, target,true);
        copier.copy(src, target, newConverter(src, target, fields,false));
    }

    /**
     * new属性转换器，
     * @param fields 需要复制或排除的属性
     * @param fieldCopyFlag 属性复制标识 true:表明fields为需要复制的属性；false:表明fields是需要排除复制的属性
     * @return a Converter instance
     */
    private static Converter newConverter(Object src, Object target, final String fields, final boolean fieldCopyFlag) {
        String key = buildConverterKey(src, target, fields, fieldCopyFlag);
        Converter converter = CONVERTER_MAP.get(key);
        if (null == converter) {
            synchronized (lock2) {
                converter = CONVERTER_MAP.get(key);
                if (null == converter) {
                    converter = new Converter() {
                        @Override
                        public Object convert(Object fieldValue, Class fieldType, Object methodName) {
                            String field = methodName.toString().substring(3); // 得到属性名，如Name
                            field = field.substring(0,1).toLowerCase() + field.substring(1); // 将首字母小写
                            if ((fieldCopyFlag && fields.contains(field)) || (!fieldCopyFlag && !fields.contains(field))) {
                                return fieldValue;
                            }
                            return null;
                        }
                    };
                    CONVERTER_MAP.put(key, converter);
                    System.out.println("Created Converter with key:" + key);
                }
            }
        }
        return converter;
    }

    private static String generateKey(Object src, Object target, boolean useConverter) {
        return src.getClass().getName() + target.getClass().getName() + useConverter;
    }

    private static String buildConverterKey(Object src,Object target,String fields,boolean copyFlag) {
        String baseKey = generateKey(src, target,true);
        return baseKey + fields + copyFlag;
    }
}
