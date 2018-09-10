package com.bytabit.mobile.config;

import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class.getName());

    private static final String PROP_FILE_NAME = "config.properties";

    private static Properties props;
    private static File privateStorage;

    private AppConfig() {

    }

    private static Properties getProps() {
        if (props == null) {
            try (InputStream inputStream = AppConfig.class.getClassLoader().getResourceAsStream(PROP_FILE_NAME)) {
                props = new Properties();
                props.load(inputStream);
            } catch (IOException ioe) {
                log.error("could not load properties {}: {}", PROP_FILE_NAME, ioe);
            }
        }
        return props;
    }

    public static String getVersion() {
        return getProps().getProperty("version");
    }

    public static String getBtcNetwork() {
        return getProps().getProperty("btcNetwork");
    }

    public static String getConfigName() {
        return getProps().getProperty("configName");
    }

    public static String getBaseUrl() {
        return getProps().getProperty("baseUrl");
    }

    public static File getPrivateStorage() {
        if (privateStorage == null) {
            try {
                File storage = Services.get(StorageService.class)
                        .flatMap(StorageService::getPrivateStorage)
                        .orElseThrow(() -> new FileNotFoundException("Could not access private storage."));
                privateStorage = new File(storage.getPath() + File.separator + getBtcNetwork() + File.separator + getConfigName());
                if (!privateStorage.exists() && !privateStorage.mkdirs()) {
                    log.warn("Can't create private storage sub-directory: {}", privateStorage.getPath());
                }
            } catch (FileNotFoundException fnfe) {
                log.error("could not get private storage {}", fnfe);
            }
        }
        return privateStorage;
    }
}
