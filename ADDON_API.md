# Stormbound Addon API

Stormbound `0.2.0-SNAPSHOT` includes the first addon loader and public API surface.

## Addon location

Addon JARs are loaded from:

```text
plugins/StormboundSkyblock/addons/
```

The folder can be changed with `addons.folder` in `config.yml`.

## addon.yml

Every addon JAR must contain an `addon.yml` at the root of the JAR:

```yaml
name: StormboundExample
version: 0.1.0-SNAPSHOT
main: net.stormbound.example.ExampleAddon
api-version: 1
depend: []
softdepend: []
```

## Addon entry point

```java
package net.stormbound.example;

import net.stormbound.skyblock.api.addon.StormboundAddon;

public final class ExampleAddon extends StormboundAddon {

    @Override
    public void onLoad() {
        getContext().logger().info("Loaded.");
    }

    @Override
    public void onEnable() {
        int islandCount = getContext().api().islands().getIslandCount();
        getContext().logger().info("Islands: " + islandCount);
    }

    @Override
    public void onDisable() {
        getContext().logger().info("Disabled.");
    }
}
```

## Context helpers

`AddonContext` currently provides:

- Stormbound API access
- addon logger
- addon data folder
- listener registration
- sync task scheduling
- delayed/repeating task scheduling
- async task scheduling

Listeners and tasks registered through the context are automatically cleaned up when the addon disables.

## Island API

The first API exposes read-only island data:

- island lookup by player
- owned island lookup
- island lookup by location
- member role lookup
- member home lookup
- island membership checks
- island count and island collection

Island API calls must run on the main server thread. Async addon tasks should return to the main thread before accessing island state.

## API version

The initial addon API version is `1`. Addons declaring a newer API version will not load.
