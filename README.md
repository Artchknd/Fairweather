# Fairweather

A weather app for Android, built with Kotlin and Jetpack Compose. Fairweather detects where you are, shows current conditions, a 36-hour forecast and a weekly forecast, and wraps it all in a liquid glass interface. In Taiwan it uses official forecasts from the Central Weather Administration (CWA); anywhere else in the world it uses Open-Meteo.

English | [繁體中文](README.zh-TW.md)

<!-- Add screenshots here, for example:
<p>
  <img src="docs/screenshots/home.png" width="240" />
  <img src="docs/screenshots/search.png" width="240" />
</p>
-->

## Features

- Automatic location: resolves your county and district in Taiwan, or your city anywhere else, and loads the matching forecast
- 36-hour forecast with rain chance, temperature range and comfort level
- Weekly forecast, including UV index
- Search any county or district in Taiwan, or any place in the world, and pin up to 10 places
- Day and night weather icons and backdrops
- Light, dark and follow-system themes
- Traditional Chinese and English interface
- Liquid glass UI (navigation bar, cards, buttons) and an animated intro
- A clear status page when location permission is missing

## Requirements

- Android 12 (API 31) or newer
- Android Studio (current stable release)
- Optional: a free API key from the Central Weather Administration (CWA) open data platform, for calling CWA directly in debug builds. Weather outside Taiwan needs no key.

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

This applies to the Taiwan data only. Open-Meteo, used outside Taiwan, needs no key, so nothing secret is involved.

## Project structure

```
app/src/main/java/com/artchknd/tryit/
  MainActivity.kt        Entry point, splash, permission flow
  IntroScreen.kt         Animated intro
  MainScreen.kt          Tab host and navigation
  location/              Device location, county and district lookup (Taiwan)
  weather/               Taiwan: CWA repositories, search, pinned places,
                         view model
  weather/global/        Outside Taiwan: Open-Meteo client, place search,
                         weather code mapping
  widget/                Home screen widget
  ui/theme/              Colors, typography, theme preference
  ui/components/         Liquid glass system, weather icons, sections
  ui/settings/           Settings sub pages (open source licenses)
```

The two data sources are kept separate. The view model picks one based on where you are: inside Taiwan it uses the CWA code in `weather/`, and anywhere else it uses `weather/global/`. Both produce the same UI state, so the screens do not care which one supplied the data.

## Data sources

- Taiwan: Central Weather Administration open data (36-hour forecast and township weekly forecast datasets)
- Taiwan location lookup: National Land Surveying and Mapping Center (NLSC) township and village point query
- Outside Taiwan: [Open-Meteo](https://open-meteo.com/) forecast API, and its Geocoding API for place search. Place names come from GeoNames.
- Place names for your current position outside Taiwan: the system geocoder on your device

Weather data by [Open-Meteo.com](https://open-meteo.com/), licensed under [CC BY 4.0](https://open-meteo.com/en/licence). The data is processed for display (rounding, and conversion of weather codes to text and icons).

Forecasts outside Taiwan come from numerical weather models rather than an official national forecast, and the app does not show weather warnings for those places.

Open-Meteo's free API is for open-source and non-commercial use, under fair-use limits (10,000 requests per day). If you fork Fairweather for a commercial product, you need a commercial plan from Open-Meteo; see their [terms](https://open-meteo.com/en/terms).

Fairweather is an independent project and is not affiliated with or endorsed by the CWA, the NLSC or Open-Meteo. Please follow the terms of use of each data provider.

## Privacy

Your location is resolved on the device. What leaves the device depends on where you are:

- Near Taiwan: coordinates rounded to about 110 m are sent to the NLSC to find your county and district. The forecast requests that follow carry district names, never coordinates.
- Outside Taiwan: coordinates rounded to about 1 km are sent to Open-Meteo for the forecast. The device's system geocoder, which is provided by your phone and usually backed by Google Play services, turns the position into a place name.
- Search text you type is sent to Open-Meteo's geocoding service. Chinese search text is also matched against Taiwan districts through CWA.
- Pinned places are stored only on the device.

## Third-party software

- [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) (`io.github.kyant0:backdrop`) by Kyant, Apache License 2.0, used unmodified as a dependency
- AndroidX and Material 3, Apache License 2.0
- [Open-Meteo](https://open-meteo.com/) weather data, CC BY 4.0, and geocoding data built on [GeoNames](https://www.geonames.org/), CC BY 4.0

Full notices are in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and inside the app under Settings.

## License

Released under the Apache License 2.0. See [LICENSE](LICENSE).
