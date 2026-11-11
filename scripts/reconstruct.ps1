param(
    [string]$InputJar = 'investigation/ic2.0all.jar',
    [string]$DecompilerJar = 'tools/vineflower.jar',
    [string]$OutputDirectory = 'build/reconstructed',
    [string]$JavaHome = $env:JAVA_HOME
)
$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    $javaCommand = if ($JavaHome) { Join-Path $JavaHome 'bin/java.exe' } else { (Get-Command java).Source }
    if ((Get-FileHash -LiteralPath $InputJar -Algorithm SHA256).Hash -ne '815a1d6e192e6a18eb7ef213baece5e743005957107595983e4e6341ab1add34') {
        throw 'Unsupported original JAR.'
    }
    if (Test-Path -LiteralPath $OutputDirectory) { throw "Output already exists: $OutputDirectory" }
    if (-not (Test-Path -LiteralPath $DecompilerJar)) {
        New-Item -ItemType Directory -Force (Split-Path $DecompilerJar -Parent) | Out-Null
        Invoke-WebRequest 'https://repo.maven.apache.org/maven2/org/vineflower/vineflower/1.11.2/vineflower-1.11.2.jar' -OutFile $DecompilerJar
    }
    if ((Get-FileHash -LiteralPath $DecompilerJar -Algorithm SHA256).Hash -ne 'e1e2415e7f78b34960402c4beddfc88e033d7842a23ecd132a8ec2eadd54f6bf') {
        throw 'Unexpected decompiler SHA-256.'
    }
    New-Item -ItemType Directory -Force $OutputDirectory | Out-Null
    $raw = Join-Path $OutputDirectory 'raw'
    & $javaCommand '-Xmx4G' '-jar' $DecompilerJar '-log=WARN' '-dgs=1' $InputJar $raw
    if ($LASTEXITCODE -ne 0) { throw "Decompiler failed: $LASTEXITCODE" }
    $javaOutput = Join-Path $OutputDirectory 'src/main/java'
    $resourceOutput = Join-Path $OutputDirectory 'src/main/resources'
    New-Item -ItemType Directory -Force $javaOutput, $resourceOutput | Out-Null
    $rawRoot = (Resolve-Path -LiteralPath $raw).Path
    foreach ($file in Get-ChildItem -LiteralPath $rawRoot -Recurse -File) {
        $relative = $file.FullName.Substring($rawRoot.Length + 1)
        $root = if ($file.Extension -eq '.java') { $javaOutput } else { $resourceOutput }
        $destination = Join-Path $root $relative
        New-Item -ItemType Directory -Force (Split-Path $destination -Parent) | Out-Null
        Copy-Item -LiteralPath $file.FullName -Destination $destination
    }
    $sourcePath = Join-Path $javaOutput 'ic2/core/item/block/ItemBlockTileEntity.java'
    $source = [IO.File]::ReadAllText((Resolve-Path -LiteralPath $sourcePath))
    $old = 'TileEntityBlock te = TileEntityBlock.instantiate(teClass);'
    $new = 'TileEntityBlock te = NativeBlockEntities.instantiate(teClass, pos, newState);'
    if ([regex]::Matches($source, [regex]::Escape($old)).Count -ne 1) { throw 'Unexpected placement source.' }
    [IO.File]::WriteAllText((Resolve-Path -LiteralPath $sourcePath), $source.Replace($old, $new), [Text.UTF8Encoding]::new($false))
    Write-Output "Reconstructed source and resources: $OutputDirectory/src"
} finally {
    Pop-Location
}
