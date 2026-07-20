# Awan Android documentation

This folder is the shared Obsidian vault and the canonical AI index for the Android repository. It records durable project decisions; code, Jira, and GitHub remain authoritative for implementation and work status.

## AI routing

1. Start here for every repository task.
2. Pick the one note below that matches the task.
3. Search before opening more documentation.
4. Read only the matching note or section, then inspect the relevant code.

After reaching this file, do not route back through `AGENTS.md` or `CLAUDE.md`; those files only point here.

```powershell
rg --files docs -g '*.md'
rg -n '<keyword>' docs -g '*.md'
```

## Feature plan files

- Whenever you generate an implementation plan to create or modify a feature, save it as Markdown under `docs/plans/`.
- Treat saved plans as implementation history, not current requirements or general routing.
- Refer to a saved plan only when modifying that existing feature, and only to understand how it was implemented; verify the plan against the current code before changing anything.
- Do not consult saved plans for new features or unrelated work.

## Note index

| Note | Purpose |
| --- | --- |
| `README.md` | This canonical AI entry point, note inventory, and search workflow. |
| [[Awan Android]] | Current project state, source map, and build commands. |
| [[Awan Android - Architecture]] | Active modules, runtime flow, product boundaries, and planned architecture. |
| [[Awan Android - Decision Rules]] | Fixed technology choices and implementation rules for future Android work. |
| [[Awan Android - Working Agreement]] | The change, verification, Jira, and documentation workflow. |

## Vault rules

- Keep notes small and durable: record decisions that constrain future work.
- Link to source paths instead of copying implementation details that will drift.
- Do not store secrets, tokens, generated output, or duplicate ticket history here.
- Shared `.obsidian` settings are tracked; workspace, cache, and trash state stay local.
