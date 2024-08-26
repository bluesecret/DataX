package com.alibaba.datax.client.util;

import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.core.util.FrameworkErrorCode;
import com.alibaba.datax.core.util.SecretUtil;
import com.alibaba.datax.core.util.container.CoreConstant;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

public class ClassLoaderConfigParser {
    private static final String CORE_CONF = "/conf/core.json";

    private static final String PLUGIN_DESC_FILE = "plugin.json";

    private static Configuration from(String jobContent){
        Configuration config = Configuration.from(jobContent);
        return SecretUtil.decryptSecretKey(config);
    }

    /**
     * 指定Job配置路径，ConfigParser会解析Job、Plugin、Core全部信息，并以Configuration返回
     * 不同于Core的ConfigParser,这里的core,plugin 不依赖于编译后的datax.home,而是扫描程序编译后的target目录
     */
    public static Configuration parse(final String jobContent) {

        Configuration configuration = from(jobContent);
        configuration.merge(coreConfig(),
                false);

        Map<String, String> pluginTypeMap = new HashMap<>();
        String readerName = configuration.getString(CoreConstant.DATAX_JOB_CONTENT_READER_NAME);
        String writerName = configuration.getString(CoreConstant.DATAX_JOB_CONTENT_WRITER_NAME);
        pluginTypeMap.put(readerName, "reader");
        pluginTypeMap.put(writerName, "writer");

        Configuration pluginsDescConfig = null;
        try {
            pluginsDescConfig = parsePluginsConfig(pluginTypeMap);
        } catch (IOException e) {
            throw DataXException.asDataXException(e.getMessage());
        }
        configuration.merge(pluginsDescConfig, false);
        return configuration;
    }

    private static Configuration parsePluginsConfig(Map<String, String> pluginTypeMap) throws IOException {

        Configuration configuration = Configuration.newDefault();

        //最初打算通过user.dir获取工作目录来扫描插件，
        //但是user.dir在不同有一些不确定性，所以废弃了这个选择

        for (URL pluginURL : runtimeBasePackages()) {
            if (pluginTypeMap.isEmpty()) {
                break;
            }
            Configuration pluginDesc = Configuration.from(pluginURL.openStream());
            String descPluginName = pluginDesc.getString("name", "");

            if (pluginTypeMap.containsKey(descPluginName)) {
                String type = pluginTypeMap.get(descPluginName);
                configuration.merge(parseOnePlugin(pluginURL.toString(), type, descPluginName, pluginDesc), false);
                pluginTypeMap.remove(descPluginName);
            }
        }
        if (!pluginTypeMap.isEmpty()) {
            String failedPlugin = pluginTypeMap.keySet().toString();
            String message = "\nplugin %s load failed ：ry to analyze the reasons from the following aspects.。\n" +
                    "1: Check if the name of the plugin is spelled correctly, and verify whether DataX supports this plugin\n" +
                    "2：Verify if the <resource></resource> tag has been added under <build></build> section in the pom file of the relevant plugin.\n<resource>" +
                    "                <directory>src/main/resources</directory>\n" +
                    "                <includes>\n" +
                    "                    <include>**/*.*</include>\n" +
                    "                </includes>\n" +
                    "                <filtering>true</filtering>\n" +
                    "            </resource>\n [Refer to the streamreader pom file] \n" +
                    "3: Check that the datax-yourPlugin-example module imported your test plugin";
            message = String.format(message, failedPlugin);
            throw DataXException.asDataXException(FrameworkErrorCode.PLUGIN_INIT_ERROR, message);
        }
        return configuration;
    }

    /**
     * 通过classLoader获取程序编译的输出目录
     *
     * @return File[/datax-example/target/classes,xxReader/target/classes,xxWriter/target/classes]
     */
    private static URL[] runtimeBasePackages() {
        List<URL> basePackages = new ArrayList<>();
        ClassLoader classLoader = ClassUtils.getClassLoader(ClassLoaderConfigParser.class);
        try {
            Enumeration<URL> resources = classLoader.getResources(PLUGIN_DESC_FILE);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                basePackages.add(resource);
            }
        } catch (IOException e) {
            throw DataXException.asDataXException(e.getMessage());
        }
        return basePackages.toArray(new URL[0]);
    }



    private static Configuration parseOnePlugin(String packagePath,
                                                String pluginType,
                                                String pluginName,
                                                Configuration pluginDesc) {
        //设置path 兼容jarLoader的加载方式URLClassLoader
        pluginDesc.set("path", packagePath);
        pluginDesc.set("loadType", "classLoader");
        Configuration pluginConfInJob = Configuration.newDefault();
        pluginConfInJob.set(
                String.format("plugin.%s.%s", pluginType, pluginName),
                pluginDesc.getInternal());
        return pluginConfInJob;
    }

    private static Configuration coreConfig() {
        try {
            return Configuration.from(getInputStreamFromClassLoader(CORE_CONF));
        } catch (Exception ignore) {
            throw DataXException.asDataXException("Failed to load the configuration file core.json. " +
                    "Please check whether /conf/core.json exists!");
        }
    }

    private static InputStream getInputStreamFromClassLoader(String path) {
        return ClassLoaderConfigParser.class.getResourceAsStream(path);
    }
}
