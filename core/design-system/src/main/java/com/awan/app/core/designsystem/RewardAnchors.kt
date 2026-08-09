package com.awan.app.core.designsystem

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * Where a reward animation starts and where it flies to, in root coordinates.
 *
 * The overlay that draws the animation lives in the app shell, while the things it animates between
 * — the tapped card, the points badge, the profile tab — are owned by screens and by the navigation
 * bar. Rather than thread pixel positions up through three layers of composable parameters, each
 * participant registers its own bounds here and the overlay reads them.
 *
 * Every slot is nullable on purpose: the badge is absent whenever the user is on another screen, and
 * the overlay is expected to fall back rather than assume a target exists.
 */
@Stable
class RewardAnchors {
    /** The points badge in the home header, when it is on screen. */
    var pointsBadge: Rect? by mutableStateOf(null)

    /** The profile tab in the bottom bar — where a won item flies. */
    var profileTab: Rect? by mutableStateOf(null)

    /**
     * Where the reward was earned, so it flies out of the thing the user just tapped.
     *
     * Stamped by the tap handler rather than tracked continuously: many cards are on screen at once
     * and only the tapped one is the origin. Callers capture their own bounds and assign on click.
     */
    var lastTapOrigin: Rect? by mutableStateOf(null)

    /**
     * The running total to show while a points award is flying in, or null when none is.
     *
     * The badge holds at the pre-award figure and climbs as each star lands, which only works if the
     * animation — not the screen's own state — owns the number for that stretch. [AwanPointsBadge]
     * prefers this whenever it is set.
     */
    var animatedPoints: Int? by mutableStateOf(null)

    /** Raised as each star lands, so the badge kicks in time with the arrivals. */
    var pointsPulse: Boolean by mutableStateOf(false)
}

enum class RewardAnchor { PointsBadge, ProfileTab }

/** Publishes this composable's bounds into the given [anchors] slot as it moves and resizes. */
fun Modifier.rewardAnchor(anchor: RewardAnchor, anchors: RewardAnchors): Modifier =
    this.onGloballyPositioned { coordinates ->
        val bounds = coordinates.boundsInRoot()
        when (anchor) {
            RewardAnchor.PointsBadge -> anchors.pointsBadge = bounds
            RewardAnchor.ProfileTab -> anchors.profileTab = bounds
        }
    }

val LocalRewardAnchors = staticCompositionLocalOf { RewardAnchors() }
