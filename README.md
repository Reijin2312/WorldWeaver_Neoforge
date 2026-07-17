![](./public/img/header.png)

<div align="center">

# WorldWeaver: New Dawn

**An independently maintained continuation of WorldWeaver for modern Minecraft versions**

[![CurseForge Downloads](https://img.shields.io/curseforge/dt/1422284?logo=curseforge\&label=CurseForge%20Downloads\&color=F16436)](https://www.curseforge.com/minecraft/mc-mods/worldweaver-neoforge)
[![GitHub Issues](https://img.shields.io/github/issues/Reijin2312/WorldWeaver-New-Dawn?logo=github)](https://github.com/Reijin2312/WorldWeaver-New-Dawn/issues)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1%20%7C%201.21.11%20%7C%2026.1.x-62B47A)](https://www.curseforge.com/minecraft/mc-mods/worldweaver-neoforge/files)
[![Loaders](https://img.shields.io/badge/Loaders-NeoForge%20%7C%20Fabric%20%7C%20Quilt-5C6BC0)](https://www.curseforge.com/minecraft/mc-mods/worldweaver-neoforge)
[![Code License](https://img.shields.io/badge/Code-MIT-blue.svg)](LICENSE)

[CurseForge](https://www.curseforge.com/minecraft/mc-mods/worldweaver-neoforge)
·
[Downloads](https://www.curseforge.com/minecraft/mc-mods/worldweaver-neoforge/files)
·
[Source](https://github.com/Reijin2312/WorldWeaver-New-Dawn)
·
[Issues](https://github.com/Reijin2312/WorldWeaver-New-Dawn/issues)
·
[Discord](https://discord.gg/BHxhJSn5uR)

</div>

---

## About

**WorldWeaver: New Dawn**, also known as **WoVer**, is an independently maintained, unofficial continuation of the WorldWeaver utility library for Fabric, Quilt, and NeoForge.

WorldWeaver provides shared APIs and utilities designed to simplify common Minecraft modding tasks, particularly those related to world generation.

The project follows Minecraft's data-driven world-generation approach and helps mods work with biomes, terrain, surfaces, structures, features, presets, tags, data packs, and other world-generation systems.

The Fabric edition is based on the original WorldWeaver codebase and includes continued maintenance, bug fixes, and compatibility improvements.

The NeoForge edition ports the same functionality to NeoForge with the loader-specific changes required for compatibility.

> [!NOTE]
> WorldWeaver is primarily a developer-facing library used by other mods. It does not add significant standalone gameplay content by itself.

> [!IMPORTANT]
> WorldWeaver: New Dawn is not maintained or endorsed by the original WorldWeaver developers.
>
> Please do not report New Dawn-specific issues to the upstream WorldWeaver repository or the official BetterX support channels. Use the [New Dawn issue tracker](https://github.com/Reijin2312/WorldWeaver-New-Dawn/issues) instead.

---

## Supported versions

| Minecraft version | Mod loader     | Latest published version | Source branch                                                                            |
| ----------------- | -------------- | -----------------------: | ---------------------------------------------------------------------------------------- |
| 1.21.1            | NeoForge       |                `21.0.23` | [`master`](https://github.com/Reijin2312/WorldWeaver-New-Dawn/tree/master)               |
| 1.21.1            | Fabric / Quilt |         `21.0.13-fabric` | [`fabric-1.21.1`](https://github.com/Reijin2312/WorldWeaver-New-Dawn/tree/fabric-1.21.1) |
| 1.21.11           | NeoForge       |                `21.11.3` | [`port/1.21.11`](https://github.com/Reijin2312/WorldWeaver-New-Dawn/tree/port/1.21.11)   |
| 26.1–26.1.2       | NeoForge       |                 `26.1.2` | [`26.1`](https://github.com/Reijin2312/WorldWeaver-New-Dawn/tree/26.1)                   |

Always check the [CurseForge files page](https://www.curseforge.com/minecraft/mc-mods/worldweaver-neoforge/files) and download the correct file for your Minecraft version and mod loader.

---

## Features

WorldWeaver provides modular APIs and shared utilities for:

* biome registration and placement;
* data-driven world generation;
* terrain and world-generator configuration;
* surface rules and surface generation;
* configured and placed features;
* structures and structure placement;
* world presets;
* blocks and items;
* recipes;
* tags;
* events;
* data generation;
* mathematical utilities;
* common cross-mod utilities;
* user-interface helpers;
* integration with vanilla world generation;
* compatibility with other mods and world-generation data packs.

The project is divided into multiple modules, including:

* `wover-biome-api`
* `wover-block-api`
* `wover-common-api`
* `wover-core-api`
* `wover-datagen-api`
* `wover-event-api`
* `wover-feature-api`
* `wover-generator-api`
* `wover-item-api`
* `wover-math-api`
* `wover-preset-api`
* `wover-recipe-api`
* `wover-structure-api`
* `wover-surface-api`
* `wover-tag-api`
* `wover-ui-api`

The distributed WorldWeaver mod file combines the required modules into a single library jar.

---

## New Dawn improvements

WorldWeaver: New Dawn aims to preserve the behavior of the original project while providing shared fixes, world-generation improvements, and loader-specific compatibility changes.

Current improvements include:

* continued maintenance of the Fabric edition;
* support for modern NeoForge versions;
* consistent functionality across supported mod loaders;
* improved TerraBlender compatibility;
* fixed incorrect placement and scaling of external TerraBlender biomes;
* preserved biome placement for non-WorldWeaver biomes instead of remapping them through WorldWeaver biome pickers;
* fixed TerraBlender and BetterEnd compatibility in The End;
* improved TerraBlender End biome-category importing;
* reduced biome-category overlap conflicts that could place biomes in the wrong generation group.

---

## Download and installation

Download WorldWeaver: New Dawn from the official CurseForge page:

### [Download from CurseForge](https://www.curseforge.com/minecraft/mc-mods/worldweaver-neoforge/files)

1. Install a supported Minecraft version.
2. Install the appropriate NeoForge, Fabric, or Quilt loader.
3. Download the WorldWeaver: New Dawn file matching your loader and Minecraft version.
4. Install all required dependencies shown on the selected CurseForge file page.
5. Place the downloaded `.jar` files in your Minecraft `mods` directory.
6. Launch the game.

Make sure the Minecraft version, mod loader, WorldWeaver version, and all dependency versions match.

WorldWeaver normally needs to be installed only when another mod lists it as a required dependency.

---

## Required dependencies

Requirements vary between Minecraft versions and mod loaders. Always check the relations shown on the selected CurseForge file.

### NeoForge

* [WunderLib: New Dawn](https://www.curseforge.com/minecraft/mc-mods/wunderlib-neoforge)

### Fabric and Quilt

* [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
* [WunderLib: New Dawn](https://www.curseforge.com/minecraft/mc-mods/wunderlib-neoforge)

---

## Using WorldWeaver in another mod

Add the BetterX Maven repository to your Gradle configuration:

```groovy
repositories {
    maven {
        url = "https://maven.ambertation.de/releases"
    }
}
```

Define the WorldWeaver version in `gradle.properties`:

```properties
worldweaver_version=21.0.23
```

Use the version matching your Minecraft version and mod loader.

### NeoForge

```groovy
dependencies {
    implementation "org.betterx:worldweaver:${worldweaver_version}"
}
```

Add WorldWeaver as a dependency in `META-INF/neoforge.mods.toml`:

```toml
[[dependencies.your_mod_id]]
modId="wover"
mandatory=true
versionRange="[21.0.0,)"
ordering="NONE"
side="BOTH"
```

Replace `your_mod_id` with the mod ID of your project.

### Fabric and Quilt

```groovy
dependencies {
    modImplementation "org.betterx:worldweaver:${worldweaver_version}"
}
```

Add WorldWeaver to the `depends` section of `fabric.mod.json`:

```json
{
  "depends": {
    "wover": "21.0.x"
  }
}
```

Adjust the version range to match the WorldWeaver release against which your mod was built.

---

## Development setup

Clone the repository:

```bash
git clone https://github.com/Reijin2312/WorldWeaver-New-Dawn.git
cd WorldWeaver-New-Dawn
```

Select the branch for the version and loader you want to work on:

```bash
# NeoForge 1.21.1
git checkout master

# Fabric / Quilt 1.21.1
git checkout fabric-1.21.1

# NeoForge 1.21.11
git checkout port/1.21.11

# NeoForge 26.1.x
git checkout 26.1
```

Import the project into IntelliJ IDEA or another Gradle-compatible IDE.

Use the Java version required by the selected Minecraft and branch version.

---

## Local WunderLib dependency

The project supports loading WunderLib from a neighboring local repository during development.

The directory structure can look like this:

```text
projects/
├── WorldWeaver-New-Dawn/
└── WunderLib/
```

The default local path is configured in `settings.gradle`:

```groovy
def WunderLibPath = '../WunderLib'
```

When the local WunderLib directory is unavailable, the project uses the configured binary dependency instead.

---

## Building

Run:

```bash
./gradlew build
```

On Windows:

```bat
gradlew.bat build
```

The compiled mod files will be available in:

```text
build/libs
```

---

## API stability

WorldWeaver is under active development.

Developers should be aware that:

* APIs may change between versions;
* compatibility cannot be assumed across different Minecraft versions;
* experimental modules may receive breaking changes;
* documentation may be incomplete for systems still under development;
* development or beta builds should be tested before being used in production modpacks.

Use an explicit compatible WorldWeaver version range in your mod metadata.

---

## Reporting issues

Before opening an issue:

1. Make sure you are using the latest available version.
2. Verify that you downloaded the correct file for your Minecraft version and loader.
3. Check that all required dependencies are installed.
4. Confirm that the dependency versions match your WorldWeaver version.
5. Test without unrelated mods when possible.
6. Include the latest log or crash report.
7. Include a complete mod list.
8. Specify whether you are using Fabric, Quilt, or NeoForge.
9. Provide clear steps to reproduce the problem.

Report bugs through the official New Dawn issue tracker:

### [Open an issue](https://github.com/Reijin2312/WorldWeaver-New-Dawn/issues)

Do not report New Dawn-specific bugs to the original WorldWeaver developers.

---

## Contributing

Contributions are welcome.

You can help by:

* reporting and reproducing bugs;
* submitting fixes;
* improving Fabric, Quilt, or NeoForge compatibility;
* improving compatibility with other world-generation mods;
* testing new Minecraft versions;
* improving API consistency;
* improving performance;
* adding or improving documentation;
* improving translations.

Please clearly explain your changes and test them before submitting a pull request.

---

## Maintainer

WorldWeaver: New Dawn is maintained by **Raijin**.

* [CurseForge profile: Raijin2312](https://www.curseforge.com/members/raijin2312/projects)
* [GitHub profile: Reijin2312](https://github.com/Reijin2312)
* [New Dawn Discord](https://discord.gg/BHxhJSn5uR)

---

## Credits and attribution

* WorldWeaver was originally developed by **Frank Bauer / Quiqueck** and the **BetterX Team**.
* The upstream WorldWeaver source is maintained at [`quiqueck/WorldWeaver`](https://github.com/quiqueck/WorldWeaver).
* The original WorldWeaver project is available on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/worldweaver).
* The original code and project assets belong to their respective developers and contributors.
* WorldWeaver: New Dawn is an independent, unofficial continuation maintained by Raijin.
* Special thanks to everyone who reports issues, contributes fixes, improves compatibility, and tests releases.

---

## License

The project source code is distributed under the [MIT License](LICENSE).

Copyright:

* © 2023 Frank Bauer
* © 2026 Raijin

See the [`LICENSE`](LICENSE) file before redistributing or modifying the project.
