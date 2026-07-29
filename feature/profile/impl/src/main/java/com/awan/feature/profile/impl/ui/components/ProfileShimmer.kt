package com.awan.feature.profile.impl.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.ShimmerItem

@Composable
fun ProfileShimmer() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        ProfileHeaderCardShimmer()
        PreferencesCardShimmer()
        AppearanceCardShimmer()
        SettingsCardShimmer()
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun ProfileHeaderCardShimmer() {
    AwanCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ShimmerItem(modifier = Modifier.size(80.dp), shape = CircleShape)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ShimmerItem(modifier = Modifier.width(140.dp).height(24.dp))
                ShimmerItem(modifier = Modifier.width(180.dp).height(16.dp))
                ShimmerItem(modifier = Modifier.width(100.dp).height(14.dp))
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            repeat(3) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShimmerItem(modifier = Modifier.size(44.dp), shape = CircleShape)
                    ShimmerItem(modifier = Modifier.width(50.dp).height(20.dp))
                    ShimmerItem(modifier = Modifier.width(40.dp).height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun PreferencesCardShimmer() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ShimmerItem(modifier = Modifier.width(100.dp).height(16.dp))
        AwanCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShimmerItem(modifier = Modifier.size(48.dp), shape = CircleShape)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShimmerItem(modifier = Modifier.width(120.dp).height(18.dp))
                    ShimmerItem(modifier = Modifier.width(180.dp).height(14.dp))
                    ShimmerItem(
                        modifier = Modifier.fillMaxWidth(0.8f).height(6.dp),
                        shape = RoundedCornerShape(3.dp)
                    )
                }
                ShimmerItem(modifier = Modifier.size(20.dp), shape = CircleShape)
            }
            PreferenceRowDivider()
            repeat(3) { index ->
                PreferenceRowShimmer()
                if (index < 2) PreferenceRowDivider()
            }
        }
    }
}

@Composable
private fun AppearanceCardShimmer() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ShimmerItem(modifier = Modifier.width(160.dp).height(16.dp))
        AwanCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp)
        ) {
            PreferenceRowShimmer()
            PreferenceRowDivider()
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ShimmerItem(modifier = Modifier.size(32.dp), shape = CircleShape)
                    ShimmerItem(modifier = Modifier.width(60.dp).height(16.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) {
                        ShimmerItem(modifier = Modifier.width(40.dp).height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCardShimmer() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ShimmerItem(modifier = Modifier.width(90.dp).height(16.dp))
        AwanCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp)
        ) {
            repeat(2) { index ->
                PreferenceRowShimmer()
                if (index < 1) PreferenceRowDivider()
            }
        }
    }
}

@Composable
private fun PreferenceRowShimmer() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShimmerItem(modifier = Modifier.size(32.dp), shape = CircleShape)
            ShimmerItem(modifier = Modifier.width(110.dp).height(16.dp))
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ShimmerItem(modifier = Modifier.width(60.dp).height(16.dp))
            ShimmerItem(modifier = Modifier.size(18.dp), shape = CircleShape)
        }
    }
}

@Composable
private fun PreferenceRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = AwanTheme.colors.line
    )
}
