# Fairweather

A Taiwan weather app for Android, built with Kotlin and Jetpack Compose. Fairweather detects where you are, shows current conditions, a 36-hour forecast and a weekly forecast, and wraps it all in a liquid glass interface.

English | [繁體中文](README.zh-TW.md)

<!-- Add screenshots here, for example:
<p>
  <img src="docs/screenshots/home.png" width="240" />
  <img src="docs/screenshots/search.png" width="240" />
</p>
-->

## Features

- Automatic location: resolves your county and district on the device and loads the matching forecast
- 36-hour forecast with rain chance, temperature range and comfort level
- Weekly forecast per district, including UV index
- Search any county or district in Taiwan and pin up to 10 places
- Light, dark and follow-system themes
- Traditional Chinese and English interface
- Liquid glass UI (navigation bar, cards, buttons) and an animated intro
- Clear status pages when location permission is missing or you are outside Taiwan

## Requirements

- Android 12 (API 31) or newer
- Android Studio (current stable release)
- A free API key from the Central Weather Administration (CWA) open data platform

## Build from source

1. Get a key: register at <https://opendata.cwa.gov.tw/> and copy your authorization code.
2. Clone the repository:

   ```
   git clone https://github.com/Artchknd/Fairweather.git
   cd Fairweather
   ```

3. Create `local.properties` from the template and fill in your key:

   ```
   cp local.properties.example local.properties
   ```

   On Windows PowerShell use `Copy-Item local.properties.example local.properties`. Then edit the file:

   ```
   CWA_API_KEY=your_key_here
   ```

   `local.properties` is git-ignored. Alternatively, set a `CWA_API_KEY` environment variable, which is used when the property is missing.
4. Open the project in Android Studio and run the `app` configuration, or build from the command line:

   ```
   ./gradlew assembleDebug
   ```

If no key is configured the project still builds, and the app shows a message explaining that `CWA_API_KEY` is not set.

### About the API key

The key is compiled into the app through `BuildConfig`, so anyone who has your APK can extract it. Do not commit it, and do not publish an APK built with a key you are not willing to rotate. Every contributor is expected to use their own key.

## Project structure

```
app/src/main/java/com/artchknd/tryit/
  MainActivity.kt        Entry point, splash, permission flow
  IntroScreen.kt         Animated intro
  MainScreen.kt          Tab host and navigation
  weather/               Data and logic: CWA repositories, location
                         resolver, search, pinned places, view model
  ui/theme/              Colors, typography, theme preference
  ui/components/         Liquid glass system, weather icons, sections
  ui/settings/           Settings sub pages (open source licenses)
```

## Data sources

- Weather data: Central Weather Administration open data (36-hour forecast and township weekly forecast datasets)
- Location lookup: National Land Surveying and Mapping Center (NLSC) township and village point query

Fairweather is an independent project and is not affiliated with or endorsed by the CWA or the NLSC. Please follow the terms of use of each data provider.

## Third-party software

- [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) (`io.github.kyant0:backdrop`) by Kyant, Apache License 2.0, used unmodified as a dependency
- AndroidX and Material 3, Apache License 2.0

Full notices are in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and inside the app under Settings.

## License

Released under the Apache License 2.0. See [LICENSE](LICENSE).
