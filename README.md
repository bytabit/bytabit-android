Bytabit App
===================

### Clone Project

```
git clone git@bitbucket.org:bytabit/bytabit-android.git
```

### Install Android Studio

- Android Gradle Plugin: 3.4.1
- Gradle: 5.1.1
    
### Run app on regtest network from Android Studio

Add the following to Build, Execution, Deployment -> Compiler -> Command-line Options

```
-PbtcNetwork=regtest -PpeerAddr=<regtest node ip address> -PpeerPort=18444
```

### Build signed release bundle for *testnet* config

```
gradle clean signReleaseBundle
```

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

1. connect via adb shell and run as com.bytabit.app
    
```
cd ~/Library/Android/sdk/platform-tools
./adb shell 
run-as com.bytabit.app
cd files
```
    
### Versioning

We follow the [Semantic Versioning 2.0](http://semver.org/spec/v2.0.0.html) specification for this project.