package com.artchknd.tryit.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artchknd.tryit.R
import com.artchknd.tryit.ui.theme.FairweatherTheme

@Composable
fun LocationPermissionPage(
    isLoading: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    LocationStatusLayout(
        title = stringResource(R.string.location_permission_title),
        description = stringResource(R.string.location_permission_desc),
        buttonText = stringResource(R.string.location_permission_action),
        hintText = stringResource(R.string.location_permission_hint),
        isLoading = isLoading,
        onAction = onRequestPermission,
        modifier = modifier
    )
}

@Composable
fun UnsupportedRegionPage(
    isLoading: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    LocationStatusLayout(
        title = stringResource(R.string.location_unsupported_title),
        description = stringResource(R.string.location_unsupported_desc),
        buttonText = stringResource(R.string.location_unsupported_action),
        hintText = null,
        isLoading = isLoading,
        onAction = onRetry,
        modifier = modifier
    )
}

@Composable
private fun LocationStatusLayout(
    title: String,
    description: String,
    buttonText: String,
    hintText: String?,
    isLoading: Boolean,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FairweatherTheme.colors

    Box(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 28.dp)
            .padding(bottom = 96.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = description,
                color = colors.textSecondary,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            GlassButton(
                text = buttonText,
                onClick = onAction,
                enabled = !isLoading
            )

            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = colors.textPrimary,
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = stringResource(R.string.location_loading_message),
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            if (!hintText.isNullOrBlank()) {
                Text(
                    text = hintText,
                    color = colors.textMuted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}