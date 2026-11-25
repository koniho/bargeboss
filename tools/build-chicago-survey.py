#!/usr/bin/env python3
"""Build the checked-in Chicago River survey (Navy Pier to Wolf Point).

Two stages, both offline:

  --refresh-osm  Reads explicitly downloaded OSM XML in out/wolfpoint-reference and
                 rewrites docs/maps/chicago-river-survey-v1.json. Needs shapely
                 (polygon union/triangulation); review the JSON diff before accepting.
  (default)      Regenerates core/com/bargeboss/game/ChicagoSurvey.java from the
                 reviewed JSON with the standard library only. No network access.

Google Maps 3D/satellite views are visual references for review only; no imagery or
Google-derived geometry is exported. Geometry is (c) OpenStreetMap contributors, ODbL.
"""

import json, math, pathlib, sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
SRC = ROOT / "out/wolfpoint-reference/span.xml"
CANONICAL = ROOT / "docs/maps/chicago-river-survey-v1.json"
JAVA = ROOT / "core/com/bargeboss/game/ChicagoSurvey.java"
ORIGIN_LAT, ORIGIN_LON, UNITS = 41.888, -87.62, 2.0
BBOX = (-87.6430, 41.8815, -87.5940, 41.8945)
N = 111132 * UNITS
E = 111320 * math.cos(math.radians(ORIGIN_LAT)) * UNITS


def world(p):
    """North is world +X, east is world +Y (upper right in the isometric camera)."""
    return [(p[1] - ORIGIN_LAT) * N, (p[0] - ORIGIN_LON) * E]


def refresh():
    import xml.etree.ElementTree as ET
    from shapely.geometry import Polygon, LineString, Point, box
    from shapely.ops import unary_union, polygonize
    import shapely

    root = ET.parse(SRC).getroot()
    nodes = {
        n.get("id"): (float(n.get("lon")), float(n.get("lat")))
        for n in root.findall("node")
    }
    ways = {
        w.get("id"): (
            [n.get("ref") for n in w.findall("nd")],
            {t.get("k"): t.get("v") for t in w.findall("tag")},
        )
        for w in root.findall("way")
    }
    rels = {
        r.get("id"): (
            {t.get("k"): t.get("v") for t in r.findall("tag")},
            [(m.get("type"), m.get("ref"), m.get("role")) for m in r.findall("member")],
        )
        for r in root.findall("relation")
    }

    def coords(wid):
        return [nodes[n] for n in ways[wid][0] if n in nodes]

    def relation_polygon(rid):
        outer, inner = [], []
        for kind, ref, role in rels[rid][1]:
            if kind != "way" or ref not in ways or len(coords(ref)) < 2:
                continue
            (inner if role == "inner" else outer).append(LineString(coords(ref)))

        def build(lines):
            polys = list(polygonize(unary_union(lines)))
            return unary_union(polys) if polys else Polygon()

        return build(outer).difference(build(inner))

    # Metric working frame keeps buffers and simplification in metres.
    K = 111320 * math.cos(math.radians(ORIGIN_LAT))

    def metric(p):
        return ((p[0] - ORIGIN_LON) * K, (p[1] - ORIGIN_LAT) * 111132)

    def lonlat(p):
        return [round(p[0] / K + ORIGIN_LON, 7), round(p[1] / 111132 + ORIGIN_LAT, 7)]

    def mpoly(points):
        return Polygon([metric(p) for p in points])

    def mline(points):
        return LineString([metric(p) for p in points])

    clip = Polygon(
        [
            metric(p)
            for p in [
                (BBOX[0], BBOX[1]),
                (BBOX[2], BBOX[1]),
                (BBOX[2], BBOX[3]),
                (BBOX[0], BBOX[3]),
            ]
        ]
    )

    # Water: river relations, lake, harbours, the lock chamber, Ogden Slip and the
    # confluence basin. Fountains and reflecting pools are dry scenery.
    parts = []
    for rid, (tags, _) in rels.items():
        if tags.get("natural") == "water" and tags.get("water") != "reflecting_pool":
            parts.append(
                shapely.transform(
                    relation_polygon(rid),
                    lambda c: [[metric(q)[0], metric(q)[1]] for q in c],
                )
            )
    for wid, (nd, tags) in ways.items():
        if (
            tags.get("natural") == "water"
            and not tags.get("amenity")
            and len(nd) > 3
            and nd[0] == nd[-1]
        ):
            parts.append(mpoly(coords(wid)))
    water = unary_union([p.buffer(0) for p in parts]).intersection(clip)
    solid = [
        mpoly(coords(w))
        for w, (nd, t) in ways.items()
        if t.get("man_made") in ("pier", "breakwater", "quay")
        and len(nd) > 3
        and nd[0] == nd[-1]
    ]
    water = water.difference(unary_union([s.buffer(0) for s in solid]))
    river = Point(metric((-87.6375, 41.8868)))
    pieces = list(getattr(water, "geoms", [water]))
    main = min(pieces, key=lambda g: g.distance(river))
    # Tiny pilings/finger floats (< 400 m2) are omitted; larger detached breakwaters and
    # piers stay solid by joining each hole to the outer ring with a 0.25 m slit.
    holes = [Polygon(h).simplify(0.6) for h in main.interiors if Polygon(h).area >= 400]
    main = Polygon(main.exterior).simplify(0.6, preserve_topology=True)
    for hole in sorted(holes, key=lambda h: -h.area):
        a, b = shapely.ops.nearest_points(hole.exterior, main.exterior)
        main = main.difference(hole).difference(
            LineString([a, b]).buffer(0.3, cap_style=3)
        )
        if main.geom_type != "Polygon":
            main = max(main.geoms, key=lambda g: g.area)
    assert main.geom_type == "Polygon" and not main.interiors, main.geom_type
    ring = list(main.exterior.coords)[:-1]
    if Polygon(ring).exterior.is_ccw is False:
        ring.reverse()
    triangles = shapely.constrained_delaunay_triangles(Polygon(ring))
    triangles = [list(t.exterior.coords)[:3] for t in triangles.geoms]

    land = clip.difference(Polygon(ring))

    # Buildings: OSM height, else levels at 3.6 m, else a conservative low-rise default.
    def height(tags):
        for key in ("height", "building:height"):
            if tags.get(key):
                try:
                    return float(tags[key].replace("m", "").split()[0])
                except ValueError:
                    pass
        if tags.get("building:levels"):
            try:
                return float(tags["building:levels"]) * 3.6 + 2
            except ValueError:
                pass
        return {"roof": 5, "service": 6, "boathouse": 6, "kiosk": 4}.get(
            tags.get("building"), 14
        )

    buildings = []
    for wid, (nd, tags) in ways.items():
        if (
            not tags.get("building")
            or len(nd) < 4
            or nd[0] != nd[-1]
            or tags.get("attraction") == "big_wheel"
        ):
            continue
        if tags.get("building") == "roof" or tags.get("layer", "0").startswith("-"):
            continue
        fp = mpoly(coords(wid)).buffer(0)
        if fp.area < 60:
            continue
        fp = fp.intersection(land)
        if fp.is_empty or fp.area < 60:
            continue
        if fp.geom_type != "Polygon":
            fp = max(fp.geoms, key=lambda g: g.area)
        rect = fp.minimum_rotated_rectangle
        shape = rect if fp.area / rect.area > 0.82 else fp.simplify(1.5).buffer(0)
        if shape.geom_type != "Polygon":
            continue
        shape = shapely.geometry.polygon.orient(shape, 1.0)
        outline = list(shape.exterior.coords)[:-1]
        if len(outline) > 24:
            shape = shape.simplify(3).buffer(0)
            shape = shapely.geometry.polygon.orient(shape, 1.0)
            outline = list(shape.exterior.coords)[:-1]
        roof = [
            list(t.exterior.coords)[:3]
            for t in shapely.constrained_delaunay_triangles(shape).geoms
        ]
        buildings.append(
            {
                "id": wid,
                "name": tags.get("name", ""),
                "height": round(height(tags), 1),
                "outline": [lonlat(p) for p in outline],
                "roof": [[lonlat(p) for p in tri] for tri in roof],
            }
        )

    # Street guide strips; underground layers and footpaths are omitted.
    roads = []
    road_kinds = {
        "motorway": 0,
        "trunk": 0,
        "primary": 0,
        "secondary": 1,
        "tertiary": 1,
        "residential": 2,
        "unclassified": 2,
        "pedestrian": 3,
    }
    for wid, (nd, tags) in ways.items():
        kind = road_kinds.get(tags.get("highway"))
        if (
            kind is None
            or tags.get("tunnel")
            or tags.get("layer", "0").startswith("-")
            or tags.get("bridge")
        ):
            continue
        line = mline(coords(wid)).intersection(clip)
        if line.is_empty:
            continue
        for seg in getattr(line, "geoms", [line]):
            seg = seg.simplify(2)
            roads.append({"kind": kind, "points": [lonlat(p) for p in seg.coords]})

    parks = []
    for wid, (nd, tags) in ways.items():
        if (
            (
                tags.get("leisure") in ("park", "garden")
                or tags.get("landuse") in ("grass",)
            )
            and len(nd) > 3
            and nd[0] == nd[-1]
        ):
            p = mpoly(coords(wid)).buffer(0).intersection(land)
            for g in getattr(p, "geoms", [p]):
                if g.geom_type == "Polygon" and g.area > 400:
                    parks.append(
                        [lonlat(q) for q in g.simplify(3).exterior.coords][:-1]
                    )

    # Lock geometry from the mapped sector gates (hinge, meeting point, hinge).
    gates = [[list(nodes[n]) for n in ways[w][0]] for w in ("762092229", "762092240")]
    wheel = mpoly(coords("686996484"))
    wheel_rect = list(wheel.minimum_rotated_rectangle.exterior.coords)
    data = {
        "schema": 1,
        "origin": {"lat": ORIGIN_LAT, "lon": ORIGIN_LON},
        "unitsPerMetre": UNITS,
        "bbox": BBOX,
        "coast": [lonlat(p) for p in ring],
        "water": [[lonlat(p) for p in t] for t in triangles],
        "buildings": sorted(buildings, key=lambda b: b["id"]),
        "roads": roads,
        "parks": parks,
        "lock": {"west": gates[0], "east": gates[1], "house": [-87.6063, 41.88861]},
        "wheel": {
            "center": lonlat(wheel.centroid.coords[0]),
            "height": 59.74,
            "footprint": [lonlat(p) for p in wheel_rect[:-1]],
        },
        "places": {
            # Navy Pier south basin, between the pier and the detached inner breakwater.
            "start": [-87.6052, 41.89055],
            # Fire barge alongside the CFD Engine 2 / Marine Safety Station fireboat pier.
            "fireDock": [-87.61175, 41.88808],
            "fireDockHeading": "west",
            "route": [
                [-87.6052, 41.89055],
                [-87.6040, 41.8895],
                [-87.6040, 41.8885],
                [-87.6052, 41.88845],
                [-87.6065, 41.88845],
                [-87.6077, 41.88843],
                [-87.6090, 41.8882],
                [-87.6105, 41.88815],
            ],
            "markers": [[-87.60455, 41.88905, 0], [-87.60455, 41.88785, 1]],
        },
        "sources": {
            "osm": "https://www.openstreetmap.org/ (Overpass extract of the bbox, retrieved 2026-11-24)",
            "river": "https://www.openstreetmap.org/relation/2192735",
            "lake": "https://www.openstreetmap.org/relation/1205149",
            "lock": "https://www.openstreetmap.org/way/762092241",
            "wheel": "https://www.openstreetmap.org/way/686996484",
            "review": "Google Maps satellite/3D used only as a visual cross-check; nothing exported.",
        },
        "attribution": "© OpenStreetMap contributors, ODbL 1.0.",
    }
    CANONICAL.write_text(json.dumps(data, separators=(",", ":")) + "\n")
    print(
        "refreshed",
        CANONICAL,
        "coast",
        len(ring),
        "triangles",
        len(triangles),
        "buildings",
        len(buildings),
        "roads",
        len(roads),
        "parks",
        len(parks),
    )


def generate():
    data = json.loads(CANONICAL.read_text())

    def pts(points):
        return ",".join(f"{v:.1f}f" for p in points for v in world(p))

    def chunked(name, rows, render, size=60):
        out, calls = [], []
        for off in range(0, len(rows), size):
            method = f"{name.lower()}{off}"
            calls.append(method + "()")
            out.append(
                f"    private static float[][] {method}(){{return new float[][]{{"
                + ",".join(render(r) for r in rows[off : off + size])
                + "};}"
            )
        out.append(
            f"    static final float[][] {name}=join(new float[][][]{{"
            + ",".join(calls)
            + "});"
        )
        return out

    coast = data["coast"]
    world_coast = [world(p) for p in coast]
    area = sum(
        a[0] * b[1] - b[0] * a[1]
        for a, b in zip(world_coast, world_coast[1:] + world_coast[:1])
    )
    water = data["water"]
    buildings = data["buildings"]

    # Painter's order: farther (smaller y-x) first. Fixed camera yaw makes this static.
    def depth(b):
        w = [world(p) for p in b["outline"]]
        return max(p[1] - p[0] for p in w)

    buildings.sort(key=depth)
    places = data["places"]
    lines = [
        "package com.bargeboss.game;",
        "",
        "/** Generated from docs/maps/chicago-river-survey-v1.json by tools/build-chicago-survey.py.",
        " * (c) OpenStreetMap contributors, ODbL 1.0. North is world +X; east is world +Y. */",
        "final class ChicagoSurvey {",
        "    private ChicagoSurvey(){}",
        f"    static final float UNITS_PER_METRE={UNITS}f;",
        "    static final float[] COAST=coast();",
        "    private static float[] coast(){return new float[]{" + pts(coast) + "};}",
        f'    static final boolean COAST_POSITIVE={"true" if area > 0 else "false"};',
    ]
    lines += chunked("WATER", water, lambda t: "{" + pts(t) + "}", 400)
    lines += chunked("OUTLINES", buildings, lambda b: "{" + pts(b["outline"]) + "}")
    lines += chunked(
        "ROOFS", buildings, lambda b: "{" + pts([p for t in b["roof"] for p in t]) + "}"
    )
    lines.append(
        "    static final float[] HEIGHTS={"
        + ",".join(f'{b["height"] * UNITS:.1f}f' for b in buildings)
        + "};"
    )
    named = [(i, b["name"]) for i, b in enumerate(buildings) if b["name"]]
    lines.append(
        "    static final int[] NAMED={" + ",".join(str(i) for i, _ in named) + "};"
    )
    lines.append(
        "    static final String[] NAMES={"
        + ",".join(json.dumps(n.upper()) for _, n in named)
        + "};"
    )
    roads = data["roads"]
    lines += chunked("ROADS", roads, lambda r: "{" + pts(r["points"]) + "}")
    lines.append(
        "    static final int[] ROAD_KIND={"
        + ",".join(str(r["kind"]) for r in roads)
        + "};"
    )
    lines += chunked("PARKS", data["parks"], lambda p: "{" + pts(p) + "}")
    lock = data["lock"]
    lines.append("    static final float[] LOCK_WEST={" + pts(lock["west"]) + "};")
    lines.append("    static final float[] LOCK_EAST={" + pts(lock["east"]) + "};")
    lines.append("    static final float[] LOCK_HOUSE={" + pts([lock["house"]]) + "};")
    wheel = data["wheel"]
    lines.append(
        "    static final float[] WHEEL={"
        + pts([wheel["center"]])
        + f',{wheel["height"] * UNITS:.1f}f}};'
    )
    lines.append(
        "    static final float[] WHEEL_FOOTPRINT={" + pts(wheel["footprint"]) + "};"
    )
    lines.append("    static final float[] START={" + pts([places["start"]]) + "};")
    lines.append(
        "    static final float[] FIRE_DOCK={" + pts([places["fireDock"]]) + "};"
    )
    lines.append(
        "    static final float[][] ROUTE={"
        + ",".join("{" + pts([p]) + "}" for p in places["route"])
        + "};"
    )
    lines.append(
        "    static final float[][] MARKERS={"
        + ",".join("{" + pts([m[:2]]) + f",{m[2]}f}}" for m in places["markers"])
        + "};"
    )
    lines.append(
        "    private static float[][] join(float[][][] parts){int n=0;for(float[][] p:parts)n+=p.length;"
        "float[][] a=new float[n][];int i=0;for(float[][] p:parts)for(float[] r:p)a[i++]=r;return a;}"
    )
    lines.append("}")
    JAVA.write_text("\n".join(lines) + "\n")
    print(
        "generated",
        JAVA.relative_to(ROOT),
        "coast",
        len(coast),
        "water",
        len(water),
        "buildings",
        len(buildings),
        "roads",
        len(roads),
        "parks",
        len(data["parks"]),
    )


if __name__ == "__main__":
    if "--refresh-osm" in sys.argv:
        refresh()
    generate()
