package com.awan.feature.marketplace.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanRemoteImage
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.rememberHapticClick
import com.awan.app.core.model.StoreItem
import com.awan.feature.marketplace.impl.R

@Composable
fun StoreItemCard(
    item: StoreItem,
    isOwned: Boolean,
    isEquipped: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticClick = rememberHapticClick(onClick)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(AwanTheme.colors.surface)
            .border(1.dp, AwanTheme.colors.line, RoundedCornerShape(24.dp))
            .clickable(onClick = hapticClick)
            .padding(12.dp)
    ) {
        // Slot Badge at top left
        Surface(
            color = Color(0xFFF1F5F9),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            AwanText(
                text = item.type.name.lowercase().replaceFirstChar { it.uppercase() },
                style = AwanTheme.styles.captionText.textStyle.copy(
                    fontSize = 10.sp,
                    color = AwanTheme.colors.sky,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AwanTheme.colors.background),
                contentAlignment = Alignment.Center
            ) {
                AwanRemoteImage(
                    url = item.image,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            AwanText(
                text = item.name,
                style = AwanTheme.styles.headingText.textStyle.copy(fontSize = 14.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Price or Status Badge
            if (isEquipped) {
                StatusBadge(
                    text = stringResource(R.string.marketplace_status_equipped),
                    color = Color(0xFF22C55E),
                    icon = Icons.Default.Check
                )
            } else if (isOwned) {
                StatusBadge(
                    text = stringResource(R.string.marketplace_status_owned),
                    color = AwanTheme.colors.sky
                )
            } else {
                PriceBadge(price = item.price)
            }
        }
    }
}
