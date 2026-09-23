$path = "app/src/main/assets/stocks_master.json"
$raw = [System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8)
$parsed = ConvertFrom-Json $raw
$stocks = if ($null -ne $parsed.value) { $parsed.value } else { $parsed }

Write-Host "Found stocks count: $($stocks.Count)"

$jsonOut = ConvertTo-Json -InputObject @($stocks) -Depth 10
[System.IO.File]::WriteAllText($path, $jsonOut, [System.Text.Encoding]::UTF8)

$check = [System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8).Trim()
Write-Host "Starts with: $($check.Substring(0, 10))"
Write-Host "Ends with: $($check.Substring($check.Length - 10))"
