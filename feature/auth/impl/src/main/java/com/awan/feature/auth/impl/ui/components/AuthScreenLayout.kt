package com.awan.feature.auth.impl.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.MascotState

import androidx.compose.foundation.layout.ExperimentalLayoutApi

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreenLayout(
    title: String,
    subtitle: String,
    mascotState: MascotState,
    modifier: Modifier = Modifier,
    topActionContent: (@Composable () -> Unit)? = null,
    bottomContent: (@Composable () -> Unit)? = null,
    formContent: @Composable ColumnScope.() -> Unit,
) {
    val isImeVisible = WindowInsets.isImeVisible
    val mascotHeight by animateDpAsState(
        targetValue = if (isImeVisible) 50.dp else 130.dp,
        animationSpec = tween(durationMillis = 250),
        label = "mascotHeightAnimation"
    )
    val mascotPaddingBottom by animateDpAsState(
        targetValue = if (isImeVisible) 8.dp else 24.dp,
        animationSpec = tween(durationMillis = 250),
        label = "mascotPaddingAnimation"
    )
    val subtitlePaddingBottom by animateDpAsState(
        targetValue = if (isImeVisible) 16.dp else 32.dp,
        animationSpec = tween(durationMillis = 250),
        label = "subtitlePaddingAnimation"
    )

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (topActionContent != null) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    topActionContent()
                }
            } else {
                Spacer(modifier = Modifier.height(32.dp))
            }

            if (mascotHeight > 10.dp) {
                Box(
                    modifier = Modifier
                        .height(mascotHeight)
                        .padding(bottom = mascotPaddingBottom),
                    contentAlignment = Alignment.Center,
                ) {
                    AuthenticationMascot(
                        state = mascotState,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // Centralized title and subtitle
            if (title.isNotEmpty()) {
                AwanText(
                    text = title,
                    style = AwanTheme.styles.titleText,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }

            if (subtitle.isNotEmpty()) {
                AwanText(
                    text = subtitle,
                    style = AwanTheme.styles.bodyText,
                    modifier = Modifier.padding(bottom = subtitlePaddingBottom),
                )
            }

            formContent()

            if (bottomContent != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    bottomContent()
                }
            }
        }
    }
}
