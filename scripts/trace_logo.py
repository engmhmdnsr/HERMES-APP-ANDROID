#!/usr/bin/env python3
"""Trace logo alpha to SVG path coordinates scaled into 24x24 viewport."""
import numpy as np
import cv2
from PIL import Image

im = Image.open('app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png').convert('RGBA')
alpha = np.array(im)[..., 3]
mask = (alpha > 40).astype(np.uint8) * 255

contours, hierarchy = cv2.findContours(mask, cv2.RETR_CCOMP, cv2.CHAIN_APPROX_SIMPLE)
h, w = mask.shape

# scale factor from image px to 24-viewport with padding
# content should occupy ~20 of 24 units (status icons ~66% safe zone, but notification small icons render 24dp)
content_pad_frac = 0.12
scale = 20 / max(h, w)

def to_path(contour):
    # simplify
    eps = 1.0
    c = cv2.approxPolyDP(contour, eps, True)
    pts = c[:, 0, :]
    # center in image, scale into viewport
    cx, cy = w / 2, h / 2
    out = []
    for x, y in pts:
        nx = (x - cx) * scale + 12
        ny = (y - cy) * scale + 12
        out.append((round(nx, 2), round(ny, 2)))
    return out

print(f'image {w}x{h}')
outer = []
holes = []
if hierarchy is not None:
    for i in range(len(contours)):
        if hierarchy[0][i][3] == -1:
            outer.append(to_path(contours[i]))
        else:
            holes.append(to_path(contours[i]))
else:
    for c in contours:
        outer.append(to_path(c))

print(f'outer: {len(outer)}, holes: {len(holes)}')
for i, p in enumerate(outer):
    print(f'--- outer {i}: {len(p)} pts ---')
    print(' '.join(f'{x},{y}' for x, y in p))
for i, p in enumerate(holes):
    print(f'--- hole {i}: {len(p)} pts ---')
    print(' '.join(f'{x},{y}' for x, y in p))
