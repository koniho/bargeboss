"""Cuts labelled character-sheet heads out of a near-white sheet into a portrait source.

Keys the paper background out by flood-filling near-white pixels from the sheet border (the
heavy ink outline stops the fill), derives soft edge alpha from distance to the paper colour,
picks the requested heads by their bounding box, registers every frame to the first by
cross-correlating their ink lines, and writes a 1536x1024 3x2 alpha source with one
512 px cell per frame that tools/PackGavinPortrait.java packs like the other captains.

Usage:
  python3 tools/extract_sheet_portraits.py SHEET OUT.png --list
  python3 tools/extract_sheet_portraits.py SHEET OUT.png --pick x,y x,y ... [--preview P.png]
Each --pick point lies inside one head on the sheet, in output frame order.
"""

import argparse

import numpy as np
from PIL import Image
from scipy import ndimage

CELL = 512


def paper_mask(rgb):
    """Background pixels connected to the sheet border."""
    lo = rgb.min(2)
    spread = rgb.max(2) - lo
    pale = (lo > 214) & (spread < 34)
    labels, _ = ndimage.label(pale)
    border = np.unique(
        np.concatenate([labels[0], labels[-1], labels[:, 0], labels[:, -1]])
    )
    border = border[border > 0]
    return np.isin(labels, border)


def key_pockets(rgb, paper, boxes, fraction):
    """Treat enclosed paper pockets (between hair strands) as paper when they lie in the back
    part of a head: left of `fraction` of its width. Teeth, lens glints and pearls sit on the
    face side and are kept."""
    lo = rgb.min(2)
    pale = (lo > 214) & (rgb.max(2) - lo < 34) & ~paper
    pockets, _ = ndimage.label(pale)
    paper = paper.copy()
    for i, sl in enumerate(ndimage.find_objects(pockets), 1):
        if sl is None:
            continue
        cy = (sl[0].start + sl[0].stop) / 2
        cx = (sl[1].start + sl[1].stop) / 2
        for x0, y0, x1, y1 in (box[:4] for box in boxes):
            if x0 <= cx <= x1 and y0 <= cy <= y1 and cx < x0 + (x1 - x0) * fraction:
                paper[sl] |= pockets[sl] == i
                break
    return paper


def cutout_alpha(rgb, paper):
    """Opaque art with a soft rim: edge pixels fade by their closeness to white paper."""
    art = ~paper
    rim = art & ~ndimage.binary_erosion(art, iterations=2)
    whiteness = np.clip((rgb.min(2) - 150) / 100.0, 0, 1)
    alpha = art.astype(np.float32)
    alpha[rim] = np.clip(1.0 - whiteness[rim] * 0.85, 0, 1)
    halo = ndimage.binary_dilation(art, iterations=1) & ~art
    alpha[halo] = np.clip(1.0 - whiteness[halo], 0, 1) * 0.6
    return alpha, art


def components(art, min_area=8000):
    """Bounding boxes of large art components, top-to-bottom then left-to-right."""
    labels, _ = ndimage.label(art)
    boxes = []
    for i, sl in enumerate(ndimage.find_objects(labels), 1):
        area = int((labels[sl] == i).sum())
        if area >= min_area:
            boxes.append((sl[1].start, sl[0].start, sl[1].stop, sl[0].stop, area, i))
    return labels, sorted(boxes, key=lambda b: (b[1] // 120, b[0]))


def decontaminate(rgb, alpha):
    """Remove the white paper blended into soft edge pixels."""
    weight = np.maximum(alpha, 1e-3)[..., None]
    out = (rgb - 255.0 * (1 - alpha[..., None])) / weight
    return np.clip(out, 0, 255)


WORK = 640


def place(img, scale, angle=0.0):
    """Scale (and slightly rotate) a head onto a roomy work canvas resting on a shared floor."""
    size = (round(img.width * scale), round(img.height * scale))
    small = img.resize(size, Image.Resampling.LANCZOS)
    if angle:
        small = small.rotate(angle, Image.Resampling.BICUBIC, expand=True)
    cell = Image.new("RGBA", (WORK, WORK), (0, 0, 0, 0))
    grow = (small.width - size[0]) // 2, (small.height - size[1]) // 2
    cell.alpha_composite(
        small, ((WORK - size[0]) // 2 - grow[0], max(WORK - 80 - size[1], 0) - grow[1])
    )
    return np.asarray(cell, np.float32)


def luma(cell):
    """Matching signal: softened ink lines (dark strokes), which carry the head's features."""
    ink = np.clip((90 - cell[..., :3].mean(2)) / 60, 0, 1) * (cell[..., 3] / 255)
    return ndimage.gaussian_filter(ink, 1.5)


def match(ref, img, window, dx, dy):
    """Normalized correlation of img shifted by (dx, dy) against a normalized window."""
    x0, y0, x1, y1 = window
    win = img[y0 - dy : y1 - dy, x0 - dx : x1 - dx]
    win = win - win.mean()
    return float((win * ref).sum() / (np.linalg.norm(win) + 1e-6))


def correlate(target, img, window, centre=(0, 0), reach=40, step=4):
    """Best (score, dx, dy) placing img onto target over a window, coarse then fine."""
    x0, y0, x1, y1 = window
    ref = target[y0:y1, x0:x1]
    ref = (ref - ref.mean()) / (np.linalg.norm(ref - ref.mean()) + 1e-6)
    best = (-2.0, centre[0], centre[1])
    for stride, span in ((step, reach), (1, step)):
        around = best[1], best[2]
        for dy in range(around[1] - span, around[1] + span + 1, stride):
            for dx in range(around[0] - span, around[0] + span + 1, stride):
                score = match(ref, img, window, dx, dy)
                if score > best[0]:
                    best = (score, dx, dy)
    return best


def parse_args():
    """Command-line options."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("sheet")
    parser.add_argument("out")
    parser.add_argument("--list", action="store_true")
    parser.add_argument("--pick", nargs="*", default=[])
    parser.add_argument("--scale", type=float, default=2.6)
    parser.add_argument("--preview")
    parser.add_argument(
        "--pockets",
        type=float,
        default=0.0,
        help="key enclosed paper left of this head-width fraction",
    )
    parser.add_argument(
        "--under", type=int, default=120, help="bust band height filled from frame zero"
    )
    return parser.parse_args()


def load_sheet(path, pockets):
    """Sheet colour (paper removed), soft alpha, component labels and head boxes."""
    rgb = np.asarray(Image.open(path).convert("RGB"), np.float32)
    paper = paper_mask(rgb)
    alpha, art = cutout_alpha(rgb, paper)
    labels, boxes = components(art)
    if pockets:
        paper = key_pockets(rgb, paper, boxes, pockets)
        alpha, art = cutout_alpha(rgb, paper)
        labels, boxes = components(art)
    return decontaminate(rgb, alpha), alpha, labels, boxes


def head_patches(specs, color, alpha, labels, boxes):
    """RGBA crops of the heads under each pick point, with their sheet heights."""
    patches = []
    heights = []
    for spec in specs:
        px, py = (int(v) for v in spec.split(","))
        label = labels[py, px]
        if not label:
            raise SystemExit(f"pick {spec} is not on a head")
        box = next(b for b in boxes if b[5] == label)
        keep = ndimage.binary_dilation(labels == label, iterations=2)
        region = (slice(box[1], box[3]), slice(box[0], box[2]))
        patch = np.dstack([color[region], alpha[region] * keep[region] * 255])
        patches.append(
            Image.fromarray(np.clip(patch + 0.5, 0, 255).astype(np.uint8), "RGBA")
        )
        heights.append(box[3] - box[1])
    return patches, heights


def align(img, target, scale, ratio):
    """Uniform scale, small rotation and offset that best match frame zero's ink lines.

    Rows on a sheet are drawn at slightly different sizes and poses; the scale stays within
    6% of the head-height ratio and no frame is stretched non-uniformly.
    """
    window = (int(WORK * 0.12), int(WORK * 0.08), int(WORK * 0.62), int(WORK * 0.8))
    best = (-2.0, 0, 0, 1.0, 0)
    for factor in ratio * np.arange(0.94, 1.061, 0.02):
        for angle in range(-10, 11, 2):
            score, dx, dy = correlate(
                target, luma(place(img, scale * factor, angle)), window
            )
            if score > best[0]:
                best = (score, dx, dy, factor, angle)
    _, dx, dy, factor, angle = best
    for fine_factor in (factor - 0.01, factor, factor + 0.01):
        for fine_angle in (angle - 1, angle, angle + 1):
            cand = luma(place(img, scale * fine_factor, fine_angle))
            score, fdx, fdy = correlate(target, cand, window, (dx, dy), reach=4, step=2)
            if score > best[0]:
                best = (score, fdx, fdy, fine_factor, fine_angle)
    return best


def share_base(frames, under_rows):
    """Give every frame frame zero's bust base so packing registers them identically.

    Also fills gaps a rotation exposes at the cut edge of the bust from frame zero (behind
    the frame, lower band only, never over the face) and crops every frame to frame zero's
    footprint so a rotated bust corner cannot widen the packed scale.
    """
    base = frames[0]
    floor = np.nonzero(base[..., 3].max(1) > 128)[0].max()
    ramp = np.clip((np.arange(WORK) - (floor - 44)) / 16.0, 0, 1)[:, None, None]
    frames = [cell * (1 - ramp) + base * ramp for cell in frames]
    under = np.clip((np.arange(WORK) - (floor - under_rows)) / 20.0, 0, 1)[:, None]
    for cell in frames[1:]:
        gap = (1 - cell[..., 3:] / 255) * (base[..., 3:] / 255) * under[..., None]
        cell[..., :3] = cell[..., :3] * (1 - gap) + base[..., :3] * gap
        cell[..., 3:] = cell[..., 3:] + gap * 255
    for cell in frames:
        cell[floor - 3 :] = 0
    rows, cols = np.nonzero(base[..., 3] > 8)
    margin = 3
    left, right = cols.min() - margin, cols.max() + margin
    for cell in frames[1:]:
        cell[:, :left] = 0
        cell[:, right + 1 :] = 0
    return frames, (left, rows.min() - margin, right, rows.max())


def layout(frames, footprint):
    """Centre the shared footprint in each 512 px cell of the 3x2 source sheet."""
    left, top, right, bottom = footprint
    width, height = right - left + 1, bottom - top + 1
    if max(width, height) > CELL - 8:
        raise SystemExit(f"aligned frames span {width}x{height}; lower --scale")
    left = left - (CELL - width) // 2
    top = bottom + 1 + 12 - CELL
    out = Image.new("RGBA", (CELL * 3, CELL * 2), (0, 0, 0, 0))
    y0, x0 = max(top, 0), max(left, 0)
    for i, cell in enumerate(frames):
        crop = np.zeros((CELL, CELL, 4), np.float32)
        part = cell[y0 : top + CELL, x0 : left + CELL]
        crop[
            y0 - top : y0 - top + part.shape[0], x0 - left : x0 - left + part.shape[1]
        ] = part
        islands, _ = ndimage.label(crop[..., 3] > 8)
        sizes = np.bincount(islands.ravel())
        sizes[0] = 0
        crop[islands != sizes.argmax()] = 0
        img = Image.fromarray(np.clip(crop + 0.5, 0, 255).astype(np.uint8), "RGBA")
        out.alpha_composite(img, ((i % 3) * CELL, (i // 3) * CELL))
    return out


def register(patches, heights, scale):
    """Place frame zero and align every other head onto it."""
    reference = place(patches[0], scale)
    target = luma(reference)
    frames = [reference]
    for i, img in enumerate(patches[1:], 1):
        score, dx, dy, factor, angle = align(
            img, target, scale, heights[0] / heights[i]
        )
        print(
            f"frame {i} scale x{factor:.3f} rot {angle} shift {dx},{dy} match {score:.3f}"
        )
        cand = place(img, scale * factor, angle)
        frames.append(np.roll(np.roll(cand, dy, 0), dx, 1))
    return frames


def main():
    """Extract, register and lay out the picked heads."""
    args = parse_args()
    color, alpha, labels, boxes = load_sheet(args.sheet, args.pockets)
    if args.list:
        for box in boxes:
            print(f"box x{box[0]}-{box[2]} y{box[1]}-{box[3]} area {box[4]}")
        return
    patches, heights = head_patches(args.pick, color, alpha, labels, boxes)
    frames = register(patches, heights, args.scale)
    frames, footprint = share_base(frames, args.under)
    out = layout(frames, footprint)
    out.save(args.out)
    if args.preview:
        backdrop = Image.new("RGBA", out.size, (36, 46, 66, 255))
        backdrop.alpha_composite(out)
        backdrop.save(args.preview)


if __name__ == "__main__":
    main()
