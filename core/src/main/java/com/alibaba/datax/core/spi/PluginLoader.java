package com.alibaba.datax.core.spi;

import com.alibaba.datax.core.spi.utils.ClassUtils;
import com.alibaba.datax.core.spi.utils.Holder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * PluginLoader
 */
public class PluginLoader<T> {

    private static final Logger logger = LoggerFactory.getLogger(PluginLoader.class);

    private static final String PLUGIN_DIR = "META-INF/datax/";

    private static final ConcurrentMap<Class<?>, PluginLoader<?>> PLUGIN_LOADERS = new ConcurrentHashMap<>(64);

    private static final ConcurrentMap<Class<?>, Object> PLUGIN_INSTANCES = new ConcurrentHashMap<>(64);

    private final Class<?> type;

    private Set<Class<?>> cachedClasses = new HashSet<>();

    private List<Holder<Object>> cachedInstances;


    private final Object pluginLock = new Object();

    private PluginLoader(Class<?> type) {
        this.type = type;
        loadPluginClasses();
    }

    @SuppressWarnings("unchecked")
    public static <T> PluginLoader<T> getPluginLoader(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("Plugin type == null");
        }

        if (!type.isInterface()) {
            throw new IllegalArgumentException("Plugin type (" + type + ") is not an interface!");
        }

        if (!withPluginAnnotation(type)) {
            throw new IllegalArgumentException("Plugin type (" + type +
                    ") is not an plugin, because it is NOT annotated with @" + SPI.class.getSimpleName() + "!");
        }

        PluginLoader<T> loader = (PluginLoader<T>) PLUGIN_LOADERS.get(type);
        if (loader == null) {
            PLUGIN_LOADERS.putIfAbsent(type, new PluginLoader<T>(type));
            loader = (PluginLoader<T>) PLUGIN_LOADERS.get(type);
        }

        return loader;
    }

    private static ClassLoader findClassLoader() {
        return ClassUtils.getClassLoader(PluginLoader.class);
    }

    private static <T> boolean withPluginAnnotation(Class<T> type) {
        return type.isAnnotationPresent(SPI.class);
    }

    /**
     * Find the plugin with the given name. If the specified name is not found, then {@link IllegalStateException}
     * will be thrown.
     */
    public List<T> getOrCreatePlugin() {
        if (cachedInstances == null) {
            synchronized (pluginLock) {
                if (cachedInstances == null) {
                    cachedInstances = new ArrayList<>();
                    cachedClasses.forEach(clazz -> cachedInstances.add(createPluginInstance(clazz)));
                }
            }
        }
        List<T> instances = new ArrayList<>();
        cachedInstances.forEach(holder -> instances.add((T) holder.get()));
        return instances;
    }


    @SuppressWarnings("unchecked")
    private Holder createPluginInstance(Class<?> clazz) {
        try {
            Object o = clazz.newInstance();
            Holder holder = new Holder();
            holder.set(o);
            return holder;
        } catch (Throwable t) {
            throw new IllegalStateException("Plugin instance (class: " +
                    type + ") couldn't be instantiated: " + t.getMessage(), t);
        }
    }

    private void loadPluginClasses() {
        String fileName = PLUGIN_DIR + type.getName();
        try {
            Enumeration<java.net.URL> urls = null;
            ClassLoader classLoader = findClassLoader();

            if (classLoader != null) {
                urls = classLoader.getResources(fileName);
            } else {
                urls = ClassLoader.getSystemResources(fileName);
            }

            if (urls != null) {
                while (urls.hasMoreElements()) {
                    java.net.URL resourceUrl = urls.nextElement();
                    loadResource(classLoader, resourceUrl);
                }
            }
        } catch (Throwable t) {
            logger.error("Exception occurred when loading plugin class (interface: " +
                    type.getName() + ", description file: " + fileName + ").", t);
        }
    }

    private void loadResource(ClassLoader classLoader, java.net.URL resourceUrl) {
        try {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceUrl.openStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    final int ci = line.indexOf('#');
                    if (ci >= 0) {
                        line = line.substring(0, ci);
                    }
                    line = line.trim();
                    if (line.length() > 0) {
                        try {
                            saveInPluginClass(Class.forName(line, true, classLoader));
                        } catch (Throwable t) {
                            IllegalStateException e = new IllegalStateException("Failed to load plugin class (interface: " + type + ", class line: " + line + ") in " + resourceUrl + ", cause: " + t.getMessage(), t);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            logger.error("Exception occurred when loading plugin class (interface: " +
                    type + ", class file: " + resourceUrl + ") in " + resourceUrl, t);
        }
    }

    private void saveInPluginClass(Class<?> clazz) {
        if (!type.isAssignableFrom(clazz)) {
            throw new IllegalStateException("Error occurred when loading plugin class (interface: " +
                    type + ", class line: " + clazz.getName() + "), class "
                    + clazz.getName() + " is not subtype of interface.");
        }

        cachedClasses.add(clazz);

    }
}
