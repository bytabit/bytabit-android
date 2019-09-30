/*
 * Copyright 2019 Bytabit AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytabit.app.core.common;

import com.msopentech.thali.toronionproxy.OnionProxyContext;

import java.io.File;

import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Value
@Builder
public class AppConfig {

    private final File appStorage;

    private final String version;

    private final String btcNetwork;

    private final String configName;

    private final String baseUrl;

    private final String peerAddress;

    private final String peerPort;

    private final OnionProxyContext onionProxyContext;

    public AppConfig(File privateStorage, String version, String btcNetwork,
                     String configName, String baseUrl, String peerAddress, String peerPort,
                     OnionProxyContext onionProxyContext) {

        this.version = version;
        this.btcNetwork = btcNetwork;
        this.configName = configName;
        this.baseUrl = baseUrl;
        this.peerAddress = peerAddress.equals("null") ? null : peerAddress;
        this.peerPort = peerPort.equals("null") ? null : peerPort;
        this.onionProxyContext = onionProxyContext;

        appStorage = new File(privateStorage.getPath() + File.separator + getBtcNetwork() + File.separator + getConfigName());
        if (!privateStorage.exists() && !privateStorage.mkdirs()) {
            log.error("Can't create private storage sub-directory: {}", privateStorage.getPath());
        }
    }
}
