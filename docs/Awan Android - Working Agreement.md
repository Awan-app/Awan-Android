# Awan Android - Working Agreement

## Before changing code

1. Start at `docs/README.md`, choose the relevant note, and continue from there. Never return to `AGENTS.md` or `CLAUDE.md` during the task; they are entry pointers, not additional guidance.
2. Read [[Awan Android - Decision Rules]] and inspect the existing implementation before adding code.
3. Reuse the shared module, component, convention plugin, result type, or stack choice that already owns the concern.
4. For visual work, start with `:core:design-system`; for navigation, use Navigation 3; for auth, preserve the authenticated and unauthenticated Retrofit client split.

## Before committing

- Run the smallest relevant Gradle check; use `assembleDebug`, local unit tests, instrumentation, lint, or Detekt as appropriate.
- An `AWAN-*` Jira key is required in the branch name or commit message.
- Keep unrelated work out of the commit.

## Capture durable knowledge

Add a note only when a decision changes future work. Link it from [[Awan Android]] or [[Awan Android - Architecture]] and keep it to context, decision, reason, and consequences. Do not create decision records for routine implementation details.
