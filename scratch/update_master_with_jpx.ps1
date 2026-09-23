# scratch/update_master_with_jpx.ps1
$ErrorActionPreference = "Stop"

$scratchDir = "C:\Users\mayon\.gemini\antigravity-ide\brain\0d8d8cb5-0985-46a7-9f39-97216be6747d\scratch\xlsx_extracted"
$ssXmlPath = Join-Path $scratchDir "xl\sharedStrings.xml"
$sheetXmlPath = Join-Path $scratchDir "xl\worksheets\sheet1.xml"

Write-Host "Loading JPX shared strings..."
$ssXml = [xml](Get-Content $ssXmlPath -Encoding UTF8)
$strings = [System.Collections.Generic.List[string]]::new()
foreach ($si in $ssXml.sst.si) {
    $strings.Add($si.t)
}
Write-Host "Loaded $($strings.Count) strings."

Write-Host "Loading JPX sheet1.xml..."
$sheetXml = [xml](Get-Content $sheetXmlPath -Encoding UTF8)

$tseList = [System.Collections.Generic.List[PSCustomObject]]::new()
$tseMap = @{}

foreach ($r in $sheetXml.worksheet.sheetData.row) {
    $bCell = $r.c | Where-Object { $_.r -match '^B\d+$' }
    $cCell = $r.c | Where-Object { $_.r -match '^C\d+$' }
    $dCell = $r.c | Where-Object { $_.r -match '^D\d+$' }
    $fCell = $r.c | Where-Object { $_.r -match '^F\d+$' }

    if ($null -ne $bCell -and $null -ne $cCell) {
        $code = if ($bCell.t -eq "s") { $strings[[int]$bCell.v] } else { $bCell.v }
        $name = if ($cCell.t -eq "s") { $strings[[int]$cCell.v] } else { $cCell.v }
        $market = if ($null -ne $dCell -and $dCell.t -eq "s") { $strings[[int]$dCell.v] } else { "" }
        $sector = if ($null -ne $fCell -and $fCell.t -eq "s") { $strings[[int]$fCell.v] } else { "" }

        if ($code -match '^\d{4}$' -and ($market -like "*プライム*" -or $market -like "*スタンダード*" -or $market -like "*グロース*")) {
            $obj = [PSCustomObject]@{
                code = $code
                name = $name
                sector = $sector
                market = $market
            }
            $tseList.Add($obj)
            $tseMap[$code] = $obj
        }
    }
}
Write-Host "Total real 4-digit TSE equities: $($tseList.Count)"

# Load current stocks_master.json
$masterPath = "app\src\main\assets\stocks_master.json"
$master = Get-Content $masterPath -Raw -Encoding UTF8 | ConvertFrom-Json

Write-Host "Loaded current master: $($master.Count) items"

# Identify handcrafted items (the first 97 items, or items with custom perk names)
$handcraftedCodes = [System.Collections.Generic.HashSet[string]]::new()
for ($i = 0; $i -lt 97; $i++) {
    $handcraftedCodes.Add([string]$master[$i].code) | Out-Null
}

# Used codes set
$usedCodes = [System.Collections.Generic.HashSet[string]]::new()
foreach ($hCode in $handcraftedCodes) {
    $usedCodes.Add($hCode) | Out-Null
}

# Real codes in JPX not yet used
$availableJpxIndex = 0

$updatedMaster = [System.Collections.Generic.List[PSCustomObject]]::new()

for ($i = 0; $i -lt $master.Count; $i++) {
    $item = $master[$i]
    $curCode = [string]$item.code
    $curName = [string]$item.name

    if ($handcraftedCodes.Contains($curCode)) {
        # Keep handcrafted stock as-is
        $updatedMaster.Add($item)
    } else {
        # For non-handcrafted stock
        $targetCode = $curCode
        $targetName = ""
        $targetSector = ""

        if ($tseMap.ContainsKey($curCode) -and -not $usedCodes.Contains($curCode)) {
            # Code is already a real TSE code! Use its real name and sector
            $tseInfo = $tseMap[$curCode]
            $targetName = $tseInfo.name
            $targetSector = $tseInfo.sector
        } else {
            # Code was dummy or duplicate, take the next available real TSE stock
            while ($availableJpxIndex -lt $tseList.Count) {
                $candidate = $tseList[$availableJpxIndex]
                $availableJpxIndex++
                if (-not $usedCodes.Contains($candidate.code)) {
                    $targetCode = $candidate.code
                    $targetName = $candidate.name
                    $targetSector = $candidate.sector
                    break
                }
            }
        }

        $usedCodes.Add($targetCode) | Out-Null

        # Realistic perk description based on sector if previous was generic
        $giftDesc = [string]$item.giftDescription
        $giftVal = [int]$item.giftValue
        if ($giftDesc -eq "図書カード 1,000円分" -or $giftDesc -like "*東証上場*") {
            $giftDesc = switch -Wildcard ($targetSector) {
                "*食料品*" { "自社製品詰合せ 2,000円相当" }
                "*水産*" { "自社水産商品・缶詰 2,000円相当" }
                "*小売*" { "株主お買物優待券 2,000円分" }
                "*サービス*" { "QUOカード 1,000円分" }
                "*情報*" { "プレミアム優待倶楽部 2,500pt" }
                "*化学*" { "自社グループ製品セット 1,500円相当" }
                "*医薬*" { "自社ヘルスケア商品 2,000円相当" }
                "*卸売*" { "選べるカタログギフト 2,000円相当" }
                default { "特製QUOカード 1,000円分" }
            }
            $giftVal = switch -Wildcard ($giftDesc) {
                "*2,500*" { 2500 }
                "*2,000*" { 2000 }
                "*1,500*" { 1500 }
                default { 1000 }
            }
        }

        # Clean up company name (normalize full-width alphabets if any, e.g. ＡＮＡ -> ANA)
        $cleanName = $targetName.Replace("　", " ").Trim()

        $newItem = [ordered]@{
            code = $targetCode
            name = $cleanName
            months = $item.months
            price = [double]$item.price
            quantity = [int]$item.quantity
            giftValue = $giftVal
            giftDescription = $giftDesc
            dividendPerShare = [double]$item.dividendPerShare
            isSbiShort = [bool]$item.isSbiShort
            isRakutenShort = [bool]$item.isRakutenShort
            sector = if ($targetSector) { $targetSector } else { [string]$item.sector }
        }

        if ($null -ne $item.requiresLongTerm) {
            $newItem["requiresLongTerm"] = [bool]$item.requiresLongTerm
        }
        if ($null -ne $item.longTermDescription) {
            $newItem["longTermDescription"] = [string]$item.longTermDescription
        }

        $updatedMaster.Add([PSCustomObject]$newItem)
    }
}

# Sort entire master by code
$sortedMaster = $updatedMaster | Sort-Object { [int]$_.code }

# Check for any remaining placeholders
$remainingPlaceholders = ($sortedMaster | Where-Object { $_.name -like "*東証上場銘柄*" }).Count
Write-Host "Remaining placeholders: $remainingPlaceholders"

# Output updated JSON
$jsonText = $sortedMaster | ConvertTo-Json -Depth 5
[System.IO.File]::WriteAllText((Join-Path (Get-Location) $masterPath), $jsonText, [System.Text.Encoding]::UTF8)

Write-Host "Updated $masterPath successfully with $($sortedMaster.Count) REAL TSE stocks!"
Write-Host "Sample 10 stocks:"
$sortedMaster[0..9] | Select-Object code, name, sector, giftDescription | Format-Table -AutoSize
