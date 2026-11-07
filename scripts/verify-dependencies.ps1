#requires -Version 7.4
[CmdletBinding()]
param(
    [string]$MetadataPath = (Join-Path $PSScriptRoot '../gradle/verification-metadata.xml'),
    [string]$ReportPath = (Join-Path $PSScriptRoot '../build/reports/dependency-verification/checksums.json'),
    [ValidateRange(1, 12)]
    [int]$ThrottleLimit = 6,
    [switch]$UpdateOrigins
)

$ErrorActionPreference = 'Stop'
$metadataFile = (Resolve-Path -LiteralPath $MetadataPath).Path
$reportFile = [System.IO.Path]::GetFullPath($ReportPath)
if ([string]::Equals($metadataFile, $reportFile, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'The review report must not overwrite dependency verification metadata.'
}
$readerSettings = [System.Xml.XmlReaderSettings]::new()
$readerSettings.DtdProcessing = [System.Xml.DtdProcessing]::Prohibit
$readerSettings.XmlResolver = $null
$reader = [System.Xml.XmlReader]::Create($metadataFile, $readerSettings)
$metadata = [System.Xml.XmlDocument]::new()
$metadata.XmlResolver = $null
try {
    $metadata.Load($reader)
} finally {
    $reader.Dispose()
}

$root = $metadata.'verification-metadata'
if ($root.configuration.'verify-metadata' -ne 'true') {
    throw 'Dependency metadata verification must remain enabled.'
}
if ($metadata.SelectNodes('//*[local-name()="trusted-artifacts"]/*').Count -gt 0) {
    throw 'Trusted-artifact exceptions require a separate security review.'
}

$artifacts = @(
    foreach ($component in $root.components.component) {
        foreach ($coordinate in @($component.group, $component.name, $component.version)) {
            if ($coordinate -notmatch '^[A-Za-z0-9_.+\-]+$') {
                throw 'Dependency coordinates contain an unsupported path character.'
            }
        }
        foreach ($artifact in $component.artifact) {
            $checksums = @($artifact.sha256)
            if ($artifact.name -notmatch '^[A-Za-z0-9_.+\-]+$' -or
                $checksums.Count -ne 1 -or $checksums[0].value -notmatch '^[a-fA-F0-9]{64}$') {
                throw "Expected one SHA-256 value for $($component.group):$($component.name):$($artifact.name)."
            }
            [pscustomobject]@{
                Group = [string]$component.group
                Name = [string]$component.name
                Version = [string]$component.version
                FileName = [string]$artifact.name
                Sha256 = $checksums[0].value.ToLowerInvariant()
            }
        }
    }
)
if ($artifacts.Count -eq 0) {
    throw 'The dependency baseline is empty.'
}

# Retrieve public checksums independently of the local Gradle artifact cache.
# Older publications fall back to hashing a fresh HTTPS artifact, never the local cache.
# Publisher signatures are not verified here.
$client = [System.Net.Http.HttpClient]::new()
$client.Timeout = [TimeSpan]::FromSeconds(20)
try {
    $results = @($artifacts | ForEach-Object -Parallel {
        $item = $_
        $http = $using:client
        $result = [ordered]@{
            Group = $item.Group; Name = $item.Name; Version = $item.Version
            FileName = $item.FileName; Sha256 = $item.Sha256
            Verified = $false; Source = $null; Verification = $null; Error = $null
        }
        try {
            $path = $item.Group.Replace('.', '/') + '/' + $item.Name + '/' +
                [uri]::EscapeDataString($item.Version) + '/' + [uri]::EscapeDataString($item.FileName)
            $repositories = @('https://repo.maven.apache.org/maven2/', 'https://plugins.gradle.org/m2/')
            $isPluginMarker = $item.Name -eq "$($item.Group).gradle.plugin"
            if ($isPluginMarker) {
                # Plugin marker POMs are resolved from the Plugin Portal by Gradle's plugins DSL.
                $repositories = @('https://plugins.gradle.org/m2/', 'https://repo.maven.apache.org/maven2/')
            }
            foreach ($repository in $repositories) {
                $artifactUrl = $repository + $path
                $checksumUrl = $artifactUrl + '.sha256'
                # A marker checksum URL can redirect to a different Central POM variant.
                # Hash the actual Portal marker instead of trusting that redirected checksum.
                $needsDownload = $isPluginMarker
                if (-not $isPluginMarker) {
                    $response = $http.GetAsync($checksumUrl).GetAwaiter().GetResult()
                    try {
                        $needsDownload = $response.StatusCode -eq [System.Net.HttpStatusCode]::NotFound
                        if (-not $needsDownload) {
                            $response.EnsureSuccessStatusCode() | Out-Null
                            $published = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult().Trim()
                            if ($published -notmatch '^(?<hash>[a-fA-F0-9]{64})(?:\s|$)') {
                                throw 'Repository did not return a SHA-256 checksum.'
                            }
                            $digest = $Matches.hash.ToLowerInvariant()
                            $source = $checksumUrl
                            $method = 'Published SHA-256'
                        }
                    } finally {
                        $response.Dispose()
                    }
                }
                if ($needsDownload) {
                    $deadline = [System.Threading.CancellationTokenSource]::new([TimeSpan]::FromSeconds(20))
                    try {
                        $download = $http.GetAsync($artifactUrl,
                            [System.Net.Http.HttpCompletionOption]::ResponseHeadersRead,
                            $deadline.Token).GetAwaiter().GetResult()
                        try {
                            if ($download.StatusCode -eq [System.Net.HttpStatusCode]::NotFound) {
                                continue
                            }
                            $download.EnsureSuccessStatusCode() | Out-Null
                            $stream = $download.Content.ReadAsStreamAsync($deadline.Token).GetAwaiter().GetResult()
                            try {
                                $hash = [System.Security.Cryptography.SHA256]::HashDataAsync(
                                    $stream, $deadline.Token).GetAwaiter().GetResult()
                                $digest = [Convert]::ToHexString($hash).ToLowerInvariant()
                            } finally {
                                $stream.Dispose()
                            }
                        } finally {
                            $download.Dispose()
                        }
                    } finally {
                        $deadline.Dispose()
                    }
                    $source = $artifactUrl
                    $method = 'Fresh HTTPS artifact SHA-256'
                }
                if ($digest -ne $item.Sha256) {
                    throw "Checksum mismatch against $source"
                }
                $result.Verified = $true
                $result.Source = $source
                $result.Verification = $method
                break
            }
            if (-not $result.Verified) {
                throw 'Artifact not found in the approved repositories.'
            }
        } catch {
            $result.Error = $_.Exception.Message
        }
        [pscustomobject]$result
    } -ThrottleLimit $ThrottleLimit)
} finally {
    $client.Dispose()
}

$results = @($results | Sort-Object Group, Name, Version, FileName)
[System.IO.Directory]::CreateDirectory([System.IO.Path]::GetDirectoryName($reportFile)) | Out-Null
$results | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $reportFile -Encoding utf8
$failed = @($results | Where-Object { -not $_.Verified })
Write-Host "Verified $($results.Count - $failed.Count) of $($results.Count) artifact checksums."
if ($failed.Count -gt 0) {
    $failed | Select-Object -First 10 Group, Name, Version, FileName, Error | Format-Table -Wrap | Out-Host
    throw "Dependency baseline review failed. See $reportFile. No checksum or origin was changed."
}

if ($UpdateOrigins) {
    $sources = @{}
    foreach ($result in $results) {
        $sources["$($result.Group):$($result.Name):$($result.Version):$($result.FileName)"] = $result.Source
    }
    foreach ($component in $root.components.component) {
        foreach ($artifact in $component.artifact) {
            $source = $sources["$($component.group):$($component.name):$($component.version):$($artifact.name)"]
            $artifact.sha256.SetAttribute('origin', "Verified $([DateTime]::UtcNow.ToString('yyyy-MM-dd')) against $source")
        }
    }
    $metadata.Save($metadataFile)
    Write-Host 'Recorded verified checksum sources; expected checksum values are unchanged.'
}
