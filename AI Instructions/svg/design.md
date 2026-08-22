# Snitrix Music — UI Design Specification

This document is the source of truth for building the UI in Jetpack Compose. Follow it
exactly — do not substitute default Material components, icons, or spacing. Read this
alongside `kotlin_and_android_instructions.md` (architecture/performance rules) and
`design-tokens.md` (raw color/spacing values).

**Assets:** All icons referenced below (e.g. `play`, `heart-outline`) are SVG files in
`/svg` at the project root. Import each via Android Studio → New → Vector Asset →
Local file, name them `ic_<filename>` (e.g. `ic_play`, `ic_heart_outline`), and place
them in `res/drawable/`.

---

## Global Rules (apply to every screen)

- Background: solid `background_black` (`#0A0508`) as the base layer on every screen.
  Screens with headers (Now Playing, list screens) add a radial gradient overlay
  (`gradient_purple_top` → `gradient_purple_mid` → transparent) anchored at the top,
  fading out by roughly 55–60% of screen height. Below that point it's flat black.
- Status bar: transparent, icons light (white).
- Horizontal screen padding: 20dp on all content, edge-to-edge on the gradient/background only.
- Icons: 24x24dp default touch icons use 44dp minimum tap target (icon centered inside).
- All icons use `stroke="currentColor"` — tint them via Compose `ColorFilter` per screen
  context (white on dark backgrounds, muted gray for inactive nav states).
- Every screen with a persistent mini-player sits above the bottom nav bar (mini-player
  is NOT part of the nav bar, it's a separate floating element directly above it).

---

## Component: Bottom Navigation Bar

Appears on all main tab screens (Home, Library, Discover, Equalizer/Visualizer, Notifications).

- Floating pill, NOT edge-to-edge: `RoundedCornerShape(32.dp)`, horizontal margin 24dp,
  bottom margin 16dp from screen edge.
- Background: `glass_surface` (white @ 8–10% opacity) with Haze backdrop blur (~20dp) so
  content scrolling underneath visibly blurs through. Add a 1dp `glass_border` (white @
  12–15% opacity) stroke around the pill.
- Height: ~64dp.
- 5 icon slots, evenly spaced, icons only (no text labels):
  1. `home` — Home tab
  2. `library` — Library/Playlists tab
  3. **Active/center slot** — shown as a circular avatar-style button (not a line icon):
     a small circular image (current context image, e.g. current album art or profile
     pic) with a soft radial gradient glow ring around it in `accent_lavender` @ ~40%
     opacity when this tab is active. ~40dp diameter circle.
  4. `waveform` — Equalizer/Visualizer tab
  5. `bell` — Notifications tab
- Inactive icon tint: `text_secondary`. Active (non-center) icon tint: `text_primary`
  with a soft gradient glow behind it, same style as the center slot but smaller.

## Component: Mini-Player

Docked directly above the bottom nav bar when a track is playing, on any screen except
the full Now Playing screen.

- Card: `RoundedCornerShape(20.dp)`, `glass_surface` background + Haze blur (~20dp),
  1dp `glass_border` stroke.
- Height: ~72dp. Horizontal margin matches bottom nav (24dp), sits ~8dp above the nav pill.
- Layout (single row): album art thumbnail (48dp, `RoundedCornerShape(8.dp)`) — 12dp gap
  — track title (`text_primary`, bold, single line, ellipsis) above artist name
  (`text_secondary`, regular, single line) — flexible spacer — `pause`/`play` icon button
  (28dp) — 12dp gap — `skip-next` icon button (28dp), 16dp right padding.
- Tapping the card body (not the buttons) navigates to the full Now Playing screen.

---

## Screen: Now Playing (Full Player)

Reference: left screenshot.

**Top bar** (56dp height, 20dp horizontal padding):
- Left: `chevron-down` icon (24dp) — dismisses back to previous screen.
- Center: a small horizontal pill/drag-handle indicator (28dp wide, 4dp tall,
  `RoundedCornerShape(2.dp)`, white @ 30% opacity) — purely decorative, signals
  swipe-to-dismiss.
- Right: `more-vertical` icon (24dp) — opens overflow menu.

**Track info block** (below top bar, 24dp top margin):
- Track title: large, bold, `text_primary`, left-aligned (e.g. "Skin") — ~28sp.
- Artist name directly below: regular weight, `text_secondary` — ~16sp.
- `heart-outline` icon (24dp, or `heart-filled` if liked) positioned top-right of this
  block, vertically centered against the title.

**Circular album art player** (centered, ~340dp diameter, generous top margin ~48dp):
- Layer 1 (back): two faint, blurred, partially-offscreen circular album art discs
  peeking from behind the left and right edges of the main circle — decorative, suggests
  prev/next tracks in queue. Low opacity (~30–40%), heavy blur.
- Layer 2: outer glass ring — a circle ~20dp larger in diameter than the album art,
  `glass_surface` fill with Haze blur, thin `glass_border` stroke — creates the frosted
  disc/vinyl effect around the art.
- Layer 3: the progress arc — drawn with `Canvas`/`drawArc`, ~4dp stroke width, in
  `accent_blue_progress`, starting from the top (12 o'clock, -90°) and sweeping
  clockwise proportional to playback progress. Track (unplayed portion) in
  `glass_border` color at lower opacity. This arc sits just inside the glass ring edge.
- Layer 4 (front, center): the actual album art image, clipped to `CircleShape`,
  roughly 75% of the outer ring's diameter, centered.
- Elapsed/remaining time label ("3:23") centered directly below the circle, `text_secondary`,
  ~14sp.

**Playback controls row** (centered, ~40dp below the time label):
- 5 icons evenly spaced, horizontally centered as a group (not full-width spread):
  `shuffle` (22dp) — `skip-previous` (28dp) — **play/pause** (large, 56dp circular
  button, white or gradient fill, black icon glyph, elevated/prominent — this is the
  primary action) — `skip-next` (28dp) — `repeat` (22dp).
- Shuffle/repeat icons show an active state (e.g. small dot indicator or tint change to
  `accent_lavender`) when toggled on.

**Bottom:** standard Bottom Navigation Bar component (see above). No mini-player on this
screen (it IS the full player).

---

## Screen: Track List (Playlist / Queue / Library detail)

Reference: right screenshot.

**Top bar:** identical structure to Now Playing top bar (`chevron-down`, drag-handle
pill, `more-vertical`), same 56dp height.

**Track list** (`LazyColumn`, starts ~16dp below top bar):
Each row, ~76dp height, 20dp horizontal padding, tappable (plays that track):
- Left: album art thumbnail, 52dp square, `RoundedCornerShape(8.dp)`.
- 12dp gap, then a column: track title (`text_primary`, bold, ~16sp, single line
  ellipsis) above artist name (`text_secondary`, regular, ~14sp, single line).
- Flexible spacer.
- Right: duration text (`text_secondary`, ~14sp, e.g. "2:56"), right-aligned.
- Divider: 1dp hairline, `divider` color, inset to align with text start (not full width
  — starts after the thumbnail column), only between rows (no divider after the last
  visible row before the mini-player).
- Use `key = { track.id }` and `contentType = "track_row"` on every item — this list is
  the primary scroll-performance target per `kotlin_and_android_instructions.md`.

**Bottom:** Mini-Player component (see above) docked above the Bottom Navigation Bar,
showing the currently playing track — both float above the scrolling list content, list
content should NOT be visible through gaps but the mini-player itself uses the standard
Haze blur so scrolled content blurs through the glass surface itself.

---

## States to Build (apply to Track List and any other list screen)

- **Loading:** skeleton rows (shimmer or static gray blocks matching row layout) instead
  of a spinner, so the layout doesn't jump when real content arrives.
- **Empty:** centered icon + short message + (where relevant) a CTA button, vertically
  centered in the available space, not top-aligned.
- **Error:** short message + retry button, same centered layout as empty state.

---

## Asset Checklist

SVGs expected in `/svg` (root or `/svg` folder — confirm actual path in project):
`play`, `pause`, `skip-next`, `skip-previous`, `shuffle`, `repeat`, `heart-outline`,
`heart-filled`, `chevron-down`, `more-vertical`, `home`, `library`, `search`, `bell`,
`download`, `settings`, `user`, `waveform`.

If a screen in the full app screen list (Search, Settings, Downloads, etc.) needs an
icon not in this set, flag it rather than substituting a default Material icon — we'll
generate a matching one in the same style (24x24, 1.75dp stroke, round caps).
