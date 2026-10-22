# Run after starting PostgreSQL and RoastPointApplication.
$ErrorActionPreference='Stop'
$api='http://localhost:8080/api/orders'
$body='{"cashierId":1,"items":[{"menuItemId":3,"quantity":2},{"menuItemId":8,"quantity":1}]}'
$created=Invoke-RestMethod -Method Post -Uri $api -ContentType 'application/json' -Body $body
if ($created.total -ne 560) { throw "Expected 560, got $($created.total)" }
$read=Invoke-RestMethod -Uri "$api/$($created.id)"
if ($read.id -ne $created.id -or $read.total -ne 560 -or $read.items.Count -ne 2) { throw 'GET returned unexpected order' }
Write-Host "PASS: saved and read order $($created.id), total 560"
try {
 Invoke-RestMethod -Method Post -Uri $api -ContentType 'application/json' -Body '{"cashierId":1,"items":[]}' | Out-Null
 throw 'Expected HTTP 400'
} catch {
 if ($_.Exception.Response.StatusCode.value__ -ne 400) { throw }
 Write-Host 'PASS: empty order returns HTTP 400'
}
try {
 Invoke-RestMethod -Uri "$api/2147483647" | Out-Null
 throw 'Expected HTTP 404'
} catch {
 if ($_.Exception.Response.StatusCode.value__ -ne 404) { throw }
 Write-Host 'PASS: missing order returns HTTP 404'
}
