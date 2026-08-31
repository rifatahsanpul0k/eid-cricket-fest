# Eid Cricket Fest Design System

Selected Stitch direction: Broadcast Clarity Variant.

## Brand Character

Eid Cricket Fest should feel like a professional cricket broadcast for a local village tape-tennis tournament. The design is dark-first, compact, data-forward, and community-owned, with restrained Eid gold accents. It must not resemble betting, fantasy cricket, SaaS dashboards, or generic startup marketing.

## Colors

- Background: `#111415`
- Surface: `#191c1d`
- Elevated surface: `#282a2b`
- Card: `#1d2021`
- Foreground: `#e1e3e4`
- Muted foreground: `#bfc9c3`
- Primary green: `#95d3ba`
- Primary container: `#064e3b`
- Gold accent: `#e9c349`
- Border: `#404944`
- LIVE red: `#d93737`
- Error: `#ffb4ab`

Use semantic CSS variables for all component styling. Do not scatter raw color values through JSX.

## Surface Hierarchy

Base pages use charcoal. Section bands use the surface token. Cards use the card token with a 1px low-contrast border. Active or emphasized panels use elevated surface, gold, or LIVE red based on status. Depth comes from tonal layering and borders, not large shadows.

## Typography

- Body: Inter, 16px default, regular weight.
- Headings: Oswald, uppercase, 600-700 weight.
- Scores and data labels: JetBrains Mono.
- Letter spacing remains normal in implementation for predictable rendering.

## Score Typography

Live score numerals are the strongest element on the page. Use JetBrains Mono at 48px on mobile and larger on wider screens where space allows. Team names use Oswald, while overs, target, match number, and table metrics use JetBrains Mono.

## Spacing

Use an 8px rhythm. Mobile content margins are 16px. Section vertical padding is 48px by default. Internal card spacing is compact: 16px to 24px depending on hierarchy.

## Container Widths

The public layout uses `max-w-7xl` for desktop and full-width mobile sections with 16px gutters. Data-heavy blocks should avoid horizontal overflow at 375px.

## Responsive Behavior

Primary design target is 375px. Navigation collapses to a sheet menu. Cards stack into a single column. Desktop may use multi-column layouts, but the mobile order remains: hero, live score, fixtures, standings, tournament info, registration, footer.

## Border Radii

Core radius is 4px. Buttons, cards, and structural panels use small radii. Status chips may be fully rounded so they read as indicators rather than containers.

## Shadows

Avoid large drop shadows. Prefer 1px borders, tonal layering, and strong contrast.

## Buttons

Primary tournament actions use gold with dark text. Secondary actions use outlined white/green borders. Touch targets should be at least 44px tall on mobile.

## Badges

LIVE uses red with white text and must include the word `LIVE`. Tournament status badges use gold or muted surfaces depending on importance. Badges are not color-only indicators.

## LIVE Treatment

The live match card gets the strongest hierarchy: status, teams, score, overs/target, then match context. The LIVE badge uses red and monospaced uppercase text. Empty live state stays in the same card language.

## Cards

Cards use dark surfaces, white/10 style borders, compact spacing, and uppercase heading labels where appropriate. Match cards stay scannable and avoid decorative clutter.

## Tables

Standings tables use compact rows, monospaced numeric columns, and gold emphasis for rank and points. Horizontal scrolling is allowed only for dense table content, not the whole page.

## Navigation

The header is sticky, dark, and lightly blurred. Brand text uses Oswald uppercase. Desktop links are compact; mobile navigation uses large tap targets in a sheet.

## Empty And Error States

Backend unavailable, no live match, no upcoming matches, and empty standings use the same card/surface/border system. Messages should remain plain and actionable without fake scores.
