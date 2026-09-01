# TFCR Fixes

Defensive fix mod for the **TerraFirma: Rebirth** modpack (Minecraft 1.21.1, NeoForge).

Small, self-contained Mixins that patch third-party mod issues affecting the pack:

- Guards Aeronautics' DynamicTexture uploads against null pixels (client crash fix)
- Stops tfc_coldsweat's climate modifier from feeding garbage temperatures in
  Cosmonautics dimensions (enforces Moon -50C / deep space near absolute zero)
- Throttles Cosmonautics' per-frame star plasma noise recompute (render thread)
- Caches Cold Sweat's per-frame thirst-HUD block raycast (render thread)
- Deep space time scale handling
- Blacklists the Aeronautics contraption diagram screen from FancyMenu customization
  (stops its data columns from flickering)
- Filters the harmless scalablelux dependency warning at startup

Original code by the TFCR Team. License: MIT (see LICENSE).
