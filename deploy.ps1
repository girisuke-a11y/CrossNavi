$apkSource = "app\build\outputs\apk\debug\app-debug.apk"
$desktopApk = "C:\Users\mayon\OneDrive\デスクトップ\CrossNavi.apk"
Copy-Item $apkSource $desktopApk -Force
Write-Host "Desktop APK updated."

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r $apkSource
& $adb shell am force-stop com.example.crossnavi
& $adb shell am start -n com.example.crossnavi/.MainActivity
Write-Host "App launched on emulator."
