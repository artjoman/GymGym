# -*- coding: utf-8 -*-
"""Generate the 20 GymGym achievement badges with Nano Banana.

One shared medal frame + one caricature gag per achievement, all anchored to the
shipped launcher icon so Reppo stays on-model. Calls the API with curl (this
Python has no CA certs) through `zsh -lic` so the
GEMINI_API_KEY from ~/.zshrc is in scope. Masters land in masters/badges/;
run fit_badges.py afterwards to crop and downscale them into res/.
"""
import base64
import json
import os
import subprocess
import sys
import tempfile

S = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(S))
MASTERS = os.path.join(S, "masters", "badges")

# The medal frame, byte-for-byte identical on every badge so the set reads as one
# system. The art is full-bleed: the app clips it to a circle, so the corners are
# discarded and the rim must run right to the edges.
FRAME = """A circular ACHIEVEMENT MEDAL for a fitness app — a collectible prize badge, drawn in the EXACT character style of the attached reference image.

BADGE FORMAT — identical on every badge in this set. The medal is a perfect CIRCLE that FILLS THE ENTIRE SQUARE FRAME edge to edge: the outer rim touches all four edges of the image. Do NOT inset, shrink, float or centre the coin on a background — it is full-bleed. Structure from the outside in: a thick solid black #070709 outer rim ring; then a thin brand-yellow #FFBE0A gap ring; then a second thinner black ring; then the flat brand-yellow #FFBE0A medal face filling the whole centre. The four corners outside the circle may be any flat colour — they are discarded.

SUBJECT on the medal face: {pose}

SCALE — the character is drawn LARGE and fills most of the medal face, and must stay clearly readable when the whole image is shrunk to thumbnail size. Keep the entire subject inside the inner ring; never let the rim cut through the head. Favour a big bold silhouette over small detail.

TONE — CARICATURE. Comic, exaggerated and funny, like a cartoon trophy sticker: push the expression and the proportions well past realistic, with oversized features, squash-and-stretch, comic sweat droplets and motion lines. It should make someone smile while still reading as a proper earned prize.

CHARACTER "Reppo" — the muscular stylized cartoon man from the reference. His face must match the reference exactly: BALD with a short dark buzzed hairline drawn as fine halftone dots, thick dark eyebrows, a short well-groomed beard and moustache. Skin lit #FDD28E with a single flat shadow tone #D3AC71 on the beard and jaw. Black athletic tank top and black training shorts, solid #070709, no pattern or logo; bare arms and legs. Chunky black dumbbells with rounded plates. His expression changes with the gag — he does NOT have to wink here.

STYLE — bold flat vector cartoon; thick uniform black outlines of even weight; hard-edged 2-tone flat colour fills; absolutely NO gradients, NO airbrush, NO soft shadows, NO glow, NO drop shadow, NO 3D, NO photorealism; fine halftone dots ONLY on the buzzed scalp; poster-bold and high contrast.

PALETTE — use only: outline/iron/black #070709, skin lit #FDD28E, skin shadow #D3AC71, brand yellow #FFBE0A, and white #FFFFFF for small highlights.

Draw NO text, NO letters, NO numbers, NO words, NO watermark and NO signature anywhere in the image.

Avoid: photorealism, 3D render, gradients, soft shading, airbrush, drop shadow, glow, bloom, neon rim light, anime styling, magenta hair, green tank top, headband, busy background, background pattern, extra characters, extra limbs, deformed hands, cropped head, misspelled text, gibberish text, watermark, signature, UI mockup, phone frame, square border, rounded corners, coin floating on a background, inset circle, drop-shadowed sticker."""

BADGES = [
    ("first_workout",
     "Reppo curling ONE single tiny dumbbell with both hands, comically overexerted — face screwed up and straining, tongue out of the corner of his mouth, eyes bulging, big comic sweat droplets flying off, whole body trembling with effort. The joke is that the dumbbell is absurdly small and he is treating it like a world record."),
    ("workouts_10",
     "Reppo cracking his knuckles with a deadly serious, comically over-furrowed scowl, eyes narrowed to slits, chin down. He has just torn the sleeves clean off his tank top and the two torn scraps of black fabric are still falling through the air beside him. A short stack of black weight plates sits at his side."),
    ("workouts_100",
     "Reppo as a Roman centurion, wearing an absurdly oversized black gladiator helmet with a huge bristly crest that is far too big for his head and tips slightly forward over his eyes. He holds a long dumbbell upright like a spear in one hand and a round black weight plate like a shield in the other, chest puffed out, chin up, ridiculously proud."),
    ("workouts_1000",
     "Reppo as a heroic monument statue: he stands on a chunky pedestal in a triumphant double-bicep flex, rendered as a solid brand-yellow statue with thick black outlines, perfectly still and noble — and one small black cartoon pigeon is perched on top of his bald head, completely ruining the dignity."),
    ("streak_3",
     "Reppo balanced on top of an enormous dumbbell that is rolling forward like a log, running frantically on the spot to stay upright, arms windmilling, eyes wide, huge delighted grin, speed lines and little dust puffs behind him."),
    ("streak_7",
     "Reppo juggling SEVEN small black dumbbells in a big arc above his head, arms a blur, sweating heavily, eyes darting upward in mild panic while still grinning — barely holding it together but absolutely not stopping."),
    ("streak_30",
     "Reppo flexing hugely while a heavy barbell laid across his shoulders BENDS like rubber into a deep droopy U around him, and a thick black chain wrapped around his chest snaps apart, the broken links flying outward. Utterly unbothered, smug half-smile."),
    ("reps_1000",
     "Reppo doing bicep curls so fast that his arm has become a fan of MANY overlapping ghosted arms sweeping through the curl arc, with bold motion arcs and speed lines. His eyes have gone into dizzy spirals and his tongue lolls out, but he is still grinning."),
    ("reps_10000",
     "Reppo as a clockwork wind-up toy: a big black wind-up key sticks out of his back and is mid-turn, his pose is stiff and mechanical mid-curl, his arms have visible bolt joints at the elbows, and his eyes are simple blank circles staring straight ahead. Deadpan and tireless."),
    ("legday_3",
     "Reppo trying to walk DOWN a short flight of steps the day after leg day: his legs have gone to jelly and wobble in rubbery curves beneath him, both hands death-gripping a handrail, face contorted in comic agony, sweat droplets flying, one foot hovering nervously over the next step."),
    ("legday_25",
     "Reppo in a deep squat under a barbell loaded with absurdly enormous black weight plates that nearly fill the frame — and his thighs and calves are gigantic tree-trunk pillars, far too big for the rest of him. Completely calm, smug half-smile, not straining in the slightest."),
    ("full_body",
     "Reppo in a most-muscular 'crab' pose with EVERY muscle flexed to a ridiculous degree at once — chest, arms, neck, calves all bulging into absurd rounded lumps, veins popping, face squeezed tight and red with effort, teeth gritted, sweat spraying off him in all directions."),
    ("perfect_1",
     "Reppo holding a textbook-perfect squat, spine straight, absolutely immaculate posture, with a small spirit level balanced across his shoulders showing its bubble dead centre — and a little halo hovering above his bald head. His expression is serene and angelic, eyes closed, faintly smug."),
    ("perfect_10",
     "Reppo peering through a monocle at his own flexed bicep while measuring it with a tape measure, one eyebrow arched extremely high, lips pursed in prim, fussy concentration, pinky finger daintily extended. Insufferably precise."),
    ("goodreps_500",
     "Reppo mid-squat with a thick open book balanced flat on top of his bald head, posture ramrod straight and utterly by-the-book, chin level, eyes forward, a prim satisfied little smile. A model student of form."),
    ("cycle_1",
     "Reppo bursting through a finish-line ribbon that snaps and streams backwards past him, both arms thrown up in triumph, mouth wide open in a roaring cheer, eyes squeezed shut with joy, one dumbbell still clutched in a raised fist. The ribbon is SOLID BLACK #070709 with white #FFFFFF stripes — it must NOT be red or any other colour."),
    ("cycle_10",
     "Reppo lounging back on a throne built from stacked black weight plates, wearing a comically small crown perched on his bald head, holding a dumbbell upright like a royal sceptre, one leg draped over the armrest, supremely smug half-smile."),
    ("early_bird",
     "Reppo barely awake at dawn: heavy drooping eyelids, one eye stuck shut, mouth open in an enormous yawn, clutching a steaming mug in one hand and letting a dumbbell dangle limply from the other. A small black cartoon bird perches on his shoulder chirping at him, and a half sun with straight rays rises behind his shoulders."),
    ("night_owl",
     "Reppo lifting late at night, wearing a floppy pointed nightcap that flops over one ear, caught mid-yawn during a dumbbell curl with his eyes watering. A small round black owl sits on the end of his dumbbell staring at him, with a crescent moon and a few four-point stars behind."),
    ("measure_7",
     "Reppo flexing an enormous bicep and trying to wrap a tape measure around it — the tape is comically far too short to reach all the way round, its two ends waving uselessly in the air. He is beaming with insufferable pride, chest out, eyes closed in delight."),
]


ICON = os.path.join(ROOT, "store-assets", "play-icon-512.png")
MODEL = "gemini-3-pro-image"
URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent" % MODEL


def generate(prompt, out):
    """POST the prompt + the launcher icon as a reference, save the PNG.

    curl does the HTTPS because this Python has no CA certificates, and it runs
    under `zsh -lic` because GEMINI_API_KEY is exported from ~/.zshrc, which only
    runs for interactive shells (`-lc` alone 403s as an unregistered caller).
    """
    body = {
        "contents": [{"parts": [
            {"text": prompt},
            {"inline_data": {
                "mime_type": "image/png",
                "data": base64.b64encode(open(ICON, "rb").read()).decode(),
            }},
        ]}],
        "generationConfig": {
            "responseModalities": ["IMAGE"],
            "imageConfig": {"aspectRatio": "1:1", "imageSize": "2K"},
        },
    }
    body_path = os.path.join(tempfile.gettempdir(), "gg_badge_body.json")
    resp_path = os.path.join(tempfile.gettempdir(), "gg_badge_resp.json")
    json.dump(body, open(body_path, "w"))
    subprocess.run(
        ["zsh", "-lic",
         'curl -s -m 300 "%s" -H "Content-Type: application/json" '
         '-H "x-goog-api-key: $GEMINI_API_KEY" -d @%s -o %s'
         % (URL, body_path, resp_path)],
        capture_output=True, text=True,
    )
    data = json.load(open(resp_path))
    if "error" in data:
        return "API error: %s" % data["error"].get("message", "")[:200]
    cands = data.get("candidates", [])
    if not cands:
        return "no candidates"
    for part in cands[0].get("content", {}).get("parts", []):
        blob = part.get("inlineData") or part.get("inline_data")
        if blob:
            open(out, "wb").write(base64.b64decode(blob["data"]))
            return "saved"
    return "no image; finishReason=%s" % cands[0].get("finishReason")


def main():
    only = sys.argv[1:] if len(sys.argv) > 1 else None
    os.makedirs(MASTERS, exist_ok=True)
    for name, pose in BADGES:
        if only and name not in only:
            continue
        out = os.path.join(MASTERS, "badge_%s.png" % name)
        if os.path.exists(out) and not only:
            print("have    %s" % name)
            continue
        print("generating %s ..." % name, flush=True)
        print("   %s" % generate(FRAME.format(pose=pose), out))


if __name__ == "__main__":
    main()
