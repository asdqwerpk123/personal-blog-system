[CmdletBinding()]
param(
    [switch]$RunChecks
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot ".."))
$hookPath = Join-Path $repoRoot ".githooks\pre-commit"
$failureDir = Join-Path $repoRoot "failure_logs"

if (-not (Test-Path $hookPath -PathType Leaf)) {
    throw "Missing hook entrypoint: $hookPath"
}

if (-not (Test-Path $failureDir -PathType Container)) {
    New-Item -ItemType Directory -Path $failureDir | Out-Null
}

Set-Location $repoRoot
& git -C $repoRoot config core.hooksPath .githooks | Out-Null

Write-Host "Harness enabled." -ForegroundColor Green
Write-Host "hooksPath -> .githooks"

if ($RunChecks) {
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "repo-lint.ps1")
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "doc-gardening.ps1")
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "agent-preflight.ps1") -Quiet
}
else {
    Write-Host "Before any task, run '.\scripts\agent-preflight.ps1'." -ForegroundColor DarkGray
    Write-Host "Run '.\scripts\repo-lint.ps1' and '.\scripts\doc-gardening.ps1' to validate the Harness locally." -ForegroundColor DarkGray
}
