package com.artchknd.tryit

import android.content.Context
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.artchknd.tryit.ui.components.CurrentWeatherHeader
import com.artchknd.tryit.ui.components.CurrentWeatherSection
import com.artchknd.tryit.ui.components.FloatingNavBar
import com.artchknd.tryit.ui.components.ForecastSection
import com.artchknd.tryit.ui.components.GlassButton
import com.artchknd.tryit.ui.components.LocationPermissionPage
import com.artchknd.tryit.ui.components.LocationSummaryItem
import com.artchknd.tryit.ui.components.MainTab
import com.artchknd.tryit.ui.components.PinnedSection
import com.artchknd.tryit.ui.components.ProvideLiquidBackdrop
import com.artchknd.tryit.ui.components.SearchSection
import com.artchknd.tryit.ui.components.SettingsSection
import com.artchknd.tryit.ui.components.UnsupportedRegionPage
import com.artchknd.tryit.ui.components.WeatherBackdrop
import com.artchknd.tryit.ui.components.WeeklyForecastSection
import com.artchknd.tryit.ui.settings.LicensesScreen
import com.artchknd.tryit.ui.theme.FairweatherTheme
import com.artchknd.tryit.ui.theme.ThemePreference
import com.artchknd.tryit.weather.DailyForecast
import com.artchknd.tryit.weather.DataSource
import com.artchknd.tryit.weather.ForecastSlot
import com.artchknd.tryit.weather.LocationStatus
import com.artchknd.tryit.weather.WeatherUiState
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val UI_PREFS_NAME = "fairweather_ui_prefs"
private const val KEY_THEME_PREFERENCE = "theme_preference"
private const val OPEN_METEO_URL = "https://open-meteo.com/"

@Composable
fun MainScreen(
    state: WeatherUiState,
    onRetry: () -> Unit,
    onRequestPermission: () -> Unit,
    searchResults: List<LocationSummaryItem> = emptyList(),
    isSearching: Boolean = false,
    searchError: String? = null,
    pinnedItems: List<LocationSummaryItem>? = null,
    onSearch: ((String) -> Unit)? = null,
    onTogglePin: ((LocationSummaryItem) -> Unit)? = null
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val uiPrefs = remember(context) {
        context.getSharedPreferences(UI_PREFS_NAME, Context.MODE_PRIVATE)
    }
    var themePreference by rememberSaveable {
        val saved = uiPrefs.getString(KEY_THEME_PREFERENCE, ThemePreference.SYSTEM.name)
        val initial = ThemePreference.entries.firstOrNull { it.name == saved }
            ?: ThemePreference.SYSTEM
        mutableStateOf(initial)
    }
    val resolvedDarkTheme = when (themePreference) {
        ThemePreference.SYSTEM -> systemDark
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var hasSearched by rememberSaveable { mutableStateOf(false) }
    val localPinnedList = remember { mutableStateListOf<LocationSummaryItem>() }
    val activePinnedItems = pinnedItems ?: localPinnedList
    val pinnedIds = remember(activePinnedItems.size, activePinnedItems.toList()) {
        activePinnedItems.map { it.id }.toSet()
    }
    val handleTogglePin: (LocationSummaryItem) -> Unit = { item ->
        if (onTogglePin != null) {
            onTogglePin(item)
        } else {
            val existingIndex = localPinnedList.indexOfFirst { it.id == item.id }
            if (existingIndex >= 0) {
                localPinnedList.removeAt(existingIndex)
            } else {
                localPinnedList.add(item)
            }
        }
    }

    FairweatherTheme(darkTheme = resolvedDarkTheme) {
        val colors = FairweatherTheme.colors
        val skyBackdrop = rememberLayerBackdrop()
        val navBackdrop = rememberLayerBackdrop()
        var selectedTab by rememberSaveable { mutableStateOf(MainTab.HOME) }
        var previousTab by remember { mutableStateOf(selectedTab) }
        var showLicenses by rememberSaveable { mutableStateOf(false) }
        val isForwardTransition = selectedTab.ordinal >= previousTab.ordinal
        val formattedLocation = formatCityDistrict(state.city, state.district)

        val mainContentAlpha by animateFloatAsState(
            targetValue = if (showLicenses) 0f else 1f,
            animationSpec = tween(durationMillis = if (showLicenses) 180 else 240),
            label = "mainContentAlpha"
        )
        val mainContentShift by animateFloatAsState(
            targetValue = if (showLicenses) -0.12f else 0f,
            animationSpec = spring(dampingRatio = 0.88f, stiffness = 320f),
            label = "mainContentShift"
        )

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .layerBackdrop(navBackdrop)
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .layerBackdrop(skyBackdrop)
                        .fillMaxSize()
                ) {
                    WeatherBackdrop(
                        condition = state.condition,
                        isDark = colors.isDark,
                        conditionCode = state.conditionCode,
                        wmoCode = state.wmoCode,
                        isDay = state.isDay
                    )
                }

                ProvideLiquidBackdrop(skyBackdrop) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .focusProperties { canFocus = !showLicenses }
                                .graphicsLayer {
                                    translationX = size.width * mainContentShift
                                    alpha = mainContentAlpha
                                }
                                .drawWithContent {
                                    if (mainContentAlpha > 0.001f) {
                                        drawContent()
                                    }
                                }
                        ) {
                            PrewarmedTabHost(
                                selectedTab = selectedTab,
                                forward = isForwardTransition
                            ) { tab ->
                                when (tab) {
                                    MainTab.HOME -> HomeTabContent(
                                        state = state,
                                        onRetry = onRetry,
                                        onRequestPermission = onRequestPermission
                                    )

                                    MainTab.PINNED -> Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .safeDrawingPadding()
                                            .padding(horizontal = 20.dp)
                                            .padding(top = 12.dp)
                                    ) {
                                        PinnedSection(
                                            pinnedItems = activePinnedItems,
                                            onUnpin = handleTogglePin,
                                            onGoToSearch = {
                                                previousTab = selectedTab
                                                selectedTab = MainTab.SEARCH
                                            }
                                        )
                                    }

                                    MainTab.SEARCH -> Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .safeDrawingPadding()
                                            .padding(horizontal = 20.dp)
                                            .padding(top = 12.dp)
                                    ) {
                                        SearchSection(
                                            query = searchQuery,
                                            onQueryChange = { searchQuery = it },
                                            onSearch = { rawQuery ->
                                                val trimmed = rawQuery.trim()
                                                if (trimmed.isNotEmpty()) {
                                                    hasSearched = true
                                                    onSearch?.invoke(trimmed)
                                                }
                                            },
                                            results = searchResults,
                                            pinnedIds = pinnedIds,
                                            onTogglePin = handleTogglePin,
                                            isSearching = isSearching,
                                            searchError = searchError,
                                            hasSearched = hasSearched
                                        )
                                    }

                                    MainTab.SETTINGS -> Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .safeDrawingPadding()
                                            .verticalScroll(rememberScrollState())
                                            .padding(horizontal = 20.dp, vertical = 16.dp)
                                    ) {
                                        SettingsSection(
                                            currentCity = formattedLocation,
                                            themePreference = themePreference,
                                            onSelectTheme = { pref ->
                                                themePreference = pref
                                                uiPrefs.edit().putString(KEY_THEME_PREFERENCE, pref.name).apply()
                                            },
                                            locationStatus = state.locationStatus,
                                            onRequestPermission = onRequestPermission,
                                            dataSource = state.source,
                                            onOpenLicenses = { showLicenses = true }
                                        )
                                        Spacer(Modifier.height(116.dp))
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = showLicenses,
                            enter = slideInHorizontally(
                                animationSpec = spring(dampingRatio = 0.88f, stiffness = 320f)
                            ) { (it * 0.16f).toInt() } + fadeIn(tween(240)),
                            exit = slideOutHorizontally(
                                animationSpec = spring(dampingRatio = 0.88f, stiffness = 320f)
                            ) { (it * 0.16f).toInt() } + fadeOut(tween(180))
                        ) {
                            LicensesScreen(
                                onBack = { showLicenses = false }
                            )
                        }
                    }
                }
            }

            ProvideLiquidBackdrop(navBackdrop) {
                AnimatedVisibility(
                    visible = !showLicenses,
                    enter = fadeIn(tween(200)) + slideInVertically(
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = 320f)
                    ) { it / 2 },
                    exit = fadeOut(tween(150)) + slideOutVertically(tween(150)) { it / 2 },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                ) {
                    FloatingNavBar(
                        selectedTab = selectedTab,
                        onSelectTab = { nextTab ->
                            if (nextTab != selectedTab) {
                                previousTab = selectedTab
                                selectedTab = nextTab
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PrewarmedTabHost(
    selectedTab: MainTab,
    forward: Boolean,
    content: @Composable (MainTab) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        MainTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val slideOffset = remember { Animatable(0f) }
            val scaleAnim = remember { Animatable(if (isSelected) 1f else 0.97f) }
            val alphaAnim = remember { Animatable(if (isSelected) 1f else 0f) }

            LaunchedEffect(selectedTab) {
                if (isSelected) {
                    if (alphaAnim.value < 0.99f) {
                        slideOffset.snapTo(if (forward) 0.16f else -0.16f)
                        launch {
                            slideOffset.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(dampingRatio = 0.88f, stiffness = 320f)
                            )
                        }
                        launch {
                            scaleAnim.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(dampingRatio = 0.88f, stiffness = 320f)
                            )
                        }
                        launch {
                            alphaAnim.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(durationMillis = 240)
                            )
                        }
                    }
                } else if (alphaAnim.value > 0.001f) {
                    val exitTarget = if (forward) -0.12f else 0.12f
                    launch {
                        slideOffset.animateTo(
                            targetValue = exitTarget,
                            animationSpec = spring(dampingRatio = 0.88f, stiffness = 320f)
                        )
                    }
                    launch {
                        scaleAnim.animateTo(
                            targetValue = 0.97f,
                            animationSpec = spring(dampingRatio = 0.88f, stiffness = 320f)
                        )
                    }
                    launch {
                        alphaAnim.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 180)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(if (isSelected) 1f else 0f)
                    .focusProperties { canFocus = isSelected }
                    .graphicsLayer {
                        translationX = size.width * slideOffset.value
                        scaleX = scaleAnim.value
                        scaleY = scaleAnim.value
                        alpha = alphaAnim.value
                    }
                    .drawWithContent {
                        if (alphaAnim.value > 0.001f) {
                            drawContent()
                        }
                    }
            ) {
                content(tab)

                if (!isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {}
                            )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTabContent(
    state: WeatherUiState,
    onRetry: () -> Unit,
    onRequestPermission: () -> Unit
) {
    val colors = FairweatherTheme.colors

    when (state.locationStatus) {
        LocationStatus.PERMISSION_REQUIRED -> {
            LocationPermissionPage(
                isLoading = state.isLoading,
                onRequestPermission = onRequestPermission
            )
        }

        LocationStatus.UNSUPPORTED_REGION -> {
            UnsupportedRegionPage(
                isLoading = state.isLoading,
                onRetry = onRetry
            )
        }

        LocationStatus.OK -> {
            when {
                state.error != null && state.slots.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.weather_error_title),
                                color = colors.textPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = state.error,
                                color = colors.textSecondary,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            GlassButton(
                                text = stringResource(R.string.action_retry),
                                onClick = onRetry
                            )
                        }
                    }
                }

                state.isLoading && state.slots.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = colors.textPrimary,
                                strokeWidth = 2.5.dp
                            )
                            Text(
                                text = stringResource(R.string.weather_loading_message),
                                color = colors.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                else -> {
                    val pullState = rememberPullToRefreshState()
                    val uriHandler = LocalUriHandler.current

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                            .padding(horizontal = 20.dp)
                            .padding(top = 12.dp)
                    ) {
                        CurrentWeatherHeader(
                            city = state.city,
                            district = state.district,
                            isLoading = state.isLoading
                        )

                        PullToRefreshBox(
                            isRefreshing = state.isLoading,
                            onRefresh = onRetry,
                            state = pullState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            indicator = {
                                PullToRefreshDefaults.Indicator(
                                    state = pullState,
                                    isRefreshing = state.isLoading,
                                    modifier = Modifier.align(Alignment.TopCenter),
                                    containerColor = colors.fallbackBackground,
                                    color = colors.textPrimary
                                )
                            }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CurrentWeatherSection(
                                    state = state,
                                    onRetry = onRetry
                                )

                                ForecastSection(
                                    slots = state.slots
                                )

                                if (state.daily.isNotEmpty()) {
                                    WeeklyForecastSection(
                                        daily = state.daily,
                                        todayMinTemp = state.minTemp,
                                        todayMaxTemp = state.maxTemp
                                    )
                                }

                                if (state.source == DataSource.OPEN_METEO) {
                                    Text(
                                        text = stringResource(R.string.open_meteo_attribution),
                                        color = colors.textMuted,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = { uriHandler.openUri(OPEN_METEO_URL) }
                                            )
                                            .padding(top = 4.dp)
                                    )
                                }

                                Spacer(Modifier.height(116.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatCityDistrict(city: String, district: String): String {
    val cleanCity = city.trim()
    val cleanDistrict = district.trim()
    return when {
        cleanCity.isNotEmpty() && cleanDistrict.isNotEmpty() -> "$cleanCity $cleanDistrict"
        cleanCity.isNotEmpty() -> cleanCity
        cleanDistrict.isNotEmpty() -> cleanDistrict
        else -> "--"
    }
}

@Preview(showBackground = true, name = "Success - Light")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Success - Dark")
@Composable
private fun MainScreenSuccessPreview() {
    val today = LocalDate.now()
    val sampleSlots = listOf(
        ForecastSlot(
            start = "$today 18:00:00",
            end = "${today.plusDays(1)} 06:00:00",
            condition = "多雲時晴",
            rainChance = 20,
            minTemp = 23,
            maxTemp = 27,
            comfort = "舒適"
        ),
        ForecastSlot(
            start = "${today.plusDays(1)} 06:00:00",
            end = "${today.plusDays(1)} 18:00:00",
            condition = "晴時多雲",
            rainChance = 10,
            minTemp = 22,
            maxTemp = 30,
            comfort = "悶熱"
        ),
        ForecastSlot(
            start = "${today.plusDays(1)} 18:00:00",
            end = "${today.plusDays(2)} 06:00:00",
            condition = "短暫陣雨",
            rainChance = 60,
            minTemp = 21,
            maxTemp = 25,
            comfort = "舒適"
        )
    )
    val sampleDaily = listOf(
        DailyForecast(
            date = today.toString(),
            condition = "多雲時晴",
            rainChance = 20,
            minTemp = 24,
            maxTemp = 26,
            uvIndex = 6
        ),
        DailyForecast(
            date = today.plusDays(1).toString(),
            condition = "晴時多雲",
            rainChance = 10,
            minTemp = 22,
            maxTemp = 30,
            uvIndex = 8
        ),
        DailyForecast(
            date = today.plusDays(2).toString(),
            condition = "陰時多雲短暫陣雨或雷雨",
            rainChance = 70,
            minTemp = 21,
            maxTemp = 26,
            uvIndex = 4
        ),
        DailyForecast(
            date = today.plusDays(3).toString(),
            condition = "多雲短暫陣雨",
            rainChance = null,
            minTemp = 20,
            maxTemp = 25,
            uvIndex = null
        ),
        DailyForecast(
            date = today.plusDays(4).toString(),
            condition = "晴時多雲",
            rainChance = null,
            minTemp = 21,
            maxTemp = 28,
            uvIndex = null
        )
    )

    MainScreen(
        state = WeatherUiState(
            city = "臺南市",
            district = "中西區",
            locationStatus = LocationStatus.OK,
            tempNow = null,
            condition = "多雲時晴",
            rainChance = 20,
            minTemp = 23,
            maxTemp = 27,
            slots = sampleSlots,
            daily = sampleDaily,
            isLoading = false,
            error = null
        ),
        onRetry = {},
        onRequestPermission = {},
        searchResults = listOf(
            LocationSummaryItem(
                city = "高雄市",
                district = "前鎮區",
                condition = "晴時多雲",
                minTemp = 24,
                maxTemp = 30,
                rainChance = 10
            )
        ),
        pinnedItems = listOf(
            LocationSummaryItem(
                city = "臺北市",
                district = "信義區",
                condition = "陰時多雲短暫陣雨",
                minTemp = 19,
                maxTemp = 23,
                rainChance = 50
            )
        )
    )
}

@Preview(showBackground = true, name = "Global Night - Open-Meteo")
@Composable
private fun MainScreenGlobalNightPreview() {
    val today = LocalDate.now()
    MainScreen(
        state = WeatherUiState(
            city = "Tokyo",
            district = "Shinjuku, Japan",
            tempNow = 14,
            feelsLike = 12,
            condition = "Clear sky",
            rainChance = 5,
            minTemp = 11,
            maxTemp = 18,
            wmoCode = 0,
            isDay = false,
            source = DataSource.OPEN_METEO,
            slots = listOf(
                ForecastSlot(
                    start = "$today 18:00:00",
                    end = "${today.plusDays(1)} 06:00:00",
                    condition = "Clear sky",
                    rainChance = 5,
                    minTemp = 11,
                    maxTemp = 15,
                    comfort = "舒適",
                    wmoCode = 0
                )
            ),
            daily = listOf(
                DailyForecast(
                    date = today.toString(),
                    condition = "Clear sky",
                    rainChance = 5,
                    minTemp = 11,
                    maxTemp = 18,
                    uvIndex = 4,
                    wmoCode = 0
                )
            ),
            isLoading = false
        ),
        onRetry = {},
        onRequestPermission = {}
    )
}

@Preview(showBackground = true, name = "Loading")
@Composable
private fun MainScreenLoadingPreview() {
    MainScreen(
        state = WeatherUiState(
            locationStatus = LocationStatus.OK,
            isLoading = true
        ),
        onRetry = {},
        onRequestPermission = {}
    )
}

@Preview(showBackground = true, name = "Error")
@Composable
private fun MainScreenErrorPreview() {
    MainScreen(
        state = WeatherUiState(
            locationStatus = LocationStatus.OK,
            isLoading = false,
            error = "無法連線至氣象伺服器，請檢查網路後再試"
        ),
        onRetry = {},
        onRequestPermission = {}
    )
}

@Preview(showBackground = true, name = "Permission Required")
@Composable
private fun MainScreenPermissionPreview() {
    MainScreen(
        state = WeatherUiState(
            locationStatus = LocationStatus.PERMISSION_REQUIRED,
            isLoading = false
        ),
        onRetry = {},
        onRequestPermission = {}
    )
}

@Preview(showBackground = true, name = "Unsupported Region")
@Composable
private fun MainScreenUnsupportedRegionPreview() {
    MainScreen(
        state = WeatherUiState(
            locationStatus = LocationStatus.UNSUPPORTED_REGION,
            isLoading = false
        ),
        onRetry = {},
        onRequestPermission = {}
    )
}