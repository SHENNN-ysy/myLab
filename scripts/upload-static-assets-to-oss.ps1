param(
    [Parameter(Mandatory = $true)]
    [string]$Bucket,
    [string]$Prefix = "static",
    [string]$Ossutil = "ossutil"
)

$ErrorActionPreference = "Stop"
$source = (Resolve-Path (Join-Path $PSScriptRoot "..\myblog\public")).Path
$target = "oss://$Bucket/$Prefix/"

Write-Host "正在同步静态图片：$source -> $target"
& $Ossutil cp $source $target --recursive --update
if ($LASTEXITCODE -ne 0) {
    throw "ossutil 同步失败，退出码：$LASTEXITCODE"
}

Write-Host "静态图片同步完成。请确认 STATIC_CDN_BASE 指向 CDN 的 /$Prefix 目录。"
