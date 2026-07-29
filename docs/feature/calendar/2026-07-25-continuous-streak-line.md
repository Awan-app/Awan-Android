# Continuous Streak Line & Today Marker Plan

**Date**: 2026-07-25  
**Feature**: Calendar (`:feature:calendar:impl`)  
**Scope**: Render continuous horizontal streak lines with circular rounded ends across contiguous streak day cells, display a smaller primary color circle inside the fire line when today's streak is active, and position deadline dots under the line.

## 1. Continuous Streak Line
- Implement `WeekRow` with a `drawBehind` canvas.
- Compute contiguous streak day runs per 7-day week row.
- Render horizontal line from center of first streak cell to center of last streak cell using `drawLine` with `cap = StrokeCap.Round` and `strokeWidth = 28.dp.toPx()`, producing rounded circular ends.

## 2. Today Streak Marker & Deadline Positioning
- If `isToday` && `isStreakDay`: draw a smaller `20.dp` primary color (`sky`) circle centered inside the `32.dp` cell region inside the fire streak line.
- If `hasDeadline`: position deadline coral dot below the cell container so it renders underneath the streak line.

## Implementation notes (what actually differed)
- **Continuous Line**: Drawn via `drawLine(brush = fireBrush, cap = StrokeCap.Round, strokeWidth = 28.dp)`.
- **Today Marker**: `20.dp` circle with `AwanTheme.colors.sky` background placed inside the `32.dp` date cell.
- **Deadline Indicators**: Sit below the cell box, rendering cleanly under the line.
- **Build Status**: `./gradlew testDebugUnitTest` executed with 0 failures (`BUILD SUCCESSFUL`).
