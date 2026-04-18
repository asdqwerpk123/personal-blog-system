[CmdletBinding()]
param(
    [string]$DbUrl = "jdbc:mysql://localhost:3306/personal_blog_system_test?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false",
    [string]$DbUsername = "root",
    [string]$DbPassword = "123456"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "harness-utils.ps1")

$repoRoot = Get-HarnessRepoRoot
Set-Location $repoRoot

$variables = [ordered]@{
    BLOG_DB_TEST_URL = $DbUrl
    BLOG_DB_TEST_USERNAME = $DbUsername
    BLOG_DB_TEST_PASSWORD = $DbPassword
}

foreach ($entry in $variables.GetEnumerator()) {
    [System.Environment]::SetEnvironmentVariable($entry.Key, $entry.Value, "User")
    Set-Item -Path "Env:$($entry.Key)" -Value $entry.Value
}

Write-Host "setup-test-env: configured BLOG_DB_TEST_* for User and Process scopes." -ForegroundColor Green
Write-Host " - BLOG_DB_TEST_URL=$DbUrl"
Write-Host " - BLOG_DB_TEST_USERNAME=$DbUsername"
Write-Host " - BLOG_DB_TEST_PASSWORD=(hidden)"
Write-Host "Next step: run .\scripts\test-with-test-db.ps1" -ForegroundColor DarkGray
