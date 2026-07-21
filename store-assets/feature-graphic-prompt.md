# Feature graphic — Nano Banana prompt

Run this in **Nano Banana (Gemini 2.5 Flash Image)**. It cannot be called from
this repo, so generate externally and drop the result back into `store-assets/`.

**Attach `play-icon-512.png` as a reference image** — the model holds the
character far steadier when anchored to a reference than from text alone.

## Palette (sampled from the icon — do not drift)

| Role | Hex |
| --- | --- |
| Background yellow | `#FFBE0A` |
| Skin, lit | `#FDD28E` |
| Skin/beard, shadow | `#D3AC71` |
| Outline, hair, iron | `#070709` |

## Aspect ratio

Play needs exactly **1024 × 500** (2.048:1). Generate at the nearest supported
wide ratio (**16:9**, or 21:9 if offered) and leave generous empty yellow at the
top and bottom — the final crop to 1024×500 is done afterwards. Do **not** place
anything important within ~8% of any edge.

---

## PROMPT

> A flat vector illustration banner for a fitness app, in the exact style of the
> attached reference image — the same character, the same line work, the same
> palette.
>
> **CHARACTER — full body, derived from the reference head.** The reference shows
> only the head; draw the SAME man at **full body, about 6 heads tall**,
> athletic and muscular but stylized, not realistic. His face must match the
> reference exactly: bald with a short dark buzzed hairline drawn as a fine
> halftone dot texture, thick dark eyebrows, a confident **wink** (his left eye
> open, right eye winking), a short well-groomed beard and moustache, and a
> smug half-smile. Skin lit `#FDD28E` with flat `#D3AC71` shadow on the beard
> and jaw. No other shading.
>
> **POSE — mid-exercise, dynamic.** He is performing a **dumbbell bicep curl**,
> caught at the top of the rep: standing three-quarter view facing slightly
> left, feet planted shoulder-width, one heavy black dumbbell curled up to
> shoulder height with the bicep flexed and visibly bulging, the other arm down
> at his side holding a second dumbbell. Chest up, shoulders back, confident and
> powerful. Legs and both feet fully visible — do not crop the body.
>
> **OUTFIT.** Simple black athletic tank top and black training shorts, drawn as
> solid `#070709` shapes with no pattern or logo. Bare arms and legs so the
> muscle definition reads. Dumbbells solid `#070709` with the same rounded,
> chunky plates as the reference.
>
> **STYLE — match the reference exactly.** Flat vector cartoon, thick uniform
> black outlines of even weight, hard-edged 2-tone flat colour fills, absolutely
> no gradients, no airbrushing, no photorealism, no 3D, no drop shadows, no
> texture beyond the halftone dots on the scalp. Clean, bold, poster-like.
>
> **COMPOSITION — wide horizontal banner.** Flat solid `#FFBE0A` yellow
> background across the whole image, no vignette and no pattern. The character
> stands in the **left third**, full body, feet near the lower edge but not
> touching it. The **right two-thirds** holds the text, left-aligned and
> vertically centred, with clear yellow space around it:
>
> - Line 1, largest, heavy extra-bold condensed sans-serif, solid `#070709`:
>   **GymGym**
> - Line 2, about half that size, bold sans-serif, solid `#070709`:
>   **AI Rep Counter**
> - Line 3, smaller, bold italic sans-serif, solid `#070709`, with a short thick
>   black underline bar beneath it: **EVERY REP COUNTS.**
>
> Spell the text **exactly** as written, correctly and legibly, with clean
> even letter spacing. No other words, numbers, watermarks or signatures
> anywhere in the image.

## NEGATIVE PROMPT

> photorealistic, 3D render, gradients, soft shading, airbrush, drop shadow,
> glow, bloom, busy background, gym background, background pattern, extra
> characters, extra limbs, deformed hands, cropped head, cropped feet,
> misspelled text, gibberish text, lorem ipsum, watermark, signature, logo,
> UI mockup, phone frame, border, frame, rounded corners

---

## After generating

1. Save the raw output as `store-assets/feature-raw.png`.
2. Crop/resize to exactly 1024×500 (ask Claude — there is a `sips` + pure-Python
   PNG pipeline in this repo, since the machine has no PIL or ImageMagick).
3. Check the text is spelled correctly and is legible at thumbnail size. Image
   models still garble small type; regenerate rather than shipping a typo.

## If the text comes out wrong

Generate the **artwork only** — same prompt with the whole COMPOSITION text
block replaced by *"the right two-thirds is empty flat `#FFBE0A` yellow, no text
anywhere"* — and have the title/quote composited on afterwards as real vector
text. That is the reliable route for crisp type, and Play renders the feature
graphic small enough that fuzzy lettering is obvious.

## Alternate quotes

- `EVERY REP COUNTS.` (recommended — doubles as a product claim)
- `NO REP LEFT BEHIND.`
- `TRAIN HARD. COUNT SMART.`
- `LET THE AI COUNT. YOU JUST LIFT.`

---

## Note on the documented mascot

`.claude/agents/asset-designer.md` defines the canonical mascot as **"Reppo"** —
neon magenta spiky hair, brand-green tank top, on a near-black background, with
green `#7FE0A0` as the primary. The icon adopted here is a different character on
yellow, and the app's default accent is now Amber. **That spec is stale.** This
prompt intentionally follows the shipped icon, not Reppo. Worth reconciling the
agent spec before more art is generated, so the two don't diverge further.
