[CmdletBinding()]
param(
    [string[]]$Files = @()
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "harness-utils.ps1")

$repoRoot = Get-HarnessRepoRoot
Set-Location $repoRoot

$inputFiles = @($Files | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
if ($inputFiles.Count -eq 0) {
    $inputFiles = Get-StagedFiles
}

$normalizedFiles = @(
    $inputFiles |
        ForEach-Object { $_.Replace("/", "\") } |
        Sort-Object -Unique
)

$errors = [System.Collections.Generic.List[string]]::new()

foreach ($requiredPath in @("AGENTS.md", "docs", "scripts", "scripts\start-task.ps1", "scripts\agent-preflight.ps1", "failure_logs\.gitignore")) {
    if (-not (Test-Path (Join-Path $repoRoot $requiredPath))) {
        $errors.Add("Missing required Harness path: $requiredPath")
    }
}

$rootMarkdownAllowList = @("README.md", "AGENTS.md", "HELP.md")

foreach ($file in $normalizedFiles) {
    $fileName = [System.IO.Path]::GetFileName($file)
    $extension = [System.IO.Path]::GetExtension($file).ToLowerInvariant()
    $absolutePath = Join-Path $repoRoot $file

    if ($file.StartsWith("failure_logs\") -and $fileName -notin @(".gitignore", "README.md")) {
        $errors.Add("Do not commit generated failure logs: $file")
    }

    if ($extension -eq ".ps1" -and -not ($file.StartsWith("scripts\") -or $file.StartsWith(".githooks\"))) {
        $errors.Add("PowerShell scripts must live under scripts/ or .githooks/: $file")
    }

    if ($extension -eq ".java" -and -not ($file.StartsWith("personal-blog-admin\") -or $file.StartsWith("personal-blog-common\"))) {
        $errors.Add("Java files must stay inside the Maven modules: $file")
    }

    if ($extension -eq ".sql" -and -not $file.StartsWith("sql\")) {
        $errors.Add("SQL files must live under sql/: $file")
    }

    if ($extension -eq ".md") {
        $isRootAllowed = $rootMarkdownAllowList -contains $fileName
        $isDocsFile = $file.StartsWith("docs\")

        if (-not ($isRootAllowed -or $isDocsFile)) {
            $errors.Add("Markdown files must be in docs/ or the root allowlist: $file")
        }

        if (Test-Path $absolutePath -PathType Leaf) {
            $raw = [System.IO.File]::ReadAllText($absolutePath)
            if ($raw -match "(^|\W)(TODO|TBD)(\W|$)") {
                $errors.Add("Remove TODO/TBD placeholders before commit: $file")
            }
        }
    }

    if ($file.StartsWith("docs\plans\") -and $fileName -ne "README.md" -and $fileName -notmatch "^\d{4}-\d{2}-\d{2}-.+\.md$") {
        $errors.Add("Plan files must use YYYY-MM-DD-topic.md: $file")
    }

    if ($file.StartsWith("docs\specs\") -and $fileName -ne "README.md" -and $fileName -notmatch "^\d{4}-\d{2}-\d{2}-.+\.md$") {
        $errors.Add("Spec files must use YYYY-MM-DD-topic.md: $file")
    }

    if ($file.StartsWith("docs\superpowers\plans\") -and $fileName -notmatch "^\d{4}-\d{2}-\d{2}-.+\.md$") {
        $errors.Add("Historical plan files should keep YYYY-MM-DD-topic.md naming: $file")
    }

    if ($file.StartsWith("docs\superpowers\specs\") -and $fileName -notmatch "^\d{4}-\d{2}-\d{2}-.+\.md$") {
        $errors.Add("Historical spec files should keep YYYY-MM-DD-topic.md naming: $file")
    }
}

if ($errors.Count -gt 0) {
    $logPath = Write-HarnessFailureLog -Check "repo-lint" -Message "Repository convention checks failed." -Files $normalizedFiles -Metadata @{
        errors = @($errors)
    }

    Write-Host "repo-lint: FAIL" -ForegroundColor Red
    foreach ($errorLine in $errors) {
        Write-Host " - $errorLine" -ForegroundColor Red
    }
    Write-Host "failure log: $logPath" -ForegroundColor Yellow
    exit 1
}

Write-Host "repo-lint: OK" -ForegroundColor Green
