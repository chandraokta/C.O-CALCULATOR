# Build Kalku SC-100 without Gradle: aapt2 -> javac -> d8 -> zipalign -> apksigner.
# Usage: pwsh -File app\build.ps1
$ErrorActionPreference = 'Stop'
$app = Split-Path -Parent $MyInvocation.MyCommand.Path
$root = Split-Path -Parent $app
$sdk = Join-Path $root '.tools\android-sdk'
$jdk = Join-Path $root '.tools\jdk'
if (!(Test-Path $jdk)) {
    $jdk = Get-ChildItem (Join-Path $root '.tools\jdk') -Directory | Select-Object -First 1 -ExpandProperty FullName
}
$javaHome = if ($env:JAVA_HOME -and (Test-Path $env:JAVA_HOME)) { $env:JAVA_HOME } else { (Get-ChildItem $jdk -Directory | Select-Object -First 1 -ExpandProperty FullName) }
$javac = Join-Path $javaHome 'bin\javac.exe'
$keytool = Join-Path $javaHome 'bin\keytool.exe'
$androidJar = Join-Path $sdk 'platforms\android-35\android.jar'
$buildTools = Join-Path $sdk 'build-tools\35.0.0'
$aapt2 = Join-Path $buildTools 'aapt2.exe'
$d8 = Join-Path $buildTools 'd8.bat'
$zipalign = Join-Path $buildTools 'zipalign.exe'
$apksigner = Join-Path $buildTools 'apksigner.bat'
$env:JAVA_HOME = $javaHome

foreach ($tool in @($javac, $androidJar, $aapt2, $d8, $zipalign, $apksigner)) {
    if (!(Test-Path $tool)) { throw "Missing build tool: $tool" }
}

$build = Join-Path $app 'build'
$classes = Join-Path $build 'classes'
$gen = Join-Path $build 'gen'
$dex = Join-Path $build 'dex'
Remove-Item $classes, $gen, $dex -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $classes, $gen, $dex | Out-Null

Write-Host '1/6  Compiling resources'
& $aapt2 compile --dir (Join-Path $app 'res') -o (Join-Path $build 'res.zip')
if ($LASTEXITCODE -ne 0) { throw 'aapt2 compile failed' }

Write-Host '2/6  Linking resources and manifest'
& $aapt2 link -o (Join-Path $build 'unsigned.apk') -I $androidJar `
    --manifest (Join-Path $app 'AndroidManifest.xml') -R (Join-Path $build 'res.zip') `
    -A (Join-Path $app 'assets') --java $gen --auto-add-overlay `
    --min-sdk-version 23 --target-sdk-version 35 -0 .ttf
if ($LASTEXITCODE -ne 0) { throw 'aapt2 link failed' }

Write-Host '3/6  Compiling Java'
$sources = Get-ChildItem (Join-Path $app 'src') -Recurse -Filter *.java | ForEach-Object { $_.FullName }
$rjava = Get-ChildItem $gen -Recurse -Filter R.java -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName }
$all = @($sources) + @($rjava)
& $javac -encoding UTF-8 -source 17 -target 17 -classpath $androidJar -d $classes $all
if ($LASTEXITCODE -ne 0) { throw 'javac failed' }

Write-Host '4/6  Dexing'
$classFiles = Get-ChildItem $classes -Recurse -Filter *.class | ForEach-Object { $_.FullName }
& $d8 --release --lib $androidJar --min-api 23 --output $dex $classFiles
if ($LASTEXITCODE -ne 0) { throw 'd8 failed' }

Write-Host '5/6  Packaging and aligning'
$withDex = Join-Path $build 'withdex.apk'
Copy-Item (Join-Path $build 'unsigned.apk') $withDex -Force
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::Open($withDex, [System.IO.Compression.ZipArchiveMode]::Update)
[System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, (Join-Path $dex 'classes.dex'), 'classes.dex', [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
$zip.Dispose()
& $zipalign -f -p 4 $withDex (Join-Path $build 'aligned.apk')
if ($LASTEXITCODE -ne 0) { throw 'zipalign failed' }

Write-Host '6/6  Signing'
$ks = Join-Path $build 'cokalku.keystore'
if (!(Test-Path $ks)) {
    & $keytool -genkeypair -v -keystore $ks -storepass android -keypass android `
        -alias cokalku -keyalg RSA -keysize 2048 -validity 10000 `
        -dname 'CN=COKALKU, OU=Dev, O=COKALKU, L=Jakarta, C=ID'
    if ($LASTEXITCODE -ne 0) { throw 'keytool failed' }
}
$out = Join-Path $build 'C.O-CALCULATOR.apk'
& $apksigner sign --ks $ks --ks-pass pass:android --key-pass pass:android --out $out (Join-Path $build 'aligned.apk')
if ($LASTEXITCODE -ne 0) { throw 'apksigner failed' }
& $apksigner verify --print-certs $out
Write-Host "Built $out"




