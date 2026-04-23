[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "harness-utils.ps1")

function Get-TestDbValue {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    $processValue = [System.Environment]::GetEnvironmentVariable($Name, "Process")
    if ($processValue) {
        return $processValue
    }

    $userValue = [System.Environment]::GetEnvironmentVariable($Name, "User")
    if ($userValue) {
        return $userValue
    }

    return $null
}

$repoRoot = Get-HarnessRepoRoot
Set-Location $repoRoot

$requiredNames = @(
    "BLOG_DB_TEST_URL",
    "BLOG_DB_TEST_USERNAME",
    "BLOG_DB_TEST_PASSWORD"
)

$missingNames = New-Object System.Collections.Generic.List[string]

foreach ($name in $requiredNames) {
    $value = Get-TestDbValue -Name $name
    if ([string]::IsNullOrWhiteSpace($value)) {
        $missingNames.Add($name)
        continue
    }

    Set-Item -Path "Env:$name" -Value $value
}

if ($missingNames.Count -gt 0) {
    throw "Missing persisted test database environment variables: $($missingNames -join ', '). Run .\scripts\setup-test-env.ps1 first."
}

Write-Host "test-with-test-db: loaded BLOG_DB_TEST_* into the current process." -ForegroundColor Green
Write-Host "test-with-test-db: running ./mvnw.cmd test from $repoRoot" -ForegroundColor DarkGray

& .\mvnw.cmd test
$exitCode = $LASTEXITCODE
if ($exitCode -ne 0) {
    exit $exitCode
}
