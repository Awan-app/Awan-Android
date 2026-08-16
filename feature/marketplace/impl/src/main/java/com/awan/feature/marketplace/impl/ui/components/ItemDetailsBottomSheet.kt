package com.awan.feature.marketplace.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.*
import com.awan.app.core.model.StoreItem
import com.awan.app.core.model.StoreItemRarity
import com.awan.feature.marketplace.impl.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailsBottomSheet(
    item: StoreItem,
    isOwned: Boolean,
    isEquipped: Boolean,
    currentPoints: Int,
    onBuyClick: () -> Unit,
    onEquipClick: () -> Unit,
    onDismiss: () -> Unit,
    isProcessing: Boolean
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val itemTypeName = item.type.name.lowercase().replaceFirstChar { it.uppercase() }
    val rarityColor = when (item.rarity) {
        StoreItemRarity.COMMON -> AwanTheme.colors.zoneBlue
        StoreItemRarity.UNCOMMON -> AwanTheme.colors.zoneGreen
        StoreItemRarity.RARE -> AwanTheme.colors.sky
        StoreItemRarity.EPIC -> AwanTheme.colors.zoneViolet
        StoreItemRarity.LEGENDARY -> AwanTheme.colors.zoneSun
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AwanTheme.colors.surface,
        contentColor = AwanTheme.colors.textPrimary,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = AwanTheme.spacing.sm, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = AwanTheme.spacing.sm)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(AwanTheme.shapes.pill)
                    .background(AwanTheme.colors.line)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = AwanTheme.colors.textSecondary)
                }
            }

            AwanRemoteImage(
                url = item.image,
                contentDescription = item.name,
                modifier = Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(AwanTheme.colors.background)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = AwanTheme.colors.sky.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        AwanText(
                            text = itemTypeName,
                            style = AwanTheme.styles.captionText.textStyle.copy(
                                color = AwanTheme.colors.sky,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        color = rarityColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        AwanText(
                            text = item.rarity.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = AwanTheme.styles.captionText.textStyle.copy(
                                color = rarityColor,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                AwanText(
                    text = item.name,
                    style = AwanTheme.styles.titleText.textStyle.copy(fontSize = 24.sp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                AwanText(
                    text = item.description,
                    style = AwanTheme.styles.bodyText,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Price and You Have Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoMiniCard(
                    label = stringResource(R.string.marketplace_details_price),
                    value = item.price.toString(),
                    icon = Icons.Default.Star,
                    modifier = Modifier.weight(1f)
                )
                InfoMiniCard(
                    label = stringResource(R.string.marketplace_details_you_have),
                    value = currentPoints.toString(),
                    icon = Icons.Default.Star,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!isOwned) {
                StatusBox(
                    title = stringResource(R.string.marketplace_details_not_owned),
                    description = stringResource(R.string.marketplace_details_not_owned_desc, itemTypeName.lowercase())
                )
            } else if (isEquipped) {
                StatusBox(
                    title = stringResource(R.string.marketplace_details_equipped),
                    description = stringResource(R.string.marketplace_details_equipped_desc, itemTypeName.lowercase())
                )
            } else {
                StatusBox(
                    title = stringResource(R.string.marketplace_details_owned),
                    description = stringResource(R.string.marketplace_details_owned_desc, itemTypeName.lowercase())
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            val canAfford = currentPoints >= item.price

            AwanButton(
                onClick = { if (isOwned) onEquipClick() else onBuyClick() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing && (isOwned || canAfford),
                variant = if (isEquipped) AwanButtonVariant.Secondary else AwanButtonVariant.Primary
            ) {
                val label = when {
                    isEquipped -> stringResource(R.string.marketplace_details_action_equipped)
                    isOwned -> stringResource(R.string.marketplace_details_action_equip)
                    else -> stringResource(R.string.marketplace_details_action_buy, item.price)
                }
                AwanText(text = label, style = AwanTheme.styles.buttonLabel)
            }

            if (!isOwned) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = AwanTheme.colors.sky, modifier = Modifier.size(16.dp))
                    AwanText(
                        text = stringResource(R.string.marketplace_details_buy_tip),
                        style = AwanTheme.styles.captionText
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
