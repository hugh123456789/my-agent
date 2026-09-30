---
version: alpha
colors:
  background: "#fbfaf7"
  surface: "#ffffff"
  text: "#1c1917"
  secondary: "#756f66"
  border: "#d8d0c5"
  accent: "#2563eb"
typography:
  display:
    fontFamily: "Space Grotesk, Inter, sans-serif"
    fontSize: "clamp(52px, 7vw, 92px)"
    lineHeight: "0.96"
  body:
    fontFamily: "Space Grotesk, Inter, sans-serif"
    fontSize: "16px"
    lineHeight: "26px"
  mono:
    fontFamily: "Maple Mono, ui-monospace, monospace"
    fontSize: "10px"
    lineHeight: "1.7"
rounded:
  avatar: "50%"
  control: "0"
  social: "50%"
spacing:
  page-gutter: "64px"
  section: "72px"
  card: "22px"
components:
  primary-action:
    background: "#2563eb"
    color: "#1c1917"
    border: "1px solid #1c1917"
  content-card:
    background: "#ffffff"
    border: "1px solid #d8d0c5"
    radius: "0"
---

## Overview

This is a personal brand and content site for a developer-designer. The visual north star is an independent editorial studio: precise grid lines, oversized typography, utilitarian metadata, and one deliberately loud cobalt-blue accent. It should feel authored and tactile, never like a generic portfolio template.

The page is a brand surface, not a dense product dashboard. Restraint wins in body copy and navigation; expression belongs in the hero composition, card hover state, and typography.

## Colors

The paper background and ink text form the default theme. Cobalt blue is the only expressive accent and is reserved for focus, primary action, active markers, text selection, and the hero offset shadow. Dark mode remaps semantic surfaces while preserving the same hierarchy and accent.

## Typography

Space Grotesk carries display and body content because its geometric shapes support the editorial, technical voice. Maple Mono is reserved for metadata, labels, dates, and footer statistics. Chinese text falls back to the system sans stack.

## Layout

Desktop uses a 12-column-inspired asymmetric composition: a compact visual anchor, a dominant hero copy block, and a vertical side note. The main content uses a 24px grid that fades out through the center and returns at the right edge, creating a measured left-to-right transition without making the reading area noisy. The primary navigation is a centered, numbered pill rail with a lime active state. Content below uses a weighted 7/5 split rather than equal cards. Mobile collapses to one column with left-aligned editorial rhythm.

## Elevation & Depth

Static surfaces are flat. Depth comes from borders, offset lime shadows on the avatar and article hover state, and concentric linework in the hero background.

## Shapes

Cards and controls use square corners to reinforce the studio / print language. Circular geometry is limited to the portrait and social controls.

## Components

Icons use Lucide through `@lucide/vue`; icon strokes are light and functional. The brand mark uses Lucide's `Origami` icon as a quiet monochrome mark, shared by the header and hero. Buttons and links use native semantics, visible focus rings, and hover states that change both color and geometry. Runtime CSS variables are the canonical token source; this document mirrors those values.

## Do's and Don'ts

- Do keep the cobalt-blue accent rare and meaningful.
- Do preserve the mono metadata treatment for dates and labels.
- Do use asymmetry and whitespace to create hierarchy.
- Do not add gradients, glassmorphism, or rounded card grids.
- Do not replace meaningful iconography with emoji or text glyphs.
