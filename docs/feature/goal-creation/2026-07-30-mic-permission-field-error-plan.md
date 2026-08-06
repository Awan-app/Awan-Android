# Microphone Permission Field Error & Settings Dialog Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move microphone permission errors directly under `AwanTextField` input fields as field errors with red rim styling, and present an `AwanConfirmDialog` directing users to Android Settings when microphone permission is permanently denied.

**Architecture:** `SpeechRecognizerHelper` detects permission rationale state via `ActivityCompat.shouldShowRequestPermissionRationale`. When permanently denied, it exposes `isPermissionError = true` and shows `AwanConfirmDialog` with an "Open Settings" action. `GoalForm` and `GoalPreviewScreen` pass `isError = speechState.isPermissionError` to `AwanTextField` and render the error message directly under the input field.

**Tech Stack:** Kotlin, Jetpack Compose, Android Permissions (`ActivityCompat.shouldShowRequestPermissionRationale`), `AwanConfirmDialog`, `AwanTextField`.

## Global Constraints
- Branch: `feature/goal-creation`
- Commit prefix: `AWAN-83:`
- No new external dependencies
- Preserve speech recognizer functionality and existing error strings
- Add Arabic translations for all new strings

---

### Task 1: SpeechRecognizerHelper Permanent Denial & Dialog

**Files:**
- Modify: `feature/add-task/src/main/java/com/awan/feature/addtask/ui/components/SpeechRecognizerHelper.kt`
- Modify: `feature/add-task/src/main/res/values/strings.xml`
- Modify: `feature/add-task/src/main/res/values-ar/strings.xml`

**Interfaces:**
- Produces: `SpeechRecognizerState(isListening: Boolean, errorMessage: String?, isPermissionError: Boolean, startListeningAction: () -> Unit, stopListeningAction: () -> Unit)`

- [ ] **Step 1: Add string resources**

  In `feature/add-task/src/main/res/values/strings.xml`, add:
  ```xml
  <string name="add_task_goal_permission_dialog_title">Microphone Permission Required</string>
  <string name="add_task_goal_permission_dialog_body">Microphone access is turned off. Please enable it in device settings to use speech recognition.</string>
  <string name="add_task_goal_permission_dialog_confirm">Open Settings</string>
  <string name="add_task_goal_permission_dialog_cancel">Cancel</string>
  ```

  In `feature/add-task/src/main/res/values-ar/strings.xml`, add:
  ```xml
  <string name="add_task_goal_permission_dialog_title">إذن الميكروفون مطلوب</string>
  <string name="add_task_goal_permission_dialog_body">تم إيقاف إذن الميكروفون. يرجى تفعيله من إعدادات التطبيق لاستخدام الإملاء الصوتي.</string>
  <string name="add_task_goal_permission_dialog_confirm">فتح الإعدادات</string>
  <string name="add_task_goal_permission_dialog_cancel">إلغاء</string>
  ```

- [ ] **Step 2: Update `SpeechRecognizerHelper.kt`**

  - Add `val isPermissionError: Boolean = false` to `SpeechRecognizerState`.
  - Add state `var isPermissionError by remember { mutableStateOf(false) }` and `var showSettingsDialog by remember { mutableStateOf(false) }`.
  - In `permissionLauncher`, if `!isGranted`:
    Check `shouldShowRequestPermissionRationale`. If `!canShowRationale`, set `showSettingsDialog = true`.
  - In `startListeningAction`:
    If `!hasPermission`:
      Check `shouldShowRequestPermissionRationale`. If `!canShowRationale`, set `showSettingsDialog = true` and `isPermissionError = true`. Otherwise launch `permissionLauncher`.
  - When `showSettingsDialog == true`, render `AwanConfirmDialog`:
    - Confirm -> launch `Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))` and `showSettingsDialog = false`.
    - Cancel -> `showSettingsDialog = false`.

- [ ] **Step 3: Verify compilation**

  ```cmd
  .\gradlew.bat :feature:add-task:compileDebugKotlin --no-daemon --console=plain
  ```

- [ ] **Step 4: Commit**

  ```cmd
  git add feature/add-task/src/main/java/com/awan/feature/addtask/ui/components/SpeechRecognizerHelper.kt feature/add-task/src/main/res/values/strings.xml feature/add-task/src/main/res/values-ar/strings.xml
  git commit -m "AWAN-83: add mic permission permanent denial rationale check and settings dialog"
  ```

---

### Task 2: Field Error UI Placement in GoalForm & GoalPreviewScreen

**Files:**
- Modify: `feature/add-task/src/main/java/com/awan/feature/addtask/ui/components/GoalForm.kt`
- Modify: `feature/add-task/src/main/java/com/awan/feature/addtask/ui/GoalPreviewScreen.kt`

**Interfaces:**
- Consumes: `speechState.isPermissionError`, `speechState.errorMessage`

- [ ] **Step 1: Update `GoalForm.kt`**

  - Update step content composables (`InitialStepContent`, `MultipleChoiceStepContent`, `WritingStepContent`, `PreviewStepContent`) to pass `isError = speechState.isPermissionError` to `AwanTextField`.
  - Display `speechError` directly below each step's `AwanTextField` inside the step's Column container as red error text (`AwanTheme.styles.errorText`), rather than at the bottom of the form.

- [ ] **Step 2: Update `GoalPreviewScreen.kt`**

  - Pass `isError = speechState.isPermissionError` to `AwanTextField` in the bottom revision footer.
  - Display `speechError` directly under `AwanTextField` inside the bottom footer Column container as red error text (`AwanTheme.styles.errorText`).

- [ ] **Step 3: Run build and unit tests**

  ```cmd
  .\gradlew.bat :feature:add-task:testDebugUnitTest :app:compileDebugKotlin --no-daemon --console=plain
  ```

- [ ] **Step 4: Commit**

  ```cmd
  git add feature/add-task/src/main/java/com/awan/feature/addtask/ui/components/GoalForm.kt feature/add-task/src/main/java/com/awan/feature/addtask/ui/GoalPreviewScreen.kt
  git commit -m "AWAN-83: display mic permission error as field error directly under input field"
  ```

---

## Self-Review Checklist

1. **Spec coverage:**
   - [x] Permission error shown under input field as field error -> Task 2
   - [x] Permanently denied permission opens dialog directing to settings -> Task 1
   - [x] Field error sets `isError = true` on `AwanTextField` -> Task 2
   - [x] Arabic translations added -> Task 1
2. **Placeholder scan:** None
3. **Type consistency:** Matches `AwanTextField(isError = ...)` and `AwanConfirmDialog` signatures.
