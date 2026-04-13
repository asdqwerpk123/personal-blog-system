[CmdletBinding()]
param(
    [switch]$Quiet
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "harness-utils.ps1")

$repoRoot = Get-HarnessRepoRoot
Set-Location $repoRoot

try {
    $hookPath = Join-Path $repoRoot ".githooks\pre-commit"
    if (-not (Test-Path $hookPath -PathType Leaf)) {
        throw "Missing hook entrypoint: $hookPath"
    }

    $hooksPath = (& git -C $repoRoot config --get core.hooksPath 2>$null)
    $isEnabled = $hooksPath -and $hooksPath.Trim() -eq ".githooks"

    if (-not $isEnabled) {
        Write-Host "start-task: Harness is not enabled. Enabling now..." -ForegroundColor Yellow
        & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "enable-harness.ps1") -RunChecks
    }
    else {
        Write-Host "start-task: Harness already enabled." -ForegroundColor Green
    }

    $preflightArgs = @(
        "-NoProfile"
        "-ExecutionPolicy"
        "Bypass"
        "-File"
        (Join-Path $PSScriptRoot "agent-preflight.ps1")
    )

    if ($Quiet) {
        $preflightArgs += "-Quiet"
    }

    & powershell.exe @preflightArgs
}
catch {
    $message = $_.Exception.Message
    $logPath = Write-HarnessFailureLog -Check "start-task" -Message $message -Metadata @{
        quiet = [bool]$Quiet
    }

    Write-Host "start-task: FAIL" -ForegroundColor Red
    Write-Host " - $message" -ForegroundColor Red
    Write-Host "failure log: $logPath" -ForegroundColor Yellow
    exit 1
}
