# Barge Boss smooth industrial wordmark

2026-11-21. Built-in image generation (`imagegen` skill), transparent-background
logo-brand request. Reference: actual shared renderer title screenshot at
1243x600; used for palette, atmosphere and two-line hierarchy, not pixel glyphs.

Brief: exact text BARGE above BOSS. Preserve warm ivory #fff0ce, gold #ffc52f,
deep navy #101c30 and restrained brick-crimson #a92d37. Custom heavy condensed
industrial lettering with slight forward lean, continuous smooth contours,
chamfered corners, large readable counters, confident painted tugboat-nameplate
character and playful arcade energy. BOSS slightly larger; balanced compact
wide silhouette. Simple navy nameplate with thin red border and subtle warm
edge highlights. No pixel grid, distressed noise, chrome extrusion, extra icons,
anchor, star, slogan or subtitle. True transparent alpha outside logo, readable
at 360px wide. Output isolated logo, not a screenshot or mockup.

Source saved unchanged as `assets/sprites/title-logo-v1.png`: 1774x887, 2:1.
At alpha>=32, bounds 83,85–1728,837; no cell-edge contact. Reviewed on the actual
harbor background in shared desktop renders at 960x600,1243x600,1333x600,960x720.
Shared `ScreenView` draws one 400x200 sprite centered 230,116, giving visible bounds
approximately 49..420 by 35..205. No platform font substitution. UI Rajdhani font,
menu positions and underlying animated title voyage are unchanged.

Replaced the procedural pixel title and removed its now-unused glyph/cache class.
The wordmark is a single static sprite using existing host bitmap caches; it is
not included in upcoming-level rotation manifests. Source decode is about 6MiB;
no glyph/path rasterization, per-frame resizing allocation, or new shader added.
No claim of measured frame-rate improvement.
