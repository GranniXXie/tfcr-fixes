# TFCR Fixes

Defensive fix mod for the **TerraFirma: Rebirth** modpack (Minecraft 1.21.1, NeoForge).

Small, self-contained Mixins and event handlers that patch third-party mod issues
affecting the pack. Everything is written for this pack's exact mod set.

## Server-side fixes

- **Cold Sweat climate guard** — stops tfc_coldsweat's climate modifier from feeding
  garbage temperatures in Cosmonautics dimensions (Moon holds -50°C, deep space near
  absolute zero)
- **Deep space time scale** — Cosmonautics deep-space position time scale handling
- **Create OBB manifold null guard** — prevents NPE crashes from Create contraption
  collision math
- **Sable seat position guard** — keeps seat entity positions sane on physical structures
- **Kinetic conversion bearing clamp** — clamps damping-bearing output to ±256 RPM,
  stopping occasional runaway force spikes
- **Firmalife vat output copy fix** — VatRecipe.assembleOutputs mutated the recipe-cached
  output stack, so after the first jar was taken every later cook consumed ingredients
  and produced nothing; outputs are now copied before mutation
- **TFC enchanting support** — TFC metal gear gets proper enchantability values
  (scaled to the TFC metal ladder), crushed gem powders work as enchanting-table fuel,
  and TFC wooden bookshelves power the enchanting table via TFC's own per-book scaling
  (each stored book +0.5, a full shelf = 3.0 power)

## Client-side fixes

- **Aeronautics DynamicTexture null guard** — client crash fix for null pixel uploads
- **Star plasma throttle** — throttles Cosmonautics' per-frame noise recompute
- **Thirst HUD raycast cache** — caches Cold Sweat's per-frame block raycast
- **FancyMenu compat** — blacklists the Aeronautics contraption diagram screen from
  FancyMenu customization (stops its data columns from flickering)
- **Startup warning filter** — filters the harmless scalablelux dependency warning
- **TFC Pot / Barrel "?" buttons** — clicking the recipe button in TFC's pot and
  barrel GUIs now opens the matching EMI recipe category
- **EMI housekeeping plugin** — hides woodencog's unfinished placeholder items and
  other non-survival clutter from EMI

## Building

Requires JDK 21. Dependency jars (TFC, Create, Cold Sweat, Cosmonautics, EMI,
FancyMenu, Firmalife, tfc_coldsweat) go in `libs/` (not committed).

```
gradlew build
```

Original code by the TFCR Team. License: MIT (see LICENSE).
