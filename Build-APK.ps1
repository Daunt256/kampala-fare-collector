param([string]$ToolsDirectory = (Join-Path $env:LOCALAPPDATA 'KampalaFareCollectorTools'))
$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
New-Item -ItemType Directory -Force -Path $ToolsDirectory | Out-Null

function Get-CheckedFile([string]$Url,[string]$Path,[string]$Hash,[string]$Algorithm='SHA256') {
    if (!(Test-Path $Path) -or (Get-FileHash $Path -Algorithm $Algorithm).Hash -ne $Hash) {
        Invoke-WebRequest -UseBasicParsing -Uri $Url -OutFile $Path
    }
    if ((Get-FileHash $Path -Algorithm $Algorithm).Hash -ne $Hash) {
        Remove-Item $Path -Force
        throw "Download checksum did not match: $Url"
    }
}

# Portable JDK. No administrator installation or permanent environment changes.
$jdk = Join-Path $ToolsDirectory 'jdk'
if (!(Test-Path (Join-Path $jdk 'bin\javac.exe'))) {
    Write-Host 'Downloading a portable Eclipse Temurin JDK 17...'
    $assets = Invoke-RestMethod 'https://api.adoptium.net/v3/assets/latest/17/hotspot?architecture=x64&image_type=jdk&os=windows&vendor=eclipse'
    $package = $assets[0].binary.package
    if (!$package.link.StartsWith('https://github.com/adoptium/') -or $package.checksum -notmatch '^[a-fA-F0-9]{64}$') { throw 'Unexpected JDK download metadata.' }
    $jdkZip = Join-Path $ToolsDirectory 'jdk17.zip'
    Get-CheckedFile $package.link $jdkZip $package.checksum
    $unpack = Join-Path $ToolsDirectory 'jdk-unpack'
    if (Test-Path $unpack) { Remove-Item $unpack -Recurse -Force }
    Expand-Archive $jdkZip $unpack -Force
    $folder = Get-ChildItem $unpack -Directory | Select-Object -First 1
    if (Test-Path $jdk) { Remove-Item $jdk -Recurse -Force }
    Move-Item $folder.FullName $jdk
}
$env:JAVA_HOME = $jdk
$env:PATH = (Join-Path $jdk 'bin') + ';' + $env:PATH

$gradle = Join-Path $ToolsDirectory 'gradle-8.9\bin\gradle.bat'
if (!(Test-Path $gradle)) {
    Write-Host 'Downloading Gradle 8.9...'
    $gradleZip = Join-Path $ToolsDirectory 'gradle-8.9-bin.zip'
    $hash = ([string](Invoke-WebRequest -UseBasicParsing 'https://services.gradle.org/distributions/gradle-8.9-bin.zip.sha256').Content).Trim()
    if ($hash -notmatch '^[a-fA-F0-9]{64}$') { throw 'Unexpected Gradle checksum.' }
    Get-CheckedFile 'https://services.gradle.org/distributions/gradle-8.9-bin.zip' $gradleZip $hash
    Expand-Archive $gradleZip $ToolsDirectory -Force
}

$sdk = Join-Path $ToolsDirectory 'android-sdk'
$sdkManager = Join-Path $sdk 'cmdline-tools\latest\bin\sdkmanager.bat'
if (!(Test-Path $sdkManager)) {
    Write-Host 'Downloading Android command-line tools from Google...'
    [xml]$repository = (Invoke-WebRequest -UseBasicParsing 'https://dl.google.com/android/repository/repository2-1.xml').Content
    # Pin the CLI tools to a release compatible with JDK 17.
    $remote = $repository.SelectSingleNode("//*[local-name()='remotePackage' and @path='cmdline-tools;12.0']")
    if (!$remote) { throw 'Android command-line tools 12.0 metadata was not returned. Use Android Studio or the GitHub build instead.' }
    $archive = $remote.SelectSingleNode(".//*[local-name()='archive'][*[local-name()='host-os' and text()='windows']]")
    if (!$archive) { throw 'Google did not return Windows command-line tool metadata.' }
    $relativeUrl = $archive.SelectSingleNode("./*[local-name()='complete']/*[local-name()='url']").InnerText
    $checksum = $archive.SelectSingleNode("./*[local-name()='complete']/*[local-name()='checksum']").InnerText
    if ($relativeUrl -notmatch '^commandlinetools-win-[0-9]+_latest\.zip$' -or $checksum -notmatch '^[a-fA-F0-9]{40}$') { throw 'Unexpected Android download metadata.' }
    $sdkZip = Join-Path $ToolsDirectory 'android-commandline.zip'
    Get-CheckedFile ('https://dl.google.com/android/repository/' + $relativeUrl) $sdkZip $checksum 'SHA1'
    $unpack = Join-Path $ToolsDirectory 'sdk-unpack'
    if (Test-Path $unpack) { Remove-Item $unpack -Recurse -Force }
    Expand-Archive $sdkZip $unpack -Force
    New-Item -ItemType Directory -Force -Path (Join-Path $sdk 'cmdline-tools') | Out-Null
    Move-Item (Join-Path $unpack 'cmdline-tools') (Join-Path $sdk 'cmdline-tools\latest')
}
$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk
Write-Host 'Review the Android SDK licences. Type y only if you agree.'
& $sdkManager --sdk_root=$sdk --licenses
if ($LASTEXITCODE -ne 0) { throw 'Android SDK licence review was not completed.' }
& $sdkManager --sdk_root=$sdk 'platforms;android-35' 'build-tools;35.0.0'
if ($LASTEXITCODE -ne 0) { throw 'Android SDK installation failed.' }

Push-Location $PSScriptRoot
try {
    & $gradle --no-daemon :app:assembleDebug :app:lintDebug
    if ($LASTEXITCODE -ne 0) { throw 'Android build failed. See the errors above.' }
    $output = Join-Path $PSScriptRoot 'Kampala-Fare-Collector-test.apk'
    Copy-Item (Join-Path $PSScriptRoot 'app\build\outputs\apk\debug\app-debug.apk') $output -Force
    Write-Host "APK created: $output"
    Write-Host 'Transfer that APK to your Android phone. See START-HERE.md for the phone test.'
} finally { Pop-Location }
