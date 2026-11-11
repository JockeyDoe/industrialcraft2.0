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
    $networkPath = Join-Path $javaOutput 'ic2/core/network/Ic2PayloadRegistration.java'
    $networkSource = [IO.File]::ReadAllText((Resolve-Path -LiteralPath $networkPath))
    $registration = 'event.registrar("26.3-1").executesOn(HandlerThread.NETWORK).playBidirectional(Ic2Payload.TYPE, Ic2Payload.CODEC, (packet, context) -> {'
    $server = '(packet, context) -> context.enqueueWork(() -> IC2.network.get(true).receive(packet.bytes(), context.player()))'
    if (-not $networkSource.Contains($registration) -or -not $networkSource.Contains("}, $server);")) { throw 'Unexpected network registration source.' }
    $newline = if ($networkSource.Contains("`r`n")) { "`r`n" } else { "`n" }
    $replacement = 'event.registrar("26.3-1").executesOn(HandlerThread.NETWORK).playBidirectional(Ic2Payload.TYPE, Ic2Payload.CODEC,' + $newline + '         ' + $server + ',' + $newline + '         (packet, context) -> {'
    $networkSource = $networkSource.Replace($registration, $replacement).Replace("}, $server);", '});')
    [IO.File]::WriteAllText((Resolve-Path -LiteralPath $networkPath), $networkSource, [Text.UTF8Encoding]::new($false))
    $guiPath = Join-Path $javaOutput 'ic2/core/GuiIC2.java'
    $guiSource = [IO.File]::ReadAllText((Resolve-Path -LiteralPath $guiPath))
    $oldGui = '      super.extractBackground(graphics, mouseX, mouseY, partialTick);' + $newline + '      this.func_146976_a(partialTick, mouseX, mouseY);'
    $newGui = @(
        '      GuiGraphicsExtractor previous = this.graphics;',
        '      this.graphics = graphics;',
        '      try {',
        '         super.extractBackground(graphics, mouseX, mouseY, partialTick);',
        '         this.func_146976_a(partialTick, mouseX, mouseY);',
        '      } finally {',
        '         this.graphics = previous;',
        '         if (previous == null) {',
        '            GuiTextures.clear();',
        '         }',
        '      }'
    ) -join $newline
    if (-not $guiSource.Contains($oldGui)) { throw 'Unexpected GUI background source.' }
    [IO.File]::WriteAllText((Resolve-Path -LiteralPath $guiPath), $guiSource.Replace($oldGui, $newGui), [Text.UTF8Encoding]::new($false))
    $statePath = Join-Path $javaOutput 'ic2/core/block/TileEntityBlock.java'
    $stateSource = [IO.File]::ReadAllText((Resolve-Path -LiteralPath $statePath))
    foreach ($assignment in @('this.facing = (byte)facing.ordinal();', 'this.active = active;')) {
        if (-not $stateSource.Contains($assignment)) { throw 'Unexpected machine state source.' }
        $stateSource = $stateSource.Replace($assignment, $assignment + $newline + '         this.syncBlockState();')
    }
    $stateSource = $stateSource.Replace('   protected final void rerender() {', '   protected final void rerender() {' + $newline + '      this.syncBlockState();')
    $loadStart = $stateSource.IndexOf('   protected void onLoaded() {')
    $loadEnd = $stateSource.IndexOf('   protected void onUnloaded() {')
    if ($loadStart -lt 0 -or $loadEnd -le $loadStart) { throw 'Unexpected machine load source.' }
    $loadBody = $stateSource.Substring($loadStart, $loadEnd - $loadStart)
    $oldEnd = '      }' + $newline + '   }' + $newline
    if (-not $loadBody.Contains($oldEnd)) { throw 'Unexpected machine load end.' }
    $loadBody = $loadBody.Replace($oldEnd, '         this.syncBlockState();' + $newline + $oldEnd)
    $stateSource = $stateSource.Substring(0, $loadStart) + $loadBody + $stateSource.Substring($loadEnd)
    $helper = @(
        '   private void syncBlockState() {',
        '      Level world = this.getLevel();',
        '      if (world != null && world.getBlockState(this.getBlockPos()).getBlock() == this.getBlockType()) {',
        '         world.setBlock(this.getBlockPos(), this.getBlockState(), 2);',
        '      }',
        '   }',
        '',
        ''
    ) -join $newline
    $stateSource = $stateSource.Replace('   protected boolean clientNeedsExtraModelInfo() {', $helper + '   protected boolean clientNeedsExtraModelInfo() {')
    [IO.File]::WriteAllText((Resolve-Path -LiteralPath $statePath), $stateSource, [Text.UTF8Encoding]::new($false))
    Write-Output "Reconstructed source and resources: $OutputDirectory/src"
} finally {
    Pop-Location
}
