[![wercker status](https://app.wercker.com/status/4b45baa4a18cf289674fff2d3db7079a/s/master "wercker status")](https://app.wercker.com/project/bykey/4b45baa4a18cf289674fff2d3db7079a) 
[![Download](https://api.bintray.com/packages/bytabit/generic/fiat-trader/images/download.svg) ](https://bintray.com/bytabit/generic/fiat-trader/_latestVersion)

Bytabit Mobile
===================

### Clone Project

```
git clone git@bitbucket.org:bytabit/bytabit-mobile.git 
```

### Install projects dependencies

1. Install [JDK 8u111](https://jdk8.java.net/download.html)
2. Install [Gradle version 3.3](https://gradle.org/gradle-download/)
3. Verify your JAVA_HOME environment variable is set to your JDK home

### Run trader client with Gradle using default (testnet) config

```
gradle run
```

### Run client on regtest network with Grade using custom config pubName

```
gradle clean run -PbtcNetwork=regtest -PconfigName=tester2
```

### Create android APK

```
gradle clean zipalignDebug
```

### IntelliJ Setup

1. Install scala and gradle plugins (if not already installed)
2. Import gradle project in IntelliJ
3. Verify the project JDK and Java Inspections settings are correct

### JavaFX Scene Builder

1. Install [JavaFX Scene Builder 2.0](http://www.oracle.com/technetwork/java/javase/downloads/index.html). Find it under “Additional Resources”.
2. Open main UI file: ```src/main/java/org/bytabit/ft/fxui/MainUI.fxml```

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