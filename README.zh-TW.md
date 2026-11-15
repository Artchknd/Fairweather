# Fairweather

使用 Kotlin 與 Jetpack Compose 開發的 Android 天氣 App。自動偵測你所在的位置，顯示即時天氣、未來 36 小時預報與本週預報，並以液態玻璃風格呈現。在台灣使用中央氣象署（CWA）的官方預報，台灣以外的地方使用 Open-Meteo。

[English](README.md) | 繁體中文

<!-- 在這裡放截圖，例如：
<p>
  <img src="docs/screenshots/home.png" width="240" />
  <img src="docs/screenshots/search.png" width="240" />
</p>
-->

## 功能

- 自動定位：在台灣判斷所在縣市與鄉鎮，在其他地方判斷所在城市，並載入對應預報
- 未來 36 小時預報：降雨機率、溫度範圍、舒適度
- 本週預報，含紫外線指數
- 搜尋台灣任一縣市或鄉鎮，或世界上任何地點，最多可固定 10 個地區
- 日間與夜間的天氣圖示與背景
- 淺色、深色與跟隨系統主題
- 繁體中文與英文介面
- 液態玻璃 UI（導覽列、卡片、按鈕）與開機動畫
- 未授權定位時，顯示清楚的狀態頁

## 需求

- Android 12（API 31）以上
- Android Studio（目前穩定版）
- 選用：中央氣象署開放資料平臺的免費 API 授權碼（debug 建置可直接連氣象署）。台灣以外的天氣不需要授權碼。

## 從原始碼建置

1. 選用：申請授權碼。到 <https://opendata.cwa.gov.tw/> 註冊並複製你的授權碼。
2. 下載專案：

   ```
   git clone https://github.com/Artchknd/Fairweather.git
   cd Fairweather
   ```

3. 選用：由範本建立 `local.properties` 並設定授權碼：

   ```
   cp local.properties.example local.properties
   ```

   Windows PowerShell 請用 `Copy-Item local.properties.example local.properties`。接著編輯該檔：

   ```
   CWA_API_KEY=your_key_here
   ```

   `local.properties` 已加入 `.gitignore`，不會被提交。也可以改設定環境變數 `CWA_API_KEY`，在檔案沒有該欄位時會使用它。授權碼只會用在 debug 建置。
4. 用 Android Studio 開啟專案並執行 `app`，或用命令列建置：

   ```
   ./gradlew assembleDebug
   ```

不設定授權碼也可以：App 會改用專案的公開代理，所以下載後可以直接編譯並執行。

### 關於 API 授權碼與代理

編進 APK 的授權碼，任何拿到 APK 的人都能取出，所以 release 建置**不會**包含授權碼。它們改呼叫一個小型代理 [fairweather-proxy](https://github.com/Artchknd/fairweather-proxy)（Vercel 函式）：授權碼保存在伺服器端，只放行 App 用到的預報資料集，並快取回應。

debug 建置在設定了 `CWA_API_KEY` 時直接呼叫氣象署，沒有設定則走代理。用你的授權碼建出的 debug APK 內含該授權碼，請不要分享。要改用自己的代理，請部署 `fairweather-proxy`、在 Vercel 環境變數設定 `CWA_API_KEY`，並在 `local.properties` 加上 `PROXY_BASE_URL=https://your-proxy.vercel.app`。請勿提交你的授權碼。

這只適用於台灣的資料。台灣以外使用的 Open-Meteo 不需要授權碼，所以沒有需要保密的東西。

## 專案結構

```
app/src/main/java/com/artchknd/tryit/
  MainActivity.kt        進入點、啟動畫面、權限流程
  IntroScreen.kt         開機動畫
  MainScreen.kt          分頁容器與導覽
  location/              裝置定位、縣市與鄉鎮查詢（台灣）
  weather/               台灣：氣象署資料存取、搜尋、固定地區、
                         ViewModel
  weather/global/        台灣以外：Open-Meteo 用戶端、地名搜尋、
                         天氣代碼對照
  widget/                桌面小工具
  ui/theme/              色票、字型、主題偏好
  ui/components/         液態玻璃系統、天氣圖示、各區塊元件
  ui/settings/           設定子頁（開源授權）
```

兩個資料來源是分開的。ViewModel 依你所在的位置選擇：在台灣時使用 `weather/` 的氣象署程式碼，其他地方使用 `weather/global/`。兩邊產出相同的 UI 狀態，所以畫面不需要知道資料是從哪裡來的。

## 資料來源

- 台灣：中央氣象署開放資料（36 小時預報與鄉鎮一週預報資料集）
- 台灣的位置查詢：內政部國土測繪中心（NLSC）鄉鎮市區與村里點位查詢
- 台灣以外：[Open-Meteo](https://open-meteo.com/) 預報 API，以及它的 Geocoding API（地名搜尋）。地名資料來自 GeoNames。
- 台灣以外目前位置的地名：裝置的系統地理編碼服務

天氣資料來自 [Open-Meteo.com](https://open-meteo.com/)，採用 [CC BY 4.0](https://open-meteo.com/en/licence) 授權。資料經過顯示用的處理（四捨五入，並將天氣代碼轉換為文字與圖示）。

台灣以外的預報來自數值天氣模式，不是官方的國家預報，App 也不會顯示那些地方的天氣警特報。

Open-Meteo 的免費 API 僅限開源與非商業用途，且有合理使用上限（每天 10,000 次請求）。如果你要把 Fairweather 改作商業產品，需要向 Open-Meteo 取得商業方案，請見他們的[條款](https://open-meteo.com/en/terms)。

Fairweather 為獨立專案，與中央氣象署、國土測繪中心及 Open-Meteo 無隸屬或背書關係。使用時請遵守各資料提供者的使用條款。

## 隱私

你的位置在裝置上解析。哪些資料會離開裝置，取決於你在哪裡：

- 在台灣附近：四捨五入到約 110 公尺的座標會送到 NLSC，用來判斷縣市與鄉鎮。之後的預報請求只帶鄉鎮名稱，不帶座標。
- 在台灣以外：四捨五入到約 1 公里的座標會送到 Open-Meteo 取得預報。把位置轉成地名的是裝置的系統地理編碼服務，由你的手機提供，通常由 Google Play 服務支援。
- 你輸入的搜尋文字會送到 Open-Meteo 的地名搜尋服務。中文搜尋文字也會透過氣象署比對台灣鄉鎮。
- 固定的地區只存在裝置上。

## 第三方軟體

- [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)（`io.github.kyant0:backdrop`），作者 Kyant，Apache License 2.0，以依賴形式使用，未經修改
- AndroidX 與 Material 3，Apache License 2.0
- [Open-Meteo](https://open-meteo.com/) 天氣資料（CC BY 4.0），以及建立在 [GeoNames](https://www.geonames.org/) 上的地名資料（CC BY 4.0）

完整聲明見 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)，App 內「設定」頁也有。

## 授權

以 Apache License 2.0 釋出，詳見 [LICENSE](LICENSE)。
