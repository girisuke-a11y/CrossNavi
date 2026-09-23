$path = "app/src/main/assets/stocks_master.json"
$content = [System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8)
$stocks = ConvertFrom-Json $content

function Convert-FullWidthToHalfWidth([string]$inputStr) {
    if ([string]::IsNullOrEmpty($inputStr)) { return $inputStr }
    $sb = New-Object System.Text.StringBuilder
    foreach ($c in $inputStr.ToCharArray()) {
        $val = [int]$c
        if ($val -ge 0xFF01 -and $val -le 0xFF5E) {
            $halfVal = $val - 0xFEE0
            [void]$sb.Append([char]$halfVal)
        } elseif ($val -eq 0x3000) {
            [void]$sb.Append(' ')
        } else {
            [void]$sb.Append($c)
        }
    }
    return $sb.ToString().Trim()
}

$count = 0
foreach ($s in $stocks) {
    $oldName = $s.name
    $newName = Convert-FullWidthToHalfWidth $oldName
    if ($oldName -ne $newName) {
        $s.name = $newName
        $count++
    }
}

Write-Host "Normalized names count: $count"
$jsonOut = ConvertTo-Json $stocks -Depth 10
[System.IO.File]::WriteAllText($path, $jsonOut, [System.Text.Encoding]::UTF8)
Write-Host "Successfully updated $path"
