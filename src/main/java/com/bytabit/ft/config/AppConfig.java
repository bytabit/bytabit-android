package com.bytabit.ft.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

public class AppConfig {

    private static final Logger LOGGER = Logger.getLogger(AppConfig.class.getName());
    final Properties prop = new Properties();

    public AppConfig() {

        String propFileName = "config.properties";

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);

        if (inputStream != null) {
            try {
                prop.load(inputStream);
            } catch (IOException e) {
                LOGGER.severe(String.format("IOException reading props from: '%s'", propFileName));
            }
        } else {
            LOGGER.severe(String.format("property file '%s' not found in the classpath", propFileName));
        }
    }

    public String getVersion() {
        return prop.getProperty("version");
    }

    public String getBtcNetwork() {
        return prop.getProperty("btcNetwork");
    }
}
