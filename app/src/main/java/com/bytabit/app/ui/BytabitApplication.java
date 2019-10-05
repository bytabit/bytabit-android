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

package com.bytabit.app.ui;

import android.app.Application;
import android.widget.Toast;

import com.bytabit.app.ApplicationComponent;
import com.bytabit.app.BuildConfig;
import com.bytabit.app.DaggerApplicationComponent;
import com.bytabit.app.R;
import com.bytabit.app.core.common.AppConfig;
import com.bytabit.app.core.common.PRNGFixes;
import com.bytabit.app.core.common.file.AssetManager;
import com.bytabit.app.ui.common.net.AndroidTorInstaller;
import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyContext;
import com.msopentech.thali.toronionproxy.OnionProxyContext;
import com.msopentech.thali.toronionproxy.TorConfig;
import com.msopentech.thali.toronionproxy.TorInstaller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.util.concurrent.Executors;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BytabitApplication extends Application {

    private final static String TOR_CONFIG_DIR_NAME = "torfiles";

    private ApplicationComponent applicationComponent;

//    private volatile BroadcastReceiver networkStateReceiver;
//
//    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    public void onCreate() {
        super.onCreate();

        applyPRNGFixes();

        AppConfig appConfig = AppConfig.builder()
                .appStorage(getFilesDir())
                .version(BuildConfig.VERSION_NAME)
                .btcNetwork(BuildConfig.BTC_NETWORK)
                .configName(BuildConfig.CONFIG_NAME)
                .baseUrl(BuildConfig.BASE_URL)
                .peerAddress(BuildConfig.PEER_ADDRESS)
                .peerPort(BuildConfig.PEER_PORT)
                .onionProxyContext(createOnionProxyContext())
                .assetManager(createAssetManager())
                .build();

        applicationComponent = DaggerApplicationComponent.builder()
                .appConfig(appConfig)
                .walletExecutor(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()))
                .build();

//        compositeDisposable.add(getApplicationComponent()
//                .flatMapObservable(a ->
//                        a.torManager().startTor()
//                )
//                .observeOn(Schedulers.io())
//                .doOnDispose(() -> {
//                    log.info("torManager.start disposed.");
//                })
//                .doOnComplete(() -> {
//                    log.info("torManager.start complete.");
//                    compositeDisposable.add(getApplicationComponent().subscribe(a -> {
//                                networkStateReceiver = new BytabitApplication.NetworkStateReceiver(a.torManager());
//                                IntentFilter filter = new IntentFilter(CONNECTIVITY_ACTION);
//                                getApplicationContext().registerReceiver(networkStateReceiver, filter);
//                            }
//                    ));
//                })
//                .subscribe(s -> {
//                    log.info("Tor state: {}", s.toString());
//                }));

    }

    private void applyPRNGFixes() {
        try {
            PRNGFixes.apply();
        } catch (Exception e0) {
            //
            // some Android 4.0 devices throw an exception when PRNGFixes is re-applied
            // removing provider before apply() is a workaround
            //
            Security.removeProvider("LinuxPRNG");
            try {
                PRNGFixes.apply();
            } catch (Exception e1) {
                Toast.makeText(getApplicationContext(), R.string.cannot_launch_app, Toast.LENGTH_SHORT).show();
                System.exit(0);
            }
        }
    }

    public Single<ApplicationComponent> getApplicationComponent() {
        return Single.just(applicationComponent)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io());
    }

    private OnionProxyContext createOnionProxyContext() {

        File configDir = new File(getFilesDir().getPath() + File.separator + TOR_CONFIG_DIR_NAME);
        TorConfig torConfig = new TorConfig.Builder(configDir, configDir).build();

        TorInstaller torInstaller = new AndroidTorInstaller(getApplicationContext(), configDir) {

            @Override
            public InputStream openBridgesStream() throws IOException {
                return null;
            }
        };

        return new AndroidOnionProxyContext(torConfig, torInstaller, null);
    }

    private AssetManager createAssetManager() {

        return new AssetManager() {

            android.content.res.AssetManager androidAssetManager = getAssets();

            @Override
            public InputStream open(String fileName) throws IOException {
                return androidAssetManager.open(fileName);
            }
        };
    }

//    private class NetworkStateReceiver extends BroadcastReceiver {
//
//        private TorManager torManager;
//
//        public NetworkStateReceiver(TorManager torManager) {
//            this.torManager = torManager;
//        }
//
//        @Override
//        public void onReceive(final Context ctx, final Intent i) {
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//
//                    boolean online = !i.getBooleanExtra(EXTRA_NO_CONNECTIVITY, false);
//                    if (online) {
//                        // Some devices fail to set EXTRA_NO_CONNECTIVITY, double check
//                        Object o = ctx.getSystemService(CONNECTIVITY_SERVICE);
//                        ConnectivityManager cm = (ConnectivityManager) o;
//                        NetworkInfo net = cm.getActiveNetworkInfo();
//                        if (net == null || !net.isConnected()) {
//                            online = false;
//                        }
//                    }
//                    log.info("Online: " + online);
//                    try {
//                        torManager.enableNetwork(online);
//                    } catch (IOException e) {
//                        log.warn(e.toString(), e);
//                    }
//                }
//            }).start();
//        }
//    }
}
