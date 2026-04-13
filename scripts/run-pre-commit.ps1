[CmdletBinding()]
param(
    [switch]$AllFiles,
    [switch]$SkipBranchGuard
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "harness-utils.ps1")

$repoRoot = Get-HarnessRepoRoot
Set-Location $repoRoot

try {
    $currentBranch = (& git -C $repoRoot branch --show-current).Trim()
    if (-not $SkipBranchGuard -and $currentBranch -in @("main", "master")) {
        $logPath = Write-HarnessFailureLog -Check "protected-branch" -Message "Direct commits to protected branches are blocked." -Metadata @{
            branch = $currentBranch
        }

        Write-Host "pre-commit: FAIL" -ForegroundColor Red
        Write-Host " - direct commits to '$currentBranch' are blocked" -ForegroundColor Red
        Write-Host "failure log: $logPath" -ForegroundColor Yellow
        exit 1
    }

    $candidateFiles = if ($AllFiles) {
        @(& git -C $repoRoot ls-files)
    }
    else {
        Get-StagedFiles
    }

    $filesToCheck = @($candidateFiles | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    if ($filesToCheck.Count -eq 0) {
        Write-Host "pre-commit: no files to check" -ForegroundColor DarkGray
        exit 0
    }

    $fixedFiles = [System.Collections.Generic.List[string]]::new()

    foreach ($file in $filesToCheck) {
        if (-not (Test-HarnessManagedTextFile -Path $file)) {
            continue
        }

        $absolutePath = Join-Path $repoRoot $file
        if (-not (Test-Path $absolutePath -PathType Leaf)) {
            continue
        }

        if (Repair-TextFile -Path $absolutePath) {
            [void]$fixedFiles.Add($file)
            & git -C $repoRoot add -- $file | Out-Null
        }
    }

    & (Join-Path $PSScriptRoot "repo-lint.ps1") -Files $filesToCheck

    $markdownFiles = @(
        $filesToCheck |
            Where-Object { [System.IO.Path]::GetExtension($_).ToLowerInvariant() -eq ".md" }
    )

    if ($markdownFiles.Count -gt 0) {
        & (Join-Path $PSScriptRoot "doc-gardening.ps1") -Files $markdownFiles
    }

    if ($fixedFiles.Count -gt 0) {
        Write-Host "pre-commit: auto-fixed formatting in $($fixedFiles -join ', ')" -ForegroundColor Yellow
    }

    Write-Host "pre-commit: OK" -ForegroundColor Green
}
catch {
    $message = $_.Exception.Message
    $logPath = Write-HarnessFailureLog -Check "pre-commit" -Message $message -Metadata @{
        allFiles = [bool]$AllFiles
        skipBranchGuard = [bool]$SkipBranchGuard
    }

    Write-Host "pre-commit: FAIL" -ForegroundColor Red
    Write-Host " - $message" -ForegroundColor Red
    Write-Host "failure log: $logPath" -ForegroundColor Yellow
    exit 1
}
