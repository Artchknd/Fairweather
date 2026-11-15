package com.artchknd.tryit.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artchknd.tryit.R
import com.artchknd.tryit.ui.theme.FairweatherTheme
import com.artchknd.tryit.ui.theme.ThemePreference
import com.artchknd.tryit.weather.LocationStatus

private const val LIQUID_GLASS_REPO_URL = "https://github.com/Kyant0/AndroidLiquidGlass"

@Composable
fun SettingsSection(
    currentCity: String,
    themePreference: ThemePreference,
    onSelectTheme: (ThemePreference) -> Unit,
    locationStatus: LocationStatus,
    onRequestPermission: () -> Unit,
    onOpenLicenses: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = FairweatherTheme.colors
    val uriHandler = LocalUriHandler.current
    val needsPermission = locationStatus == LocationStatus.PERMISSION_REQUIRED
    val permissionStatusText = if (needsPermission) {
        stringResource(R.string.settings_permission_denied_action)
    } else {
        stringResource(R.string.settings_permission_granted)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_section_title),
            color = colors.textPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(4.dp))

        // 一般設定
        SettingsGroupHeader(title = stringResource(R.string.settings_group_general))

        ThemeSelectorBlock(
            selected = themePreference,
            onSelect = onSelectTheme
        )

        SettingsItemDivider()

        SettingsItemRow(
            title = stringResource(R.string.settings_item_display_city),
            value = currentCity.ifBlank { "--" }
        )

        SettingsItemDivider()

        SettingsItemRow(
            title = stringResource(R.string.settings_item_data_source),
            value = stringResource(R.string.settings_value_data_source_cwa)
        )

        SettingsItemDivider()

        SettingsItemRow(
            title = stringResource(R.string.settings_item_app_name),
            value = stringResource(R.string.app_name)
        )

        Spacer(Modifier.height(8.dp))

        // 權限狀態與說明
        SettingsGroupHeader(title = stringResource(R.string.settings_group_permissions))

        SettingsItemRow(
            title = stringResource(R.string.settings_item_location_permission),
            value = permissionStatusText,
            valueColor = if (needsPermission) colors.textPrimary else colors.textMuted,
            onClick = if (needsPermission) onRequestPermission else null
        )

        Text(
            text = stringResource(R.string.settings_location_permission_desc),
            color = colors.textMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )

        Spacer(Modifier.height(8.dp))

        // 開發者名單、引用庫、開源授權與版權聲明
        SettingsGroupHeader(title = stringResource(R.string.settings_group_about))

        SettingsItemRow(
            title = stringResource(R.string.settings_item_developer),
            value = "Artchknd"
        )

        SettingsItemDivider()

        SettingsItemRow(
            title = stringResource(R.string.settings_item_architecture),
            value = "Claude"
        )

        SettingsItemDivider()

        SettingsItemRow(
            title = stringResource(R.string.settings_item_ui_design),
            value = "Gemini"
        )

        SettingsItemDivider()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { uriHandler.openUri(LIQUID_GLASS_REPO_URL) }
                )
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_item_open_source),
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "AndroidLiquidGlass (Kyant0)",
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "github.com/Kyant0/AndroidLiquidGlass",
                color = colors.textMuted,
                fontSize = 12.sp
            )
        }

        SettingsItemDivider()

        SettingsItemRow(
            title = stringResource(R.string.settings_item_licenses),
            value = stringResource(R.string.settings_item_licenses_action),
            valueColor = colors.textSecondary,
            onClick = onOpenLicenses
        )

        SettingsItemDivider()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_item_copyright),
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.settings_copyright_desc),
                color = colors.textMuted,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }

        Text(
            text = stringResource(R.string.settings_data_attribution),
            color = colors.textMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun SettingsGroupHeader(title: String) {
    val colors = FairweatherTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = colors.textMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp
        )
        HorizontalDivider(
            color = colors.textPrimary.copy(alpha = 0.16f),
            thickness = 0.8.dp
        )
    }
}

@Composable
private fun SettingsItemDivider() {
    HorizontalDivider(
        color = FairweatherTheme.colors.textPrimary.copy(alpha = 0.08f),
        thickness = 0.6.dp
    )
}

@Composable
private fun ThemeSelectorBlock(
    selected: ThemePreference,
    onSelect: (ThemePreference) -> Unit
) {
    val colors = FairweatherTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_item_theme),
            color = colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemePreference.entries.forEach { option ->
                val isSelected = option == selected
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) colors.textPrimary else colors.textMuted,
                    animationSpec = tween(180),
                    label = "themeOptionColor"
                )
                val indicatorAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    animationSpec = tween(180),
                    label = "themeOptionIndicatorAlpha"
                )

                Column(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(option) }
                        )
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(option.labelResId),
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(2.dp)
                            .alpha(indicatorAlpha)
                            .clip(CircleShape)
                            .background(colors.textPrimary)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsItemRow(
    title: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = FairweatherTheme.colors.textMuted,
    onClick: (() -> Unit)? = null
) {
    val colors = FairweatherTheme.colors
    val rowModifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 8.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    }

    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 14.sp,
            textAlign = TextAlign.End
        )
    }
}