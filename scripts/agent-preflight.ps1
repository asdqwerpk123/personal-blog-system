[CmdletBinding()]
param(
    [switch]$Quiet
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "harness-utils.ps1")

$repoRoot = Get-HarnessRepoRoot
Set-Location $repoRoot

$requiredPaths = [ordered]@{
    "AGENTS guide" = "AGENTS.md"
    "Harness guide" = "docs\harness.md"
    "Plans entrypoint" = "docs\plans\README.md"
    "Specs entrypoint" = "docs\specs\README.md"
    "Historical docs entrypoint" = "docs\superpowers\README.md"
    "Pre-commit entrypoint" = "scripts\run-pre-commit.ps1"
}

$missing = [System.Collections.Generic.List[string]]::new()
foreach ($label in $requiredPaths.Keys) {
    $relativePath = $requiredPaths[$label]
    if (-not (Test-Path (Join-Path $repoRoot $relativePath))) {
        $missing.Add("$label missing: $relativePath")
    }
}

if ($missing.Count -gt 0) {
    $logPath = Write-HarnessFailureLog -Check "agent-preflight" -Message "Task-start Harness validation failed." -Files $requiredPaths.Values -Metadata @{
        errors = @($missing)
    }

    Write-Host "agent-preflight: FAIL" -ForegroundColor Red
    foreach ($line in $missing) {
        Write-Host " - $line" -ForegroundColor Red
    }
    Write-Host "failure log: $logPath" -ForegroundColor Yellow
    exit 1
}

$branch = (& git -C $repoRoot branch --show-current).Trim()
if ([string]::IsNullOrWhiteSpace($branch)) {
    $branch = "detached"
}

$statusLines = @(& git -C $repoRoot status --short)

Write-Host "agent-preflight: OK" -ForegroundColor Green
Write-Host "Read before work:" -ForegroundColor Cyan
Write-Host " - AGENTS.md"
Write-Host " - docs\\harness.md"
Write-Host "Open when needed:" -ForegroundColor Cyan
Write-Host " - docs\\plans\\README.md"
Write-Host " - docs\\specs\\README.md"
Write-Host " - docs\\superpowers\\README.md"

if (-not $Quiet) {
    Write-Host "Branch: $branch" -ForegroundColor DarkGray

    if ($statusLines.Count -gt 0) {
        Write-Host "Working tree already has $($statusLines.Count) change(s). Preserve existing edits." -ForegroundColor Yellow
        foreach ($line in $statusLines | Select-Object -First 10) {
            Write-Host " - $line" -ForegroundColor Yellow
        }

        if ($statusLines.Count -gt 10) {
            Write-Host " - ... ($($statusLines.Count - 10) more)" -ForegroundColor Yellow
        }
    }
    else {
        Write-Host "Working tree clean." -ForegroundColor DarkGray
    }
}
