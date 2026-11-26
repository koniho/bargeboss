"""Frame registration for the player-captain portrait sheets.

Holds the per-captain face-feature layout (eyes, brows, mouth and glasses as fractions of the
neutral frame's bounding box), dense ECC registration restricted to the static parts of a head,
and the compositor that rebuilds every expression frame on top of the neutral frame so the head
outline, hair, glasses, ears and shoulders stay pixel-identical and only the face changes.

Measure a packed 3x2 sheet (static-region residuals of every frame against neutral):
  python3 tools/portrait_registration.py SHEET.png CAPTAIN [--cell 192]
"""

import argparse

import cv2
import numpy as np
from PIL import Image
from scipy import ndimage

# Ellipses are (centre x, centre y, radius x, radius y) in fractions of the neutral frame's
# alpha bounding box. "anchor" names the pixels a feature is registered on before it is
# blended: "glasses" uses the neutral frame's glasses, an ellipse list uses those static areas.
# "inside" keeps a feature within the neutral silhouette; only the mouth may reshape the jaw.
# "erase" is a reach in pixels around the neutral glasses where the expression's own glasses
# strokes are inpainted away before the blend (see-through glasses drawn at another tilt);
# the neutral glasses laid on top replace them.
CAPTAINS = {
    "whitney": {
        "pick": ["500,150", "845,150", "685,430", "1430,150", "380,710", "1250,430"],
        "scale": 2.3,
        "pockets": 0.5,
        "mirror": True,
        "features": [
            {
                "ellipse": (0.78, 0.4, 0.25, 0.13),
                "anchor": "glasses",
                "inside": True,
                "erase": None,
            },
            {
                "ellipse": (0.82, 0.71, 0.2, 0.15),
                "anchor": [
                    (0.88, 0.52, 0.08, 0.05),
                    (0.6, 0.6, 0.1, 0.1),
                    (0.33, 0.57, 0.1, 0.12),
                ],
                "inside": False,
                "erase": None,
            },
        ],
        "glasses": {"box": (0.45, 0.3, 1.0, 0.56), "dark": 70, "fill": True, "band": 7},
    },
    "wesley": {
        "pick": ["460,240", "830,240", "660,510", "1420,240", "370,790", "1230,510"],
        "scale": 2.3,
        "pockets": 0.0,
        "mirror": False,
        "features": [
            {
                "ellipse": (0.69, 0.345, 0.24, 0.1),
                "anchor": [(0.62, 0.17, 0.25, 0.1), (0.85, 0.45, 0.1, 0.07)],
                "inside": True,
                "erase": None,
            },
            {
                "ellipse": (0.8, 0.62, 0.22, 0.16),
                "anchor": [(0.82, 0.44, 0.14, 0.07), (0.45, 0.55, 0.15, 0.15)],
                "inside": False,
                "erase": None,
            },
        ],
        "glasses": None,
    },
    "dot": {
        "pick": ["460,200", "820,200", "650,470", "1420,200", "380,750", "1240,470"],
        "scale": 2.6,
        "pockets": 0.0,
        "mirror": False,
        "features": [
            {
                "ellipse": (0.69, 0.31, 0.18, 0.12),
                "anchor": "glasses",
                "inside": True,
                "erase": 5,
            },
            {
                "ellipse": (0.85, 0.6, 0.18, 0.14),
                "anchor": [(0.85, 0.44, 0.12, 0.06), (0.5, 0.55, 0.12, 0.12)],
                "inside": False,
                "erase": None,
            },
        ],
        "glasses": {"box": (0.2, 0.3, 1.0, 0.5), "dark": 70, "fill": False, "band": 0},
    },
}

BASE_BAND = 0.1
FEATHER = 0.025


def alpha_box(alpha):
    """Bounding box (left, top, right, bottom) of the occupied pixels."""
    rows, cols = np.nonzero(alpha > 8)
    return cols.min(), rows.min(), cols.max(), rows.max()


def ellipse_field(shape, box, ellipse, grow=1.0):
    """Signed inside distance (pixels, approximate) of every pixel to a box-relative ellipse."""
    left, top, right, bottom = box
    width, height = right - left, bottom - top
    cx, cy = left + ellipse[0] * width, top + ellipse[1] * height
    rx, ry = ellipse[2] * width * grow, ellipse[3] * height * grow
    yy, xx = np.mgrid[0 : shape[0], 0 : shape[1]].astype(np.float32)
    radius = np.sqrt(((xx - cx) / rx) ** 2 + ((yy - cy) / ry) ** 2)
    return (1 - radius) * min(rx, ry)


def soft_ellipse(shape, box, ellipse, feather):
    """Feathered ellipse mask: 1 well inside, ramping to 0 at the ellipse edge."""
    return np.clip(ellipse_field(shape, box, ellipse) / feather, 0, 1).astype(
        np.float32
    )


def gray(cell):
    """Matching signal: luminance composited over mid grey, so the outline is an edge too."""
    alpha = cell[..., 3:] / 255.0
    rgb = cell[..., :3] * alpha + 128.0 * (1 - alpha)
    return (rgb @ np.array([0.299, 0.587, 0.114], np.float32)).astype(np.float32)


def glasses_mask(cell, box, spec):
    """The neutral frame's glasses: the largest dark stroke structure inside the glasses box."""
    if not spec:
        return np.zeros(cell.shape[:2], bool)
    left, top, right, bottom = box
    width, height = right - left, bottom - top
    x0, y0, x1, y1 = spec["box"]
    window = np.zeros(cell.shape[:2], bool)
    window[
        int(top + y0 * height) : int(top + y1 * height),
        int(left + x0 * width) : int(left + x1 * width),
    ] = True
    dark = (cell[..., :3].max(2) < spec["dark"]) & (cell[..., 3] > 128) & window
    labels, count = ndimage.label(dark)
    if not count:
        return dark
    sizes = np.bincount(labels.ravel())
    sizes[0] = 0
    frame = labels == sizes.argmax()
    if spec["fill"]:
        frame = ndimage.binary_fill_holes(ndimage.binary_closing(frame, iterations=2))
    return ndimage.binary_dilation(frame, iterations=2) & window


def glasses_cover(neutral, glasses, spec):
    """Glasses pixels pinned to neutral: the whole wire frame, or the outer band of filled
    sunglasses plus their bright lens glints, so the eyes behind the lenses can still change
    while the glasses and their reflections stay put."""
    if not spec or not spec["band"]:
        return glasses
    band = glasses & ~ndimage.binary_erosion(glasses, iterations=spec["band"])
    glints = glasses & (neutral[..., :3].min(2) > 170)
    return band | ndimage.binary_dilation(glints, iterations=2)


def erase_glasses(cell, glasses, reach):
    """Inpaint a premultiplied cell's own glasses strokes near the neutral glasses.

    Strokes are the dark and the grey (low saturation, not bright) pixels within `reach`
    pixels of the neutral frame's glasses, so brows and eyes further away are kept.
    """
    straight = unpremultiply(cell)
    rgb = straight[..., :3]
    spread = rgb.max(2) - rgb.min(2)
    stroke = (rgb.max(2) < 90) | ((spread < 30) & (rgb.max(2) < 200))
    near = ndimage.binary_dilation(glasses, iterations=reach)
    strokes = ndimage.binary_dilation(stroke & near, iterations=1)
    pixels = np.clip(rgb + 0.5, 0, 255).astype(np.uint8)
    straight[..., :3] = cv2.inpaint(
        pixels, strokes.astype(np.uint8), 4, cv2.INPAINT_TELEA
    )
    return premultiply(straight)


def feature_union(shape, box, captain, grow):
    """All feature ellipses of a captain, grown by a factor, as one boolean mask."""
    union = np.zeros(shape, bool)
    for feature in captain["features"]:
        union |= ellipse_field(shape, box, feature["ellipse"], grow) > 0
    return union


def static_mask(cell, box, captain):
    """Pixels that must not move between frames: the solid head minus faces and the base band."""
    shape = cell.shape[:2]
    solid = ndimage.binary_erosion(cell[..., 3] > 230, iterations=1)
    edge = ndimage.binary_dilation(cell[..., 3] > 8, iterations=3) & ~solid
    mask = (solid | edge) & ~feature_union(shape, box, captain, 1.25)
    floor = box[3] - BASE_BAND * (box[3] - box[1])
    mask[int(floor) :] = False
    return mask


def ecc(template, image, mask, warp=None, levels=3):
    """Affine warp mapping template pixels onto image, estimated by masked ECC over a pyramid.

    Returns the 2x3 warp and the final correlation; when ECC does not converge on a level
    the previous estimate is kept.
    """
    warp = np.eye(2, 3, dtype=np.float32) if warp is None else warp.astype(np.float32)
    score = 0.0
    criteria = (cv2.TERM_CRITERIA_EPS | cv2.TERM_CRITERIA_COUNT, 300, 1e-6)
    for level in range(levels - 1, -1, -1):
        factor = 2**level
        size = (template.shape[1] // factor, template.shape[0] // factor)
        small_t = cv2.resize(template, size, interpolation=cv2.INTER_AREA)
        small_i = cv2.resize(image, size, interpolation=cv2.INTER_AREA)
        small_m = cv2.resize(
            mask.astype(np.uint8), size, interpolation=cv2.INTER_NEAREST
        )
        scaled = warp.copy()
        scaled[:, 2] /= factor
        try:
            score, scaled = cv2.findTransformECC(
                small_t, small_i, scaled, cv2.MOTION_AFFINE, criteria, small_m, 3
            )
        except cv2.error:
            continue
        warp = scaled.copy()
        warp[:, 2] *= factor
    return warp, score


def edges(image):
    """Gradient magnitude of a grey image, the signal glasses rims are matched on."""
    gx = cv2.Sobel(image, cv2.CV_32F, 1, 0, ksize=3)
    gy = cv2.Sobel(image, cv2.CV_32F, 0, 1, ksize=3)
    return cv2.GaussianBlur(np.sqrt(gx * gx + gy * gy), (0, 0), 1.0)


def as3(warp):
    """A 2x3 affine warp as a 3x3 matrix."""
    return np.vstack([warp.astype(np.float64), [0.0, 0.0, 1.0]])


def inverse_warp(image, warp):
    """Resample an image so the warp's image coordinates land on the template grid."""
    return cv2.warpAffine(
        image,
        warp.astype(np.float32),
        (image.shape[1], image.shape[0]),
        flags=cv2.INTER_LINEAR | cv2.WARP_INVERSE_MAP,
    )


def best_shift(aligned, patch, weight, corner, reach):
    """Masked template match of an edge patch around its own corner: (score, 3x3 shift)."""
    x0, y0 = corner
    left, top = max(x0 - reach, 0), max(y0 - reach, 0)
    window = aligned[
        top : y0 + patch.shape[0] + reach, left : x0 + patch.shape[1] + reach
    ]
    scores = cv2.matchTemplate(window, patch, cv2.TM_CCORR_NORMED, mask=weight)
    _, score, _, (found_x, found_y) = cv2.minMaxLoc(scores)
    shift = np.eye(3)
    shift[0, 2] = found_x + left - x0
    shift[1, 2] = found_y + top - y0
    return score, shift


def edge_target(template, mask):
    """The template's masked edge patch: (patch, weight, top-left corner, mask centroid)."""
    rows, cols = np.nonzero(mask)
    region = (slice(rows.min(), rows.max() + 1), slice(cols.min(), cols.max() + 1))
    return (
        edges(template)[region],
        mask[region].astype(np.float32),
        (cols.min(), rows.min()),
        (float(cols.mean()), float(rows.mean())),
    )


def grid_best(image, target, warp, reach):
    """Best (score, warp) over a rotation/scale grid about the target centroid."""
    patch, weight, corner, centre = target
    best = (-2.0, warp)
    for angle in np.arange(-8.0, 8.1, 1.0):
        for scale in np.arange(0.94, 1.061, 0.015):
            trial = as3(warp) @ as3(cv2.getRotationMatrix2D(centre, angle, scale))
            aligned = edges(inverse_warp(image, trial[:2]))
            score, shift = best_shift(aligned, patch, weight, corner, reach)
            if score > best[0]:
                best = (score, (trial @ shift)[:2].astype(np.float32))
    return best


def search(template, image, mask, warp, reach=18):
    """Exhaustive similarity search around a warp, then masked ECC on the same pixels.

    Rotation and scale about the mask centroid are tried on a grid; for each, the best
    translation comes from masked template matching of edge maps. This finds glasses drawn
    at a noticeably different tilt, where ECC alone would stop in a local optimum; the ECC
    refinement is kept only when it matches the edges better.
    """
    _, found = grid_best(image, edge_target(template, mask), warp, reach)
    refined, _ = ecc(template, image, mask, found, levels=2)
    candidates = [found, refined]
    scores = [edge_match(template, image, mask, each) for each in candidates]
    return candidates[int(np.argmax(scores))], max(scores)


def edge_match(template, image, mask, warp):
    """Normalized correlation of edge maps over the mask after warping image onto template."""
    moved = edges(inverse_warp(image, warp))[mask]
    fixed = edges(template)[mask]
    moved, fixed = moved - moved.mean(), fixed - fixed.mean()
    norm = np.linalg.norm(moved) * np.linalg.norm(fixed) + 1e-6
    return float((moved * fixed).sum() / norm)


def describe(warp, mask):
    """Offset at the static centroid, rotation (degrees), scale and RMS static displacement."""
    rows, cols = np.nonzero(mask)
    points = np.stack([cols, rows, np.ones_like(cols)], 1).astype(np.float64)
    moved = points @ warp.T.astype(np.float64)
    shift = moved - points[:, :2]
    centre = points[:, :2].mean(0)
    offset = np.append(centre, 1.0) @ warp.T.astype(np.float64) - centre
    linear = warp[:, :2].astype(np.float64)
    rotation = np.degrees(
        np.arctan2(linear[1, 0] - linear[0, 1], linear[0, 0] + linear[1, 1])
    )
    scale = np.sqrt(abs(np.linalg.det(linear)))
    rms = float(np.sqrt((shift**2).sum(1).mean()))
    return offset[0], offset[1], rotation, scale, rms


def difference(warp, reference, mask):
    """Mean offset and RMS displacement between two warps over the masked pixels."""
    rows, cols = np.nonzero(mask)
    points = np.stack([cols, rows, np.ones_like(cols)], 1).astype(np.float64)
    shift = points @ (warp.astype(np.float64) - reference.astype(np.float64)).T
    dx, dy = shift.mean(0)
    return dx, dy, float(np.sqrt((shift**2).sum(1).mean()))


def premultiply(cell):
    """Straight RGBA (0-255) to premultiplied RGBA."""
    out = cell.copy()
    out[..., :3] *= cell[..., 3:] / 255.0
    return out


def unpremultiply(cell):
    """Premultiplied RGBA back to straight RGBA."""
    out = np.clip(cell, 0, 255)
    alpha = out[..., 3:]
    out[..., :3] = np.where(
        alpha > 0, out[..., :3] * 255.0 / np.maximum(alpha, 1e-3), 0
    )
    return np.clip(out, 0, 255)


def warp_cell(cell, warp):
    """Resample a premultiplied cell so the warp's image coordinates land on the template."""
    return cv2.warpAffine(
        cell,
        warp,
        (cell.shape[1], cell.shape[0]),
        flags=cv2.INTER_CUBIC | cv2.WARP_INVERSE_MAP,
        borderMode=cv2.BORDER_CONSTANT,
        borderValue=0,
    )


def anchor_mask(neutral, box, captain, feature, glasses):
    """Static pixels a feature is registered on: the glasses or listed areas around it."""
    shape = neutral.shape[:2]
    if feature["anchor"] == "glasses":
        return ndimage.binary_dilation(glasses, iterations=4)
    area = np.zeros(shape, bool)
    for ellipse in feature["anchor"]:
        area |= ellipse_field(shape, box, ellipse) > 0
    near = ndimage.binary_dilation(neutral[..., 3] > 8, iterations=4)
    return area & near & ~feature_union(shape, box, captain, 1.05)


class NeutralFace:
    """Everything derived once from the neutral frame that expression frames are rebuilt on."""

    def __init__(self, neutral, captain):
        self.captain = captain
        self.box = alpha_box(neutral[..., 3])
        self.template = gray(neutral)
        self.still = static_mask(neutral, self.box, captain)
        self.glasses = glasses_mask(neutral, self.box, captain["glasses"])
        pinned = glasses_cover(neutral, self.glasses, captain["glasses"])
        self.cover = ndimage.gaussian_filter(pinned.astype(np.float32), 0.7)[..., None]
        self.base = premultiply(neutral)
        solid = ndimage.binary_erosion(neutral[..., 3] > 128, iterations=3)
        self.within = ndimage.gaussian_filter(solid.astype(np.float32), 1.5)
        self.neutral = neutral

    def feature_warp(self, image, feature, global_warp):
        """Warp registering one feature's anchors, and its offset report against global."""
        anchors = anchor_mask(
            self.neutral, self.box, self.captain, feature, self.glasses
        )
        if feature["anchor"] == "glasses":
            local, _ = search(self.template, image, anchors, global_warp)
        else:
            local, _ = ecc(self.template, image, anchors, global_warp, levels=2)
        dx, dy, rms = difference(local, global_warp, anchors)
        return local, f"feature {dx:+.2f},{dy:+.2f} rms {rms:.2f}"

    def feature_blend(self, feature):
        """Feathered blend weight of one feature, kept inside the silhouette when asked."""
        feather = FEATHER * (self.box[3] - self.box[1])
        blend = soft_ellipse(
            self.neutral.shape[:2], self.box, feature["ellipse"], feather
        )
        if feature["inside"]:
            blend = blend * self.within
        return blend[..., None]

    def rebuild(self, frame):
        """The neutral frame carrying only this frame's features; returns it and a report."""
        image = gray(frame)
        global_warp, score = ecc(self.template, image, self.still)
        dx, dy, rot, scale, rms = describe(global_warp, self.still)
        parts = [
            f"ecc {score:.3f}: static {dx:+.2f},{dy:+.2f} rot {rot:+.2f} x{scale:.4f}"
            f" rms {rms:.2f}"
        ]
        source = premultiply(frame)
        built = self.base.copy()
        for feature in self.captain["features"]:
            local, report = self.feature_warp(image, feature, global_warp)
            parts.append(report)
            moved = warp_cell(source, local)
            if feature["erase"]:
                moved = erase_glasses(moved, self.glasses, feature["erase"])
            blend = self.feature_blend(feature)
            built = built * (1 - blend) + moved * blend
        built = built * (1 - self.cover) + self.base * self.cover
        return unpremultiply(built), "; ".join(parts)


def composite(frames, captain, log=print):
    """Rebuild frames 1..n as the neutral frame with only the expression's face features.

    Each frame is first registered to neutral on the static head; each feature is then
    registered on its own anchors (glasses or nearby static areas), warped onto the neutral
    face and blended in with a feathered ellipse. The neutral glasses are laid back on top so
    they sit identically on every frame.
    """
    face = NeutralFace(frames[0], captain)
    out = [frames[0]]
    for index, frame in enumerate(frames[1:], 1):
        built, report = face.rebuild(frame)
        out.append(built)
        log(f"frame {index} {report}")
    return out


def load_cells(path, cell):
    """The six cells of a packed 3x2 portrait sheet as float RGBA arrays."""
    sheet = np.asarray(Image.open(path).convert("RGBA"), np.float32)
    return [
        sheet[row * cell : (row + 1) * cell, col * cell : (col + 1) * cell]
        for row in range(2)
        for col in range(3)
    ]


def measure(cells, captain):
    """Per-frame static-region residual of every frame against the neutral frame.

    Rows are (frame, dx, dy, rotation, scale, rms displacement, static pixel mean abs diff).
    """
    neutral = cells[0]
    box = alpha_box(neutral[..., 3])
    still = static_mask(neutral, box, captain)
    template = gray(neutral)
    rows = []
    for index, frame in enumerate(cells[1:], 1):
        warp, _ = ecc(template, gray(frame), still)
        dx, dy, rot, scale, rms = describe(warp, still)
        diff = np.abs(premultiply(frame) - premultiply(neutral)).max(2)[still].mean()
        rows.append((index, dx, dy, rot, scale, rms, float(diff)))
    return rows


def main():
    """Print the static-region residuals of a packed sheet."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("sheet")
    parser.add_argument("captain", choices=sorted(CAPTAINS))
    parser.add_argument("--cell", type=int, default=192)
    args = parser.parse_args()
    names = ["neutral", "small talk", "open talk", "blink", "mad", "wince"]
    print(f"{args.sheet} ({args.captain}, {args.cell} px cells)")
    print("frame        dx      dy     rot    scale   rms px  static diff")
    for index, dx, dy, rot, scale, rms, diff in measure(
        load_cells(args.sheet, args.cell), CAPTAINS[args.captain]
    ):
        print(
            f"{names[index]:<10} {dx:+7.2f} {dy:+7.2f} {rot:+7.2f} {scale:8.4f}"
            f" {rms:7.2f} {diff:12.2f}"
        )


if __name__ == "__main__":
    main()
