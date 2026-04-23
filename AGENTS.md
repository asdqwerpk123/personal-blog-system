# AGENTS

This file is the task-start contract for agents working in `personal-blog-system`.

## Before Any Task

An agent should complete this checklist before exploring, editing, or running project commands:

1. Read this file fully.
2. Read [docs/harness.md](docs/harness.md).
3. Run `powershell -ExecutionPolicy Bypass -File .\scripts\start-task.ps1`.
4. If the task needs implementation notes, open [docs/plans](docs/plans/README.md).
5. If the task needs requirements or design context, open [docs/specs](docs/specs/README.md).
6. If the task depends on earlier planning history, inspect [docs/superpowers](docs/superpowers/README.md).

Do not start editing until the task-start command succeeds.

## Directory Index

- [docs/plans](docs/plans/README.md): lightweight implementation plan entrypoint
- [docs/specs](docs/specs/README.md): lightweight design/spec entrypoint
- [docs/superpowers](docs/superpowers/README.md): historical Superpowers plans/specs and related notes

## Working Rules

- Project docs belong in `docs/` unless they are root entry files such as `README.md` or this file.
- Local automation scripts belong in `scripts/`.
- Generated failure records belong in `failure_logs/` and should not be committed.
- Pre-commit checks are enabled through `.githooks/pre-commit`.
- Preferred task-start entrypoint is `scripts/start-task.ps1`.
- Agent startup checks are handled by `scripts/agent-preflight.ps1`.

## Harness Entry Points

- Activation: `powershell -ExecutionPolicy Bypass -File .\scripts\enable-harness.ps1 -RunChecks`
- Start task: `powershell -ExecutionPolicy Bypass -File .\scripts\start-task.ps1`
- Agent preflight: `powershell -ExecutionPolicy Bypass -File .\scripts\agent-preflight.ps1`
- Manual pre-commit run: `powershell -ExecutionPolicy Bypass -File .\scripts\run-pre-commit.ps1 -AllFiles`
- Repo linter: `powershell -ExecutionPolicy Bypass -File .\scripts\repo-lint.ps1`
- Doc gardening: `powershell -ExecutionPolicy Bypass -File .\scripts\doc-gardening.ps1`

## Failure Logs

- All Harness script failures are written to `failure_logs/*.json`.
- Keep only `.gitignore` and `README.md` under version control in that directory.
