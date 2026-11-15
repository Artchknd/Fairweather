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
- Optional: a free API key from the Central Weather Administration (CWA) open data platform, for calling CWA directly in debug builds

## Build from source

1. Optional: get a key. Register at <https://opendata.cwa.gov.tw/> and copy your authorization code.
2. Clone the repository:

   ```
   git clone https://github.com/Artchknd/Fairweather.git
   cd Fairweather
   ```

3. Optional: create `local.properties` from the template and set your key:

   ```
   cp local.properties.example local.properties
   ```

   On Windows PowerShell use `Copy-Item local.properties.example local.properties`. Then edit the file:

   ```
   CWA_API_KEY=your_key_here
   ```

   `local.properties` is git-ignored. Alternatively, set a `CWA_API_KEY` environment variable, which is used when the property is missing. The key is used by debug builds only.
4. Open the project in Android Studio and run the `app` configuration, or build from the command line:

   ```
   ./gradlew assembleDebug
   ```

You can skip the key entirely: without one, the app falls back to the project's public proxy, so it builds and runs out of the box.

### About the API key and the proxy

An API key compiled into an APK can be extracted by anyone who has the APK, so release builds never contain one. They call a small proxy, [fairweather-proxy](https://github.com/Artchknd/fairweather-proxy), a Vercel function that keeps the key on the server, only allows the forecast datasets the app uses, and caches responses.

Debug builds call CWA directly when `CWA_API_KEY` is set, and use the proxy otherwise. A debug APK built with your key contains that key, so do not share it. To run against your own proxy, deploy `fairweather-proxy`, set `CWA_API_KEY` in its Vercel environment variables, and add `PROXY_BASE_URL=https://your-proxy.vercel.app` to `local.properties`. Never commit your key.

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
