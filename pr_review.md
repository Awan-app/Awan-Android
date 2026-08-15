Great work on this PR! The interactive bottom sheet looks very polished, and I appreciate the correct placement of the dialog inside the :feature:home:impl module since it's state-driven. It's also great to see the user-facing strings properly extracted for localization.

I've reviewed the code against our architecture guidelines and have a couple of notes:

**Action Required: Missing Typography Styles in AwanText**
Because Awan disables inherited text styles at the Compose foundation layer (ComposeFoundationFlags.isInheritedTextStyleEnabled = false), every AwanText component requires an explicit style to render correctly. I noticed a few instances in the new UI components (like UnifiedSessionTaskContent and EditSessionTaskContent) where AwanText is called without a style.
*   **Fix:** Please pass an explicit style to all new AwanText instances (e.g., style = AwanTheme.typography.bodyMedium).

**Note on Clean Architecture (For Future Tech Debt)**
Excellent job using Use Cases (GetSessionDetailUseCase, UpdateTaskDetailUseCase, etc.) for all the new features instead of touching the repository directly! I did notice that HomeViewModel still injects HomeRepository directly. I know this is a pre-existing issue and outside the scope of this PR, but let's keep it in mind for a future refactoring task so we fully align with the *"ViewModels never touch a repository"* guideline in CLAUDE.md.

Other than adding the missing text styles, the logic and domain layers look solid. Thanks!
