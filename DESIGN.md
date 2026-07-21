# Pi Java Agent Console Design System

## 1. Atmosphere & Identity

The Console is an Amp-inspired dark operational surface: a deep green-black field, quiet technical grid, warm orange action signal, editorial display type, and restrained mono metadata. The signature is the single centered conversation column, where the feed reads as the operational record and the composer remains physically attached to it.

## 2. Color

| Role | Token | Value | Usage |
|---|---|---:|---|
| Canvas | `--pi-amp-bg` | `#091c1e` | Console background |
| Deep canvas | `--pi-amp-bg-deep` | `#061315` | Feed and composer depth |
| Foreground | `--pi-amp-fg` | `#f6fff5` | Primary content |
| Quiet text | `--pi-amp-muted` | `#978e81` | Labels and secondary content |
| Operational text | `--pi-amp-muted-green` | `#78918b` | Run and provider metadata |
| Action | `--pi-amp-accent` | `#f6833b` | Primary action and user message |
| Action wash | `--pi-amp-accent-soft` | `rgba(246, 131, 59, 0.15)` | Status and selected states |
| Structural line | `--pi-amp-line` | `rgba(125, 102, 91, 0.62)` | Strong decorative line |
| Soft line | `--pi-amp-line-soft` | `rgba(125, 102, 91, 0.24)` | Cards and dividers |
| Surface | `--pi-amp-surface` | `rgba(246, 255, 245, 0.055)` | Resting translucent surface |
| Raised surface | `--pi-amp-surface-strong` | `rgba(246, 255, 245, 0.105)` | Emphasized translucent surface |

Use the warm accent only for user intent, primary actions, and consequential state. Console depth uses tinted gradients, soft lines, and directional shadows rather than flat blocks.

## 3. Typography

| Level | Token / scale | Usage |
|---|---|---|
| Display | `clamp(2.75rem, 5.5vw, 5rem)` | Conversation hero |
| Page title | `clamp(1.45rem, 2.4vw, 2.4rem)` | Shell title |
| Lead | `clamp(1.05rem, 1.5vw, 1.55rem)` | Hero supporting copy |
| Body | `0.95rem` to `1rem` | Messages and controls |
| Metadata | `0.8rem` | Status and provider details |
| Overline | `0.72rem` | Provider label |

- Display: `SagittaireAsset, "Cormorant Garamond", Georgia, serif`.
- Body: `Avenir Next, Manrope, "Helvetica Neue", sans-serif`.
- Metadata: `Berkeley Mono, "IBM Plex Mono", "JetBrains Mono", ui-monospace, monospace`.

## 4. Spacing & Layout

- Base spacing uses the existing 4px-derived `--pi-mobile-space-*` scale: 4px, 8px, and 16px are the compact, standard, and section increments.
- `.pi-console-workbench.pi-console-home` is the only Console width owner. It defines one `minmax(0, 58rem)` column and centers it at every viewport.
- Every direct Console child has `width: 100%` and `min-width: 0`; children never independently claim a Console max width.
- Breakpoints: mobile is `<=640px`; tablet is `641px-899px`; desktop is `>=900px`. The Console remains one column across all three.
- `.pi-console-chat` is a two-row grid: a constrained event-feed row followed by the composer row. `.pi-console-event-feed` is the only vertical scroll owner.

## 5. Components

### Console Shell
- **Structure**: hero, provider bar, active-session banner, chat panel.
- **States**: new and continued session; provider ready and blocked.
- **Accessibility**: all route, role, action, session, run, stream, and message data hooks remain stable; direct children must not overflow horizontally.

### Chat Feed and Composer
- **Structure**: `event-feed` followed by `chat-composer` inside `.pi-console-chat`.
- **Variants**: empty feed, user/assistant primary bubbles, secondary runtime/tool/approval cards, loading assistant, terminal assistant.
- **States**: pending, streaming, completed, failed, cancelled, partial; loading dots communicate pending assistant work only.
- **Accessibility**: the text area stays at two to six rows; send and cancel retain keyboard-capable Vaadin controls and stable action hooks.

## 6. Motion & Interaction

- Navigation uses a 180ms transform/color/background transition to signal a selectable destination.
- Assistant loading dots use a 960ms opacity and translate animation. The animation is disabled by the existing `prefers-reduced-motion` rule.
- No layout property is animated.

## 7. Depth & Surface

The Console uses a mixed strategy: translucent tonal shifts and soft structural borders establish hierarchy; tinted, low-opacity shadows establish the elevation of the model bar and chat shell. The feed keeps a subdued grid texture so dense event history remains readable without introducing a second panel.

## 8. Accessibility Constraints & Accepted Debt

### Constraints

- Target WCAG 2.2 AA contrast, visible keyboard focus, minimum 44px touch target for actionable controls, and no horizontal overflow at 375px, 768px, or 1280px.
- Preserve the chat-only mounted DOM. Session, catalog, and run-context objects may remain service collaborators but must not be mounted in the Console tree.
- Preserve all existing `data-route`, `data-layout`, `data-role`, `data-action`, `data-message-*`, `data-stream-*`, `data-session-*`, `data-run-*`, and `data-step-*` selectors.

### Accepted Debt

| Item | Location | Why accepted | Owner / Exit |
|---|---|---|---|
| External display-font request | `styles.css` `@font-face` | Existing Amp-inspired direction already depends on the hosted font with `font-display: swap`; system fallbacks preserve readable content. | Replace with a locally hosted licensed asset when asset policy is defined. |
| Broader legacy Admin token set | `styles.css` outside Console selectors | This refactor intentionally avoids unrelated Admin restyling. | Consolidate in a separately approved Admin design-system pass. |
