param(
    [string]$Registry = "localhost:5000",
    [switch]$ForceRefresh
)

$ErrorActionPreference = "Stop"
$originalLocation = Get-Location
Set-Location -LiteralPath $PSScriptRoot

$images = @(
    "postgres:16-alpine",
    "redis:7-alpine",
    "rabbitmq:3.13-management-alpine",
    "minio/minio:latest",
    "node:20-alpine",
    "nginx:1.27-alpine",
    "python:3.12-slim",
    "maven:3.9-eclipse-temurin-21",
    "eclipse-temurin:21-jre"
)

function Invoke-Docker {
    param([Parameter(Mandatory)][string[]]$Arguments)

    & docker @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Docker command failed: docker $($Arguments -join ' ')"
    }
}

function Test-RegistryTag {
    param([Parameter(Mandatory)][string]$Image)

    $lastColon = $Image.LastIndexOf(":")
    $repository = $Image.Substring(0, $lastColon)
    $tag = $Image.Substring($lastColon + 1)

    try {
        Invoke-WebRequest `
            -Uri "http://$Registry/v2/$repository/manifests/$tag" `
            -Method Head `
            -Headers @{ Accept = "application/vnd.oci.image.manifest.v1+json, application/vnd.oci.image.index.v1+json, application/vnd.docker.distribution.manifest.v2+json" } `
            -TimeoutSec 5 `
            -UseBasicParsing | Out-Null
        return $true
    } catch {
        return $false
    }
}

try {
    Invoke-RestMethod -Uri "http://$Registry/v2/" -TimeoutSec 5 | Out-Null
} catch {
    throw "Cannot reach http://$Registry/v2/. Start the local-registry container first."
}

try {
    foreach ($image in $images) {
        if (-not $ForceRefresh -and (Test-RegistryTag -Image $image)) {
            Write-Host "Already present, skipping: $Registry/$image" -ForegroundColor DarkGray
            continue
        }

        Write-Host "Syncing base image: $image" -ForegroundColor Cyan
        Invoke-Docker -Arguments @("pull", $image)
        Invoke-Docker -Arguments @("tag", $image, "$Registry/$image")
        Invoke-Docker -Arguments @("push", "$Registry/$image")
    }

    Write-Host "Building project images..." -ForegroundColor Cyan
    Invoke-Docker -Arguments @("compose", "build")

    $projectImages = @("myblog-api:latest", "myblog-web:latest", "myblog-admin:latest")
    foreach ($image in $projectImages) {
        Write-Host "Pushing project image: $Registry/$image" -ForegroundColor Cyan
        Invoke-Docker -Arguments @("push", "$Registry/$image")
    }

    Write-Host "Local registry sync completed." -ForegroundColor Green
} finally {
    Set-Location -LiteralPath $originalLocation
}
