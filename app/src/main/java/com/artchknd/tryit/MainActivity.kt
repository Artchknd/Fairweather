package com.artchknd.tryit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artchknd.tryit.ui.components.LocationSummaryItem
import com.artchknd.tryit.weather.PlaceForecast
import com.artchknd.tryit.weather.WeatherViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        actionBar?.hide()

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        setContent {
            val vm: WeatherViewModel = viewModel()
            val state by vm.state.collectAsStateWithLifecycle()
            LaunchedEffect(state) {
                com.artchknd.tryit.widget.FairweatherWidgetProvider.syncFromApp(this@MainActivity, state)
            }
            val searchState by vm.search.collectAsStateWithLifecycle()
            val pinned by vm.pinned.collectAsStateWithLifecycle()
            // 開機頁的狀態：introDone 一旦成立就鎖定（旋轉後不重播），其餘只在這次開機流程內有效
            var introDone by rememberSaveable { mutableStateOf(false) }
            var drawDone by remember { mutableStateOf(false) }      // 線條動畫已畫完
            var holdDone by remember { mutableStateOf(false) }      // 畫完後的最短停留已過
            var dataTimedOut by remember { mutableStateOf(false) }  // 等資料等太久，放棄等待
            var mainWarmed by remember { mutableStateOf(false) }    // 主畫面已在底下實際繪製過
            var permissionAsked by rememberSaveable { mutableStateOf(false) }
            var awaitingPermission by rememberSaveable { mutableStateOf(false) }
            var permissionBlocked by rememberSaveable { mutableStateOf(false) }
            val isDarkTheme = isSystemInDarkTheme()

            val searchItems = remember(searchState.results) {
                searchState.results.map { it.toItem() }
            }
            val pinnedItems = remember(pinned) { pinned.map { it.toItem() } }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { result ->
                awaitingPermission = false
                // 沒授權、且系統已不再顯示對話框：之後授權按鈕改為前往系統設定
                permissionBlocked = !result.values.any { it } &&
                        LOCATION_PERMISSIONS.none { shouldShowRequestPermissionRationale(it) }
                vm.refresh()
            }

            // 首次啟動時主動詢問位置權限（只問一次，避免旋轉螢幕後重複彈出）
            LaunchedEffect(Unit) {
                if (!permissionAsked && !hasLocationPermission()) {
                    permissionAsked = true
                    awaitingPermission = true
                    permissionLauncher.launch(LOCATION_PERMISSIONS)
                }
            }

            // 一次性訊息（例如固定數量已達上限）
            LaunchedEffect(vm) {
                vm.messages.collect { message ->
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                }
            }

            // === 開機頁收尾 ===
            // 線條動畫畫完後，在黑色開機頁底下先組合並繪製主畫面；資料到位、主畫面也畫過幾個影格
            // 之後才淡出開機頁，進入時不會再有第一次組合與 shader 編譯造成的卡頓。
            // 資料最多等 DATA_WAIT_TIMEOUT_MS，逾時就帶著主畫面自己的載入中畫面進入。
            val dataSettled = !state.isLoading || dataTimedOut
            val showMain = introDone || drawDone

            LaunchedEffect(drawDone) {
                if (drawDone) {
                    delay(INTRO_MIN_HOLD_MS)
                    holdDone = true
                }
            }

            LaunchedEffect(drawDone) {
                if (drawDone) {
                    delay(DATA_WAIT_TIMEOUT_MS)
                    dataTimedOut = true
                }
            }

            // 資料就緒後，等主畫面實際繪製幾個影格；資料狀態又變回載入中就重新計算
            LaunchedEffect(drawDone, dataSettled) {
                mainWarmed = false
                if (drawDone && dataSettled) {
                    repeat(WARMUP_FRAMES) { withFrameNanos { } }
                    mainWarmed = true
                }
            }

            // 等使用者回應權限對話框後才結束開機頁
            val introReady = drawDone && holdDone && dataSettled && mainWarmed && !awaitingPermission
            LaunchedEffect(introReady) {
                if (introReady) introDone = true
            }

            // 進入主畫面後，依系統深淺主題切換狀態列圖示顏色（黑底開機頁固定維持白色圖示）
            LaunchedEffect(introDone, isDarkTheme) {
                val useDarkBar = !introDone || isDarkTheme
                val style = if (useDarkBar) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(
                    statusBarStyle = style,
                    navigationBarStyle = style
                )
            }

            val requestPermission: () -> Unit = {
                if (permissionBlocked) {
                    openAppSettings()
                } else {
                    permissionLauncher.launch(LOCATION_PERMISSIONS)
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (showMain) {
                    MainScreen(
                        state = state,
                        onRetry = { vm.refresh() },
                        onRequestPermission = requestPermission,
                        searchResults = searchItems,
                        isSearching = searchState.isSearching,
                        searchError = searchState.error,
                        pinnedItems = pinnedItems,
                        onSearch = { query -> vm.searchLocation(query) },
                        onTogglePin = { item -> vm.togglePin(item.city, item.district) }
                    )
                }

                AnimatedVisibility(
                    visible = !introDone,
                    exit = fadeOut(animationSpec = tween(durationMillis = 450))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            // 擋住底下主畫面的觸控，開機頁還在時不能點到後面的按鈕
                            .pointerInput(Unit) {}
                    ) {
                        FairweatherLoadingAnimation(
                            isDataReady = dataSettled,
                            onDrawFinished = { drawDone = true }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val vm = ViewModelProvider(this)[WeatherViewModel::class.java]
        // 從系統設定回來，或使用者移動後再開啟時，位置被擋住的狀態自動重新偵測
        vm.refreshIfLocationBlocked()
        // 固定地區的預報超過 5 分鐘沒更新就重抓
        vm.refreshPinnedIfStale()
    }

    private fun hasLocationPermission(): Boolean =
        LOCATION_PERMISSIONS.any {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun openAppSettings() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null)
            )
        )
    }

    private companion object {
        val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        const val INTRO_MIN_HOLD_MS = 800L      // 線條畫完後最短停留，讓狀態文字看得到
        const val DATA_WAIT_TIMEOUT_MS = 5_000L // 畫完後最多再等資料這麼久
        const val WARMUP_FRAMES = 3             // 資料就緒後，主畫面要在底下繪製幾個影格
    }
}

// 邏輯層的 PlaceForecast 對應到 Gemini 的 UI 模型；欄位型別若不合，只需要改這一處
private fun PlaceForecast.toItem() = LocationSummaryItem(
    city = county,
    district = district,
    condition = condition,
    minTemp = minTemp,
    maxTemp = maxTemp,
    rainChance = rainChance
)