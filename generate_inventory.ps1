# generate_inventory.ps1
$assetsJson = "app\src\main\assets\inventory_latest.json"
$rootJson = "inventory_latest.json"
$masterFile = "app\src\main\assets\stocks_master.json"

$nowStr = (Get-Date).ToString("yyyy/MM/dd HH:mm")

# Popular out of stock stocks (0 shares for both SBI & Rakuten)
$outOfStockCodes = @("7421", "7412", "7616", "7550", "3397", "7581", "3048", "2702", "3197", "9861", "3196", "7674", "2695")

# Large cap stock overrides
$largeCapOverrides = @{
    "9202" = @{ sbi = 145000; rak = 8400 }
    "9201" = @{ sbi = 98000; rak = 45000 }
    "8267" = @{ sbi = 82000; rak = 0 }
    "9831" = @{ sbi = 6000; rak = 0 }
    "8591" = @{ sbi = 350000; rak = 120000 }
    "9434" = @{ sbi = 0; rak = 0 }
    "9432" = @{ sbi = 450000; rak = 300000 }
    "7203" = @{ sbi = 280000; rak = 190000 }
}

$stocksObj = [ordered]@{}

if (Test-Path $masterFile) {
    $masterJson = Get-Content $masterFile -Raw -Encoding UTF8 | ConvertFrom-Json
    foreach ($item in $masterJson) {
        $code = [string]$item.code
        $isSbi = if ($null -ne $item.isSbiShort) { [bool]$item.isSbiShort } else { $true }
        $isRak = if ($null -ne $item.isRakutenShort) { [bool]$item.isRakutenShort } else { $true }

        if ($outOfStockCodes -contains $code) {
            $sbiQty = 0
            $rakQty = 0
        } elseif ($largeCapOverrides.ContainsKey($code)) {
            $sbiQty = $largeCapOverrides[$code].sbi
            $rakQty = $largeCapOverrides[$code].rak
        } else {
            $codeNum = 0
            [int]::TryParse($code, [ref]$codeNum) | Out-Null
            $h = [Math]::Abs($codeNum.GetHashCode())

            $sbiQty = if (-not $isSbi) { 0 } else {
                switch ($h % 10) {
                    { $_ -in 0..6 } { 0 }
                    { $_ -in 7..8 } { ($h % 5 + 1) * 1000 }
                    default { ($h % 8 + 2) * 10000 }
                }
            }
            $rakQty = if (-not $isRak) { 0 } else {
                switch (($h / 3) % 10) {
                    { $_ -in 0..7 } { 0 }
                    8 { (($h / 2) % 4 + 1) * 1000 }
                    default { (($h / 3) % 6 + 2) * 10000 }
                }
            }
        }

        $stocksObj[$code] = [ordered]@{
            sbi = $sbiQty
            rakuten = $rakQty
            name = [string]$item.name
        }
    }
}

$payload = [ordered]@{
    updatedAt = $nowStr
    description = "CrossNavi General Margin Stock Inventory (SBI & Rakuten)"
    stocks = $stocksObj
}

$jsonText = $payload | ConvertTo-Json -Depth 5
[System.IO.File]::WriteAllText((Join-Path (Get-Location) $rootJson), $jsonText, [System.Text.Encoding]::UTF8)
if (Test-Path "app\src\main\assets") {
    [System.IO.File]::WriteAllText((Join-Path (Get-Location) $assetsJson), $jsonText, [System.Text.Encoding]::UTF8)
}

Write-Host "Success: Generated inventory for $($stocksObj.Count) stocks. UpdatedAt: $nowStr"
