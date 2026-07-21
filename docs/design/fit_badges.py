# -*- coding: utf-8 -*-
"""Crop each badge master to its coin and downscale into drawable-nodpi.

The model leaves a little white margin around the medal, and the app clips the
bitmap to a circle — so an inset coin would show a white ring at the edge. Find
the coin's bounding box, crop to it, then area-average down to 512px.

No PIL/ImageMagick here: sips writes an uncompressed BMP, this reads it and
hand-encodes the PNG (same pipeline as store-assets/README.md).
"""
import os
import struct
import subprocess
import sys
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
S = os.path.join(ROOT, "docs", "design")
MASTERS = os.path.join(S, "masters", "badges")
DEST = os.path.join(ROOT, "app", "src", "main", "res", "drawable-nodpi")
OUT_PX = 512
WHITE = 238  # anything at/above this in all channels counts as page white


def read_bmp(path):
    data = open(path, "rb").read()
    assert data[:2] == b"BM", "not a BMP"
    off = struct.unpack_from("<I", data, 10)[0]
    w = struct.unpack_from("<i", data, 18)[0]
    h = struct.unpack_from("<i", data, 22)[0]
    bpp = struct.unpack_from("<H", data, 28)[0]
    assert bpp in (24, 32), "unexpected bpp %d" % bpp
    bottom_up = h > 0
    h = abs(h)
    stride = ((bpp * w + 31) // 32) * 4
    px = bpp // 8
    # Flatten to top-down RGB so the resampler can index arithmetically.
    flat = bytearray(w * h * 3)
    for y in range(h):
        src = (h - 1 - y) if bottom_up else y
        base = off + src * stride
        row = data[base:base + w * px]
        d = y * w * 3
        for x in range(w):
            i = x * px
            flat[d] = row[i + 2]
            flat[d + 1] = row[i + 1]
            flat[d + 2] = row[i]
            d += 3
    return w, h, flat


def coin_bbox(w, h, flat, step=4):
    """Bounding box of everything that isn't page white."""
    x0, y0, x1, y1 = w, h, -1, -1
    for y in range(0, h, step):
        row = y * w * 3
        for x in range(0, w, step):
            i = row + x * 3
            if flat[i] < WHITE or flat[i + 1] < WHITE or flat[i + 2] < WHITE:
                if x < x0:
                    x0 = x
                if x > x1:
                    x1 = x
                if y < y0:
                    y0 = y
                if y > y1:
                    y1 = y
    return x0, y0, x1, y1


def resample(w, h, flat, box, out_px):
    """Area-average `box` (x0,y0,x1,y1) down to out_px square."""
    x0, y0, x1, y1 = box
    sw = x1 - x0
    sh = y1 - y0
    rows = []
    for oy in range(out_px):
        sy0 = y0 + oy * sh // out_px
        sy1 = max(sy0 + 1, y0 + (oy + 1) * sh // out_px)
        row = []
        for ox in range(out_px):
            sx0 = x0 + ox * sw // out_px
            sx1 = max(sx0 + 1, x0 + (ox + 1) * sw // out_px)
            r = g = b = n = 0
            for yy in range(sy0, sy1):
                base = yy * w * 3
                for xx in range(sx0, sx1):
                    i = base + xx * 3
                    r += flat[i]
                    g += flat[i + 1]
                    b += flat[i + 2]
                    n += 1
            row.append((r // n, g // n, b // n))
        rows.append(row)
    return rows


def median_cut(counts, n):
    """Classic median-cut palette. The art is flat vector, so a small palette is
    visually lossless and cuts the file to a fraction of truecolour."""
    boxes = [list(counts.keys())]
    while len(boxes) < n:
        # Split the box with the widest spread on any channel.
        best, best_range, best_ch = None, -1, 0
        for box in boxes:
            if len(box) < 2:
                continue
            for ch in range(3):
                lo = min(c[ch] for c in box)
                hi = max(c[ch] for c in box)
                if hi - lo > best_range:
                    best, best_range, best_ch = box, hi - lo, ch
        if best is None:
            break
        best.sort(key=lambda c: c[best_ch])
        # Split at the weighted median so both halves carry similar pixel mass.
        total = sum(counts[c] for c in best)
        acc, cut = 0, 1
        for i, c in enumerate(best):
            acc += counts[c]
            if acc >= total / 2:
                cut = max(1, min(i, len(best) - 1))
                break
        boxes.remove(best)
        boxes.append(best[:cut])
        boxes.append(best[cut:])
    palette = []
    for box in boxes:
        w = sum(counts[c] for c in box) or 1
        palette.append(tuple(
            sum(c[ch] * counts[c] for c in box) // w for ch in range(3)
        ))
    return palette


def write_png(path, width, height, rows, colors=64):
    counts = {}
    for row in rows:
        for px in row:
            counts[px] = counts.get(px, 0) + 1
    palette = median_cut(counts, min(colors, len(counts)))

    nearest = {}

    def index_of(px):
        i = nearest.get(px)
        if i is None:
            i = min(range(len(palette)), key=lambda k: (
                (palette[k][0] - px[0]) ** 2
                + (palette[k][1] - px[1]) ** 2
                + (palette[k][2] - px[2]) ** 2))
            nearest[px] = i
        return i

    raw = bytearray()
    for row in rows:
        raw.append(0)  # filter: None
        raw += bytes(index_of(px) for px in row)

    def chunk(tag, payload):
        return (struct.pack(">I", len(payload)) + tag + payload
                + struct.pack(">I", zlib.crc32(tag + payload) & 0xFFFFFFFF))

    plte = b"".join(bytes(c) for c in palette)
    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 3, 0, 0, 0))
    png += chunk(b"PLTE", plte)
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    open(path, "wb").write(png)


def fit(name):
    src = os.path.join(MASTERS, "badge_%s.png" % name)
    bmp = os.path.join(S, "_badge.bmp")
    subprocess.run(["sips", "-s", "format", "bmp", src, "--out", bmp],
                   capture_output=True, check=True)
    w, h, flat = read_bmp(bmp)
    x0, y0, x1, y1 = coin_bbox(w, h, flat)
    # Square the box on the coin's centre so the circle stays circular.
    cx, cy = (x0 + x1) // 2, (y0 + y1) // 2
    half = max(x1 - x0, y1 - y0) // 2
    half = min(half, cx, cy, w - 1 - cx, h - 1 - cy)
    box = (cx - half, cy - half, cx + half, cy + half)
    rows = resample(w, h, flat, box, OUT_PX)
    dst = os.path.join(DEST, "badge_%s.png" % name)
    write_png(dst, OUT_PX, OUT_PX, rows)
    print("%-16s %dx%d -> crop %dpx -> %s (%d KB)"
          % (name, w, h, half * 2, os.path.basename(dst),
             os.path.getsize(dst) // 1024))


if __name__ == "__main__":
    os.makedirs(DEST, exist_ok=True)
    names = sys.argv[1:] or sorted(
        f[6:-4] for f in os.listdir(MASTERS) if f.startswith("badge_")
    )
    for n in names:
        fit(n)
