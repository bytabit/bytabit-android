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

package com.bytabit.app.core.common.net;

import com.bytabit.app.core.common.AppConfig;
import com.msopentech.thali.toronionproxy.OnionProxyManager;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class TorManager extends OnionProxyManager {

    public enum State {
        IDLE,
        CONNECTED,
        DISCONNECTED,
        CONNECTING
    }

    //private final BehaviorSubject<State> torStatus = BehaviorSubject.create();

    @Inject
    public TorManager(AppConfig appConfig) {

        super(appConfig.getOnionProxyContext(), null, null);

    }

    public Observable<State> startTor() {

        return Observable.create(s -> {

            log.info("setup tor.");
            try {
                setup();
            } catch (IOException e) {
                s.onError(e);
            }

            log.info("start tor.");
            s.onNext(State.CONNECTING);

            int totalSecondsPerTorStartup = 4 * 60;
            int totalTriesPerTorStartup = 5;
            try {
                boolean ok = startWithRepeat(totalSecondsPerTorStartup, totalTriesPerTorStartup, false);
                if (!ok) {
                    log.error("Couldn't start tor");
                    throw new RuntimeException("Couldn't start tor");
                }
                while (!isRunning()) {
                    Thread.sleep(90);
                }
                //proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", onionProxyManager.getIPv4LocalHostSocksPort()));
                //currentPort = onionProxyManager.getIPv4LocalHostSocksPort();

                s.onNext(State.CONNECTED);
                //isProcessRunning = true;
                //state = CONNECTION_STATES.CONNECTED;
                s.onComplete();

                //return proxy;
            } catch (Exception e) {
                //e.printStackTrace();

                if (!isRunning()) {
                    log.error("tor is not running:", e);
                    s.onNext(State.DISCONNECTED);
                }
                s.onError(e);
//                e.printStackTrace();
//                return proxy;
            }
        });

    }

}