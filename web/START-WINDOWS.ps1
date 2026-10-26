$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot 'site')
Write-Host 'Opening http://localhost:8080/' -ForegroundColor Green
Start-Process 'http://localhost:8080/'
py -m http.server 8080
