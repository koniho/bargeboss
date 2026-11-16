# Gameplay highlights — restored quiet style

The user rejected the broad neon redesign on 2026-11-16 and requested a full
visual rollback. The earlier `e48588a` highlight appearance is the current baseline.
Do not reintroduce thick saturated bands, filled arrows or segmented rings based
on the superseded guidelines in Git history.

Use thin translucent strokes, open arrowheads, continuous rings, restrained
feathering and the original gentle pulse. Restore the original hull-glow assets,
berth corner treatment and interaction highlighting. Preserve readable hulls and
keep docks dark. Any further visual redesign needs a small reviewed example first.

The independent moored-boat lighting correction and world-arrow depth ordering
remain. Arrows use the old open strokes within the retained world depth pass;
lighting, splash stamps, gameplay and physics are outside this visual rollback.

Valve's [Connecting Visuals to Gameplay](https://cdn.fastly.steamstatic.com/apps/valve/2008/MIGS08_ConnectingVisualsToGameplay.pdf)
remains useful background for readability and visual hierarchy. Our rejected
neon implementation was a project interpretation, not a Valve requirement.

See [the original guidance notes](../art-prompts/soft-guidance-v1.md) and
[drawing order](../rendering/drawing-order.md).
