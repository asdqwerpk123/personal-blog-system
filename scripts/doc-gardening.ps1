[CmdletBinding()]
param(
    [string[]]$Files = @(),
    [string[]]$Paths = @("docs", "README.md", "AGENTS.md")
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "harness-utils.ps1")

$repoRoot = Get-HarnessRepoRoot
Set-Location $repoRoot

function ConvertTo-MarkdownAnchor {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Heading
    )

    $anchor = $Heading.Trim().ToLowerInvariant()
    $anchor = $anchor -replace "<[^>]+>", ""
    $anchor = $anchor -replace "[^\p{L}\p{Nd}\- _]+", ""
    $anchor = $anchor -replace "\s+", "-"
    $anchor = $anchor -replace "-{2,}", "-"
    return $anchor.Trim("-")
}

function Get-MarkdownAnchors {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    $content = [System.IO.File]::ReadAllText($Path)
    $matches = [regex]::Matches($content, "(?m)^\s{0,3}#{1,6}\s+(?<heading>.+?)\s*$")
    $anchors = [System.Collections.Generic.HashSet[string]]::new()
    $seen = @{}

    foreach ($match in $matches) {
        $heading = $match.Groups["heading"].Value.Trim()
        $baseAnchor = ConvertTo-MarkdownAnchor -Heading $heading
        if ([string]::IsNullOrWhiteSpace($baseAnchor)) {
            continue
        }

        if ($seen.ContainsKey($baseAnchor)) {
            $seen[$baseAnchor] += 1
            $anchor = "$baseAnchor-$($seen[$baseAnchor])"
        }
        else {
            $seen[$baseAnchor] = 0
            $anchor = $baseAnchor
        }

        [void]$anchors.Add($anchor)
    }

    return $anchors
}

function Get-MarkdownFilesToCheck {
    if ($Files.Count -gt 0) {
        return @(
            $Files |
                ForEach-Object { Join-Path $repoRoot $_ } |
                Where-Object { (Test-Path $_ -PathType Leaf) -and ([System.IO.Path]::GetExtension($_).ToLowerInvariant() -eq ".md") } |
                Sort-Object -Unique
        )
    }

    $collected = [System.Collections.Generic.List[string]]::new()
    foreach ($pathItem in $Paths) {
        $absolutePath = Join-Path $repoRoot $pathItem
        if (-not (Test-Path $absolutePath)) {
            continue
        }

        if (Test-Path $absolutePath -PathType Leaf) {
            if ([System.IO.Path]::GetExtension($absolutePath).ToLowerInvariant() -eq ".md") {
                $collected.Add($absolutePath)
            }
            continue
        }

        $markdownFiles = Get-ChildItem -Path $absolutePath -Filter *.md -Recurse -File | Select-Object -ExpandProperty FullName
        foreach ($markdownFile in $markdownFiles) {
            $collected.Add($markdownFile)
        }
    }

    return @($collected | Sort-Object -Unique)
}

function Get-LinkTarget {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RawTarget
    )

    $target = $RawTarget.Trim()
    if ($target.StartsWith("<") -and $target.EndsWith(">")) {
        $target = $target.Substring(1, $target.Length - 2)
    }

    if ($target -match '^(?<path>\S+)\s+"[^"]*"$') {
        return $matches["path"]
    }

    return $target
}

$markdownFiles = Get-MarkdownFilesToCheck
$errors = [System.Collections.Generic.List[string]]::new()
$anchorCache = @{}
$linkPattern = [regex]'!\[[^\]]*\]\((?<target>[^)]+)\)|\[[^\]]+\]\((?<target>[^)]+)\)'

foreach ($markdownFile in $markdownFiles) {
    $content = [System.IO.File]::ReadAllText($markdownFile)
    $matches = $linkPattern.Matches($content)

    foreach ($match in $matches) {
        $rawTarget = Get-LinkTarget -RawTarget $match.Groups["target"].Value
        if ([string]::IsNullOrWhiteSpace($rawTarget)) {
            continue
        }

        if ($rawTarget -match '^[a-zA-Z][a-zA-Z0-9+.-]*:') {
            continue
        }

        $targetPath = $rawTarget
        $fragment = ""

        if ($rawTarget.StartsWith("#")) {
            $targetPath = ""
            $fragment = $rawTarget.Substring(1)
        }
        elseif ($rawTarget.Contains("#")) {
            $parts = $rawTarget.Split("#", 2)
            $targetPath = $parts[0]
            $fragment = $parts[1]
        }

        $resolvedPath = if ([string]::IsNullOrWhiteSpace($targetPath)) {
            $markdownFile
        }
        else {
            [System.IO.Path]::GetFullPath((Join-Path (Split-Path $markdownFile -Parent) $targetPath))
        }

        if (-not (Test-Path $resolvedPath)) {
            $errors.Add("Broken link in $markdownFile -> $rawTarget")
            continue
        }

        if (-not [string]::IsNullOrWhiteSpace($fragment) -and [System.IO.Path]::GetExtension($resolvedPath).ToLowerInvariant() -eq ".md") {
            if (-not $anchorCache.ContainsKey($resolvedPath)) {
                $anchorCache[$resolvedPath] = Get-MarkdownAnchors -Path $resolvedPath
            }

            $expectedAnchor = ConvertTo-MarkdownAnchor -Heading $fragment
            if ([string]::IsNullOrWhiteSpace($expectedAnchor) -or -not $anchorCache[$resolvedPath].Contains($expectedAnchor)) {
                $errors.Add("Broken markdown anchor in $markdownFile -> $rawTarget")
            }
        }
    }
}

if ($errors.Count -gt 0) {
    $logPath = Write-HarnessFailureLog -Check "doc-gardening" -Message "Markdown link validation failed." -Files $markdownFiles -Metadata @{
        errors = @($errors)
    }

    Write-Host "doc-gardening: FAIL" -ForegroundColor Red
    foreach ($errorLine in $errors) {
        Write-Host " - $errorLine" -ForegroundColor Red
    }
    Write-Host "failure log: $logPath" -ForegroundColor Yellow
    exit 1
}

Write-Host "doc-gardening: OK ($($markdownFiles.Count) files)" -ForegroundColor Green
