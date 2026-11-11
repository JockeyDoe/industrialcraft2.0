param(
    [string]$InputJar = 'investigation/ic2.0all.jar',
    [string]$OutputJar = 'dist/ic2.0all-render-state-fix.jar',
    [string]$JavaHome = $env:JAVA_HOME
)

$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    $javaCommand = if ($JavaHome) {
        Join-Path $JavaHome 'bin/java.exe'
    } else {
        (Get-Command java -ErrorAction Stop).Source
    }
    if (-not (Test-Path -LiteralPath $InputJar -PathType Leaf)) {
        throw "Original-JAR fehlt: $InputJar. Download und Anleitung siehe README.md."
    }
    & $javaCommand 'src/main/java/PatchPlacement.java' $InputJar $OutputJar
    if ($LASTEXITCODE -ne 0) {
        throw "Patch fehlgeschlagen (Exitcode $LASTEXITCODE). JDK 25 erforderlich."
    }
} finally {
    Pop-Location
}
