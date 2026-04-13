# Harness Guide

This repository uses a lightweight local Harness built on Git hooks and PowerShell scripts.

## Task Start Workflow

Before an agent starts a task, it should:

1. read `AGENTS.md`
2. read this file
3. run:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-task.ps1
```

The task-start command enables Harness automatically when needed, then runs preflight checks, shows the current branch, and reminds the agent which docs to read before making changes.

## What It Covers

- trailing whitespace cleanup
- single newline at end of text files
- protected branch commit guard for `main` and `master`
- repository convention linting
- markdown document validation
- failure log capture under `failure_logs/`

## Main Scripts

- `scripts/enable-harness.ps1`: enable the git hook path and optionally run safe verification
- `scripts/start-task.ps1`: single task-start entrypoint that enables Harness if needed and then runs preflight
- `scripts/agent-preflight.ps1`: task-start validation and reading reminder for agents
- `scripts/run-pre-commit.ps1`: the main pre-commit entrypoint and optional full text normalization pass
- `scripts/repo-lint.ps1`: custom repository rule linter
- `scripts/doc-gardening.ps1`: markdown link and anchor checker
- `scripts/harness-utils.ps1`: shared helpers, including failure log writing

## Daily Usage

Enable once:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\enable-harness.ps1 -RunChecks
```

Before a task:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-task.ps1
```

Run a full manual pre-commit sweep when you want text normalization plus all checks:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-pre-commit.ps1 -AllFiles
```

If a check fails, inspect the newest JSON record under `failure_logs/`.
