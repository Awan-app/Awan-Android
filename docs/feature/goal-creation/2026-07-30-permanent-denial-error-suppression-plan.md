# Permanent Permission Denial Error Suppression Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Suppress the microphone permission error message under `AwanTextField` when permission is permanently denied, displaying only the Settings dialog. If the user dismisses/cancels the dialog without enabling the permission, display the field error under the input field.

**Architecture:** `SpeechRecognizerHelper` checks `ActivityCompat.shouldShowRequestPermissionRationale`. If `shouldShowRationale == false` upon permission denial, `showSettingsDialog` becomes `true` while `isPermissionError` remains `false`. If the dialog's `onDismiss` callback fires, `isPermissionError` is set to `true` and `errorMessage` is set to the permission error string.

**Tech Stack:** Kotlin, Jetpack Compose, `SpeechRecognizerHelper`, `AwanConfirmDialog`.

## Global Constraints
- Branch: `feature/goal-creation`
- Commit prefix: `AWAN-83:`
- No new external dependencies

---

### Task 1: Update SpeechRecognizerHelper Error Suppression Logic

**Files:**
- Modify: `feature/add-task/src/main/java/com/awan/feature/addtask/ui/components/SpeechRecognizerHelper.kt`

- [ ] **Step 1: Update `SpeechRecognizerHelper.kt` logic**

  In `permissionLauncher` callback when `!isGranted`:
  ```kotlin
  val activity = context.findActivity()
  val shouldShowRationale = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
      activity,
      Manifest.permission.RECORD_AUDIO,
  )
  if (!shouldShowRationale) {
      showSettingsDialog = true
      isPermissionError = false
      errorMessage = null
  } else {
      isPermissionError = true
      errorMessage = context.getString(R.string.add_task_goal_speech_permission_denied)
  }
  ```

  In `startListeningAction` when `!hasPermission`:
  ```kotlin
  val activity = context.findActivity()
  val shouldShowRationale = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
      activity,
      Manifest.permission.RECORD_AUDIO,
  )
  if (!shouldShowRationale && errorMessage != null) {
      showSettingsDialog = true
      isPermissionError = false
      errorMessage = null
  } else {
      permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
  }
  ```

  In `AwanConfirmDialog`:
  - `onConfirm`:
    ```kotlin
    showSettingsDialog = false
    isPermissionError = false
    errorMessage = null
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
    ```
  - `onDismiss`:
    ```kotlin
    showSettingsDialog = false
    isPermissionError = true
    errorMessage = context.getString(R.string.add_task_goal_speech_permission_denied)
    ```

- [ ] **Step 2: Verify compilation and tests**

  ```cmd
  .\gradlew.bat :feature:add-task:testDebugUnitTest :app:compileDebugKotlin --no-daemon --console=plain
  ```

- [ ] **Step 3: Commit**

  ```cmd
  git add feature/add-task/src/main/java/com/awan/feature/addtask/ui/components/SpeechRecognizerHelper.kt
  git commit -m "AWAN-83: suppress mic permission error on permanent denial until settings dialog dismissed"
  ```
