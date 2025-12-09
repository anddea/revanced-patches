# playstore-api-v2 (DEPRECATED) 

Checkout Kotlin implementaion of the same [here](https://gitlab.com/AuroraOSS/gplayapi)

Google Play Store protobuf API wrapper in java

## Build separately

    git clone https://github.com/yeriomin/play-store-api
    gradlew :assemble
    gradlew :build
    
Protobuf classes generation happens on `assemble` step, tests a run on `build` step.

## Usage

### First login

```java
        // A device definition is required to log in
        // See resources for a list of available devices
        Properties properties = new Properties();
        try {
            properties.load(getClass().getClassLoader().getSystemResourceAsStream("device-honami.properties"));
        } catch (IOException e) {
            System.out.println("device-honami.properties not found");
            return null;
        }
        PropertiesDeviceInfoProvider deviceInfoProvider = new PropertiesDeviceInfoProvider();
        deviceInfoProvider.setProperties(properties);
        deviceInfoProvider.setLocaleString(Locale.ENGLISH.toString());
        
        // Provide valid google account info
        PlayStoreApiBuilder builder = new PlayStoreApiBuilder()
            // Extend HttpClientAdapter using a http library of your choice
            .setHttpClient(new HttpClientAdapterImplementation())
            .setDeviceInfoProvider(deviceInfoProvider)
            .setEmail(email)
            .setPassword(password)
        ;
        GooglePlayAPI api = builder.build();
        
        // We are logged in now
        // Save and reuse the generated auth token and gsf id,
        // unless you want to get banned for frequent relogins
        api.getToken();
        api.getGsfId();
        
        // API wrapper instance is ready
        DetailsResponse response = api.details("com.cpuid.cpu_z");
```
        
### Further logins

```java
        // A device definition is required for routine requests too
        // See resources for a list of available devices
        Properties properties = new Properties();
        try {
            properties.load(getClass().getClassLoader().getSystemResourceAsStream("device-honami.properties"));
        } catch (IOException e) {
            System.out.println("device-honami.properties not found");
            return null;
        }
        PropertiesDeviceInfoProvider deviceInfoProvider = new PropertiesDeviceInfoProvider();
        deviceInfoProvider.setProperties(properties);
        deviceInfoProvider.setLocaleString(Locale.ENGLISH.toString());
        
        // Provide auth token and gsf id you previously saved
        PlayStoreApiBuilder builder = new PlayStoreApiBuilder()
            // Extend HttpClientAdapter using a http library of your choice
            .setHttpClient(new HttpClientAdapterImplementation())
            .setDeviceInfoProvider(deviceInfoProvider)
            .setToken(token)
            .setGsfId(gsfId)
        ;
        GooglePlayAPI api = builder.build();
        
        // API wrapper instance is ready
        DetailsResponse response = api.details("com.cpuid.cpu_z");
```
        
## License

playstore-api-v2 is Free Software: You can use, study share and improve it at your will. Specifically you can redistribute and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

## Credits
playstore-api-v2 is a fork of https://github.com/yeriomin/play-store-api
