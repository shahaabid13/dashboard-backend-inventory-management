[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.SecurityProtocolType]::Tls12
[System.Net.ServicePointManager]::ServerCertificateValidationCallback = { $true }

$loginUrl = "https://172.30.0.52:7443/REST/user/login"
$loginBody = '{"userid":"bel","password":"e8be9807c3ec0bd1ce2a58e9cddd4a89966820aedc509830b62f05a226dd29a3764e4962acf28b4b87377b347b9395e1dd5bc87fed1e44f17315b89d7501836c"}'

$loginResp = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $loginBody -ContentType "application/json"
$vsessionid = $loginResp.result[0].vsessionid
Write-Output "Obtained session ID: $vsessionid"

$headers = @{
    "Cookie" = "VSESSIONID=$vsessionid"
    "Accept" = "application/json"
}

$searchBody = '{"starttimestamp":1672531200000,"endtimestamp":1790000000000,"page":1,"limit":2}'

Write-Output "GETEVENTS 100:"
$s100 = Invoke-RestMethod -Uri "https://172.30.0.52:7443/REST/100/event/getevents" -Method Post -Body $searchBody -ContentType "application/json" -Headers $headers
$s100 | ConvertTo-Json -Depth 5

Write-Output "COUNT 100:"
$c100 = Invoke-RestMethod -Uri "https://172.30.0.52:7443/REST/100/event/count" -Method Post -Body $searchBody -ContentType "application/json" -Headers $headers
$c100 | ConvertTo-Json -Depth 5
