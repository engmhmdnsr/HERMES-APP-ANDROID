#!/usr/bin/env python3
"""Convert the real app logo into a white silhouette PNG for notification icon."""
import numpy as np
from PIL import Image

im = Image.open('app/src/main/res/drawable/ic_hermes_logo.png').convert('RGBA')
arr = np.array(im)
alpha = arr[..., 3]

# crop to content bbox
ys, xs = np.where(alpha > 40)
y0, y1, x0, x1 = ys.min(), ys.max(), xs.min(), xs.max()
content = alpha[y0:y1+1, x0:x1+1]

# add ~10% padding around, resize to 96x96 (notification-friendly), keep aspect
pad_frac = 0.10
h, w = content.shape
side = max(h, w)
canvas = int(side * (1 + 2 * pad_frac))
square = np.zeros((canvas, canvas), dtype=np.uint8)
oy, ox = (canvas - h) // 2, (canvas - w) // 2
square[oy:oy+h, ox:ox+w] = content

img = Image.fromarray(square, mode='L').resize((96, 96), Image.LANCZOS)
out = Image.new('RGBA', (96, 96), (255, 255, 255, 0))
out.putalpha(img)
out.save('app/src/main/res/drawable/ic_stat_hermes.png')
print('saved ic_stat_hermes.png 96x96')

# verify: show silhouette
a = np.array(img)
for y in range(0, 96, 3):
    row = ''
    for x in range(0, 96, 2):
        row += '#' if a[y, x] > 128 else ('+' if a[y, x] > 40 else '.')
    print(row)
