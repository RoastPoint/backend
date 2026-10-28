param([string]$BaseUrl='http://localhost:8080',[string]$Password=$env:DEMO_PASSWORD)
$ErrorActionPreference='Stop'
if (!$Password) { throw 'Set DEMO_PASSWORD to the initial demo password.' }
function Login([string]$LoginName) {
 $session=New-Object Microsoft.PowerShell.Commands.WebRequestSession
 $csrf=Invoke-RestMethod "$BaseUrl/api/auth/csrf" -WebSession $session
 $headers=@{}; $headers[$csrf.headerName]=$csrf.token
 Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/login" -WebSession $session -Headers $headers -Body @{login=$LoginName;password=$Password} | Out-Null
 $csrf=Invoke-RestMethod "$BaseUrl/api/auth/csrf" -WebSession $session
 $headers=@{}; $headers[$csrf.headerName]=$csrf.token
 return @{session=$session;headers=$headers}
}
function Request($Client,[string]$Method,[string]$Path,[string]$Body) {
 $params=@{Method=$Method;Uri="$BaseUrl$Path";WebSession=$Client.session;Headers=$Client.headers}
 if($Body) { $params.ContentType='application/json'; $params.Body=$Body }
 Invoke-RestMethod @params
}
function ExpectError($Client,[string]$Method,[string]$Path,[string]$Body,[int]$Status) {
 try { Request $Client $Method $Path $Body | Out-Null } catch {
  if([int]$_.Exception.Response.StatusCode -eq $Status) { Write-Host "PASS: $Method $Path returns $Status"; return }
  throw
 }
 throw "Expected HTTP $Status from $Method $Path"
}
$cashier=Login 'cashier1'; $barista=Login 'barista1'; $admin=Login 'admin1'
$menu=Request $cashier Get '/api/menu' ''
$item=$menu | Where-Object available | Select-Object -First 1
if(!$item) { throw 'No available menu items' }
$body=@{items=@(@{menuItemId=$item.id;quantity=2})} | ConvertTo-Json -Depth 5
$order=Request $cashier Post '/api/orders' $body
if($order.total -ne 2*$item.price) { throw 'Incorrect order total' }
ExpectError $barista Post '/api/orders' $body 403
ExpectError $cashier Patch "/api/orders/$($order.id)/status" '{"status":"preparing"}' 403
ExpectError $cashier Patch "/api/orders/$($order.id)/status" '{"status":"issued"}' 409
foreach($status in @('preparing','ready')) {
 $changed=Request $barista Patch "/api/orders/$($order.id)/status" (@{status=$status}|ConvertTo-Json)
 if($changed.status -ne $status) { throw 'Incorrect status' }
}
$issued=Request $cashier Patch "/api/orders/$($order.id)/status" '{"status":"issued"}'
if($issued.status -ne 'issued') { throw 'Order was not issued' }
ExpectError $cashier Patch "/api/orders/$($order.id)/status" '{"status":"issued"}' 409
ExpectError $cashier Post '/api/orders' '{"items":[]}' 400
ExpectError $cashier Post '/api/orders' '{"cashierId":2,"items":[{"menuItemId":1,"quantity":1}]}' 400
ExpectError $cashier Get '/api/reports/sales?from=2026-01-01&to=2026-12-31' '' 403
$today=Get-Date -Format 'yyyy-MM-dd'
Request $admin Get "/api/reports/sales?from=$today&to=$today" '' | ConvertTo-Json
Request $cashier Post '/api/auth/logout' '' | Out-Null
ExpectError $cashier Get '/api/auth/me' '' 401
Write-Host "PASS: login, roles, validation, full lifecycle and logout; order $($order.id), total $($order.total)"
