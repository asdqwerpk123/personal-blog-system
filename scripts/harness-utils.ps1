Set-StrictMode -Version Latest

function Get-HarnessRepoRoot {
    return [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot ".."))
}

function Get-StagedFiles {
    param(
        [switch]$IncludeDeleted
    )

    $repoRoot = Get-HarnessRepoRoot
    $diffFilter = if ($IncludeDeleted) { "ACMRD" } else { "ACMR" }
    $output = & git -C $repoRoot diff --cached --name-only "--diff-filter=$diffFilter"
    return @($output | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
}

function Test-HarnessManagedTextFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    $fileName = [System.IO.Path]::GetFileName($Path)
    if ($fileName -in @(".gitignore", ".gitattributes")) {
        return $true
    }

    $extension = [System.IO.Path]::GetExtension($Path).ToLowerInvariant()
    return $extension -in @(
        ".bat",
        ".cmd",
        ".gitattributes",
        ".gitignore",
        ".java",
        ".json",
        ".md",
        ".properties",
        ".ps1",
        ".sh",
        ".sql",
        ".txt",
        ".xml",
        ".yaml",
        ".yml"
    )
}

function Get-TextEncoding {
    param(
        [byte[]]$Bytes
    )

    if ($Bytes.Length -ge 3 -and $Bytes[0] -eq 0xEF -and $Bytes[1] -eq 0xBB -and $Bytes[2] -eq 0xBF) {
        return [System.Text.UTF8Encoding]::new($true)
    }

    if ($Bytes.Length -ge 2 -and $Bytes[0] -eq 0xFF -and $Bytes[1] -eq 0xFE) {
        return [System.Text.UnicodeEncoding]::new($false, $true)
    }

    if ($Bytes.Length -ge 2 -and $Bytes[0] -eq 0xFE -and $Bytes[1] -eq 0xFF) {
        return [System.Text.UnicodeEncoding]::new($true, $true)
    }

    return [System.Text.UTF8Encoding]::new($false)
}

function Get-TextFileInfo {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    $bytes = [System.IO.File]::ReadAllBytes($Path)
    $encoding = Get-TextEncoding -Bytes $bytes
    $preamble = $encoding.GetPreamble()

    if ($bytes.Length -eq 0) {
        $contentBytes = [byte[]]::new(0)
    }
    elseif ($preamble.Length -gt 0 -and $bytes.Length -ge $preamble.Length) {
        $contentBytes = $bytes[$preamble.Length..($bytes.Length - 1)]
    }
    else {
        $contentBytes = $bytes
    }

    $text = $encoding.GetString($contentBytes)
    $newline = if ($text.Contains("`r`n")) { "`r`n" } else { "`n" }

    return @{
        Encoding = $encoding
        Newline = $newline
        Text = $text
    }
}

function Set-TextFileContent {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [Parameter(Mandatory = $true)]
        [string]$Text,
        [Parameter(Mandatory = $true)]
        [System.Text.Encoding]$Encoding
    )

    [System.IO.File]::WriteAllText($Path, $Text, $Encoding)
}

function Repair-TextFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if (-not (Test-Path $Path -PathType Leaf)) {
        return $false
    }

    $info = Get-TextFileInfo -Path $Path
    $lines = [regex]::Split($info.Text, "\r?\n")
    $trimmedLines = foreach ($line in $lines) {
        $line -replace "[ \t]+$", ""
    }

    $normalized = $trimmedLines -join $info.Newline
    $normalized = $normalized -replace "(\r?\n)+\z", ""

    if ($normalized.Length -gt 0) {
        $normalized += $info.Newline
    }

    if ($normalized -ne $info.Text) {
        Set-TextFileContent -Path $Path -Text $normalized -Encoding $info.Encoding
        return $true
    }

    return $false
}

function Write-HarnessFailureLog {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Check,
        [Parameter(Mandatory = $true)]
        [string]$Message,
        [string[]]$Files = @(),
        [hashtable]$Metadata = @{}
    )

    $repoRoot = Get-HarnessRepoRoot
    $failureDir = Join-Path $repoRoot "failure_logs"

    if (-not (Test-Path $failureDir -PathType Container)) {
        New-Item -ItemType Directory -Path $failureDir | Out-Null
    }

    $branch = (& git -C $repoRoot branch --show-current 2>$null)
    if ([string]::IsNullOrWhiteSpace($branch)) {
        $branch = "detached"
    }

    $safeCheck = ($Check -replace "[^A-Za-z0-9_-]+", "-").Trim("-")
    if ([string]::IsNullOrWhiteSpace($safeCheck)) {
        $safeCheck = "failure"
    }

    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $logPath = Join-Path $failureDir "$timestamp-$safeCheck.json"
    $payload = [ordered]@{
        branch = $branch.Trim()
        check = $Check
        files = @($Files)
        message = $Message
        metadata = $Metadata
        timestamp = (Get-Date).ToString("s")
    }

    $json = $payload | ConvertTo-Json -Depth 8
    [System.IO.File]::WriteAllText(
        $logPath,
        $json + [Environment]::NewLine,
        [System.Text.UTF8Encoding]::new($false)
    )

    return $logPath
}
