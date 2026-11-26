"""Flip-test, difference and animation renders for the player-captain portrait sheets.

For each captain it writes, under OUT_DIR:
  <captain>-flip-<tag>.png   every frame, an onion skin of it over neutral (ghosts show
                             misregistration) and its difference to neutral, at 2x HUD size
  <captain>-diff-<tag>.png   per-frame difference heat maps against neutral at cell size
  <captain>-<tag>.gif        all six frames cycling at HUD size (114 px), plus a 3x copy
Usage:
  python3 tools/captain_portrait_review.py OUT_DIR TAG [--sheets DIR]
"""

import argparse
import os

import numpy as np
from PIL import Image, ImageDraw

SHEETS = {
    "whitney": "portrait-captain-whitney-v1.png",
    "wesley": "portrait-captain-wesley-v1.png",
    "dot": "portrait-captain-dot-v2.png",
}
NAMES = ["neutral", "small talk", "open talk", "blink", "mad", "wince"]
CELL = 192
HUD = 114
BACKDROP = (36, 46, 66, 255)


def cells(path):
    """The six 192 px cells of a runtime sheet."""
    sheet = Image.open(path).convert("RGBA")
    return [
        sheet.crop(
            (i % 3 * CELL, i // 3 * CELL, i % 3 * CELL + CELL, i // 3 * CELL + CELL)
        )
        for i in range(6)
    ]


def on_backdrop(cell):
    """A cell composited over the HUD plate colour."""
    out = Image.new("RGBA", cell.size, BACKDROP)
    out.alpha_composite(cell)
    return out


def heat(frame, neutral):
    """Difference to neutral: black where identical, red to yellow as pixels change."""
    shown = np.asarray(on_backdrop(frame), np.float32)[..., :3]
    still = np.asarray(on_backdrop(neutral), np.float32)[..., :3]
    level = np.clip(np.abs(shown - still).max(2) / 96.0, 0, 1)
    rgb = np.stack(
        [np.clip(level * 3, 0, 1), np.clip(level * 3 - 1, 0, 1), level * 0], 2
    )
    shade = still * 0.18
    return Image.fromarray(
        np.clip(shade * (1 - level[..., None]) + rgb * 255, 0, 255).astype(np.uint8)
    )


def flip_sheet(frames):
    """Rows of frame / onion skin over neutral / difference, one column per frame."""
    size = HUD * 2
    out = Image.new("RGB", (size * 6, size * 3 + 18), (12, 16, 24))
    draw = ImageDraw.Draw(out)
    neutral = frames[0]
    for i, frame in enumerate(frames):
        shown = on_backdrop(frame).resize((size, size), Image.Resampling.LANCZOS)
        ghost = Image.blend(on_backdrop(neutral), on_backdrop(frame), 0.5)
        out.paste(shown.convert("RGB"), (i * size, 18))
        out.paste(
            ghost.resize((size, size), Image.Resampling.LANCZOS).convert("RGB"),
            (i * size, 18 + size),
        )
        out.paste(
            heat(frame, neutral).resize((size, size), Image.Resampling.NEAREST),
            (i * size, 18 + size * 2),
        )
        draw.text((i * size + 4, 3), NAMES[i], fill=(240, 230, 200))
    return out


def diff_sheet(frames):
    """Difference heat maps of every frame against neutral at cell size."""
    out = Image.new("RGB", (CELL * 3, CELL * 2))
    for i, frame in enumerate(frames):
        out.paste(heat(frame, frames[0]), (i % 3 * CELL, i // 3 * CELL))
    return out


def animation(frames, size, path):
    """Cycle all six frames, holding each long enough to read the swap."""
    shown = [
        on_backdrop(frame).resize((size, size), Image.Resampling.LANCZOS).convert("RGB")
        for frame in frames
    ]
    sequence = [
        shown[0],
        shown[1],
        shown[2],
        shown[1],
        shown[0],
        shown[3],
        shown[0],
        shown[4],
        shown[0],
        shown[5],
    ]
    sequence[0].save(
        path, save_all=True, append_images=sequence[1:], duration=220, loop=0
    )


def main():
    """Render the review images of all three captains."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("out_dir")
    parser.add_argument("tag")
    parser.add_argument("--sheets", default="assets/sprites")
    args = parser.parse_args()
    os.makedirs(args.out_dir, exist_ok=True)
    for captain, name in SHEETS.items():
        frames = cells(os.path.join(args.sheets, name))
        base = os.path.join(args.out_dir, f"{captain}-")
        flip_sheet(frames).save(f"{base}flip-{args.tag}.png")
        diff_sheet(frames).save(f"{base}diff-{args.tag}.png")
        animation(frames, HUD, f"{base}{args.tag}.gif")
        animation(frames, HUD * 3, f"{base}{args.tag}-3x.gif")


if __name__ == "__main__":
    main()
