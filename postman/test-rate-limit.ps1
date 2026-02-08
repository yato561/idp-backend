# Rate Limit Testing Script (PowerShell for Windows)
# Usage: powershell -ExecutionPolicy Bypass -File "test-rate-limit.ps1" -Endpoint service -Requests 100 -DelayMs 0

param(
    [ValidateSet('service', 'admin')]
    [string]$Endpoint = 'service',
    
    [int]$Requests = 100,
    
    [int]$DelayMs = 0
)

$BaseUrl = "http://localhost:8080"

# Colors
$Green = "`e[32m"
$Red = "`e[31m"
$Yellow = "`e[33m"
$Reset = "`e[0m"

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Rate Limit Testing Script (PowerShell)" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Endpoint: $Endpoint"
Write-Host "Requests: $Requests"
Write-Host "Delay: ${DelayMs}ms"
Write-Host ""

$allowed = 0
$limited = 0
$errors = 0
$startTime = Get-Date

if ($Endpoint -eq 'admin') {
    Write-Host "Testing ADMIN endpoints (deployments)..." -ForegroundColor Yellow
    Write-Host ""
    
    $AdminToken = "your_admin_jwt_token_here"
    $Headers = @{
        "Authorization" = "Bearer $AdminToken"
        "Content-Type" = "application/json"
    }
    
    $Body = @{
        serviceName = "test"
        version = "1.0.0"
    } | ConvertTo-Json
    
    for ($i = 1; $i -le $Requests; $i++) {
        try {
            $Response = Invoke-WebRequest -Uri "$BaseUrl/api/deployments" `
                -Method POST `
                -Headers $Headers `
                -Body $Body `
                -ErrorAction SilentlyContinue `
                -SkipHttpErrorCheck
            
            $Status = $Response.StatusCode
            
            if ($Status -eq 429) {
                Write-Host "$i : RATE LIMITED (429)" -ForegroundColor Red
                $limited++
            } elseif ($Status -eq 200 -or $Status -eq 201) {
                Write-Host "$i : ALLOWED ($Status)" -ForegroundColor Green
                $allowed++
            } else {
                Write-Host "$i : ERROR ($Status)" -ForegroundColor Yellow
                $errors++
            }
        } catch {
            Write-Host "$i : ERROR (Connection failed)" -ForegroundColor Yellow
            $errors++
        }
        
        if ($DelayMs -gt 0) {
            Start-Sleep -Milliseconds $DelayMs
        }
    }
    
} elseif ($Endpoint -eq 'service') {
    Write-Host "Testing SERVICE endpoints (metrics/ingest)..." -ForegroundColor Yellow
    Write-Host ""
    
    $ServiceId = "service-001"
    $Headers = @{
        "X-Service-Id" = $ServiceId
        "Content-Type" = "application/json"
    }
    
    $Body = @{
        metrics = @(
            @{
                name = "cpu"
                value = 45.5
                timestamp = (Get-Date).ToString("o")
            }
        )
    } | ConvertTo-Json
    
    for ($i = 1; $i -le $Requests; $i++) {
        try {
            $Response = Invoke-WebRequest -Uri "$BaseUrl/api/metrics/ingest" `
                -Method POST `
                -Headers $Headers `
                -Body $Body `
                -ErrorAction SilentlyContinue `
                -SkipHttpErrorCheck
            
            $Status = $Response.StatusCode
            
            if ($Status -eq 429) {
                Write-Host "$i : RATE LIMITED (429)" -ForegroundColor Red
                $limited++
            } elseif ($Status -eq 200 -or $Status -eq 201) {
                Write-Host "$i : ALLOWED ($Status)" -ForegroundColor Green
                $allowed++
            } else {
                Write-Host "$i : ERROR ($Status)" -ForegroundColor Yellow
                $errors++
            }
        } catch {
            Write-Host "$i : ERROR (Connection failed)" -ForegroundColor Yellow
            $errors++
        }
        
        if ($DelayMs -gt 0) {
            Start-Sleep -Milliseconds $DelayMs
        }
    }
}

$endTime = Get-Date
$totalTime = ($endTime - $startTime).TotalSeconds

Write-Host ""
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Results Summary" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Allowed      : $allowed" -ForegroundColor Green
Write-Host "Rate Limited : $limited" -ForegroundColor Red
Write-Host "Errors       : $errors" -ForegroundColor Yellow
Write-Host "Total        : $Requests"
Write-Host "Time Elapsed : $totalTime seconds"
Write-Host ""
if ($allowed -gt 0) {
    Write-Host "First rate limit hit at request: $($allowed + 1)" -ForegroundColor Yellow
}
