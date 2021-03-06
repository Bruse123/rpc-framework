package com.lhb.rpc.spi;

import com.lhb.rpc.exception.ServiceLoadException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * spi类加载器辅助类
 *
 * @author BruseLin
 */
@SuppressWarnings("unchecked")
public class SpiLoader<T> {

    private final static String SERVICE_DIRECTORY = "META-INF/services/";
    /**
     * 单例服务
     */
    private final static Map<String, Object> SINGLETON_SERVICES = new HashMap<>();

    /**
     * 该spi所加载的类型
     */
    private final Class<?> classType;

    /**
     * spi加载器
     */
    private final static Map<Class<?>, SpiLoader<?>> SPI_LOADER_MAP = new ConcurrentHashMap<>();

    private final Map<Class<?>, Object> INSTANCES = new ConcurrentHashMap<>();


    private SpiLoader(Class<?> classType) {
        this.classType = classType;
    }

    /**
     * 根据接口类获取spi加载器
     *
     * @param classType 服务的接口类对象
     */
    public static <S> SpiLoader<S> getSpiLoader(Class<S> classType) {
        if (classType == null) {
            throw new IllegalArgumentException("load spiService type should not be null.");
        }
        if (!classType.isInterface()) {
            throw new IllegalArgumentException("load spiService type must be an interface.");
        }
        if (classType.getAnnotation(Spi.class) == null) {
            throw new IllegalArgumentException("load spiService type must be annotated by @Spi");
        }
        SpiLoader<S> spiLoader = (SpiLoader<S>) SPI_LOADER_MAP.get(classType);
        if (spiLoader == null) {
            //未加载过此类型，创建一个spi加载器并保存
            SPI_LOADER_MAP.putIfAbsent(classType, new SpiLoader<>(classType));
            spiLoader = (SpiLoader<S>) SPI_LOADER_MAP.get(classType);
        }
        return spiLoader;
    }

    /**
     * 根据具体的实现类对象获取spi拓展的实例对象
     *
     * @param type 具体服务的类对象
     * @return 实例对象
     */
    public T getService(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException("Extension Service Class should not be null or empty.");
        }
        Object instance = INSTANCES.get(type);
        if (instance == null) {
            Collection<?> services = loadAllService(this.classType);
            for (Object service : services) {
                INSTANCES.put(service.getClass(), service);
            }
        }
        instance = INSTANCES.get(type);
        if (instance == null) {
            throw new IllegalArgumentException("Extension Service in NETA-INF/services is not exist, SPI could not load.");
        }
        return (T) instance;
    }

    /**
     * 加载路径META-INF/services/全限定类名 下所有配置的类
     */
    public synchronized static <S> Collection<S> loadAllService(Class<S> service) {
        return StreamSupport.stream(ServiceLoader.load(service).spliterator(), false)
                .map(SpiLoader::singletonFilter).collect(Collectors.toList());

    }

    /**
     * 加载路径META-INF/services/全限定类名 下第一个配置的类
     */
    public synchronized static <S> S loadService(Class<S> service) {
        return StreamSupport.stream(ServiceLoader.load(service).spliterator(), false)
                .map(SpiLoader::singletonFilter).findFirst()
                .orElseThrow(() -> new ServiceLoadException(String.format("spi加载配置类失败，Class：%s", service.getCanonicalName())));
    }

    @SuppressWarnings("unchecked")
    private static <S> S singletonFilter(S service) {
        if (service.getClass().isAssignableFrom(Singleton.class)) {
            //如果服务的类带有单例注解，判断集合是否已存在该单例对象
            Object singletonInstance = SINGLETON_SERVICES.putIfAbsent(service.getClass().getCanonicalName(), service);
            return singletonInstance == null ? service : (S) singletonInstance;
        } else {
            return service;
        }
    }

}
