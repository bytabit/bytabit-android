[![wercker status](https://app.wercker.com/status/4b45baa4a18cf289674fff2d3db7079a/s/master "wercker status")](https://app.wercker.com/project/bykey/4b45baa4a18cf289674fff2d3db7079a) 
[![Download](https://api.bintray.com/packages/bytabit/generic/fiat-trader/images/download.svg) ](https://bintray.com/bytabit/generic/fiat-trader/_latestVersion)

Bytabit Mobile
===================

### Clone Project

```
git clone git@bitbucket.org:bytabit/bytabit-mobile.git 
```

### Install projects dependencies

1. Install Java JDK [version 8](https://jdk8.java.net/download.html)
2. Install Gradle [version 4 or later ](https://gradle.org/releases/)
3. Verify your JAVA_HOME environment variable is set to your JDK home
4. Install
4. Set a gradle property with the name ANDROID_HOME: defined in ~/.gradle/gradle.properties 
   or a system environment variable with the name ANDROID_HOME. Also increase max
   gradle memory. For example:
   
   ```   
     echo org.gradle.jvmargs=-Xms256m -Xmx4096m -Xss2m >> ~/.gradle/gradle.properties
     echo ANDROID_HOME=`echo $HOME`/Library/Android/sdk >> ~/.gradle/gradle.properties
   ```

### Run Bytabit app on desktop with Gradle using default (testnet) config

```
./gradlew run
```

### Run client on regtest network with Grade using custom config pubName

```
./gradlew clean run -PbtcNetwork=regtest -PconfigName=tester2
```

### Create android APK

```
./gradlew clean zipalignDebug
```

### JavaFX Scene Builder

1. Install [JavaFX Scene Builder](https://gluonhq.com/products/scene-builder/)
2. Open trades UI file: ```src/main/resources/com/bytabit/mobile/trade/ui/trades.fxml```

### Testnet In a Box via Docker

1. Pull bitcoin-testnet-box docker image
    
    ```
    docker pull freewil/bitcoin-testnet-box
    ```

2. Running docker container, mapping and exposing port 18444 from 19000 in our docker container 
    
    ```
    docker run -t -i -p 18444:19000 --expose 18444 freewil/bitcoin-testnet-box
    ```

3. Follow bitcoin-testnet-box [README.md](https://github.com/freewil/bitcoin-testnet-box) instructions

### ADB file system debugging

1. connect via adb shell and run as com.bytabit.mobile
    ```
    cd ~/Library/Android/sdk/platform-tools
    ./adb shell 
    run-as com.bytabit.mobile
    cd /data/data/com.bytabit.mobile
    ```
    
### Versioning

We follow the [Semantic Versioning 2.0](http://semver.org/spec/v2.0.0.html) specification for this project.