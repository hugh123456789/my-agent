const BAYER_ORDER = [
  [0, 8, 2, 10],
  [12, 4, 14, 6],
  [3, 11, 1, 9],
  [15, 7, 13, 5],
]

const BAYER = BAYER_ORDER.map((row) => row.map((value) => (value + 0.5) / 16))

export function getDitherInk(ink, column, row) {
  return ink > BAYER[row % 4][column % 4]
}

export function normalizeLuminance(luminance, low, high) {
  return Math.max(0, Math.min(1, (luminance - low) / Math.max(0.05, high - low)))
}
