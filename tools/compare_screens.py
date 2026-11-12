"""Compare captured app content with the supplied references (pip install Pillow).

Run SmokeInstrumentation with `-e screenshots true`, then pull visual-checks as
documented in README.md. System bars are excluded because they belong to Android,
not the app. References are color-managed from Display P3 into sRGB before use.
"""
from io import BytesIO
from pathlib import Path
import json
from PIL import Image, ImageCms, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
PAIRS = {
    "home": "21", "grid": "21 (2)", "drawer": "20 (3)",
    "perfil": "20 (2)", "carnet": "20", "carnet-usuario": "20", "ajustes": "20 (4)",
    "Social": "21 (3)", "Materias": "22 (4)", "Académica": "22 (5)",
    "Recursos": "23 (3)", "Tip's": "24 (6)", "pagos": "24",
    "documentos-0": "22 (6)", "horarios-0": "22 (8)",
    "notas": "22 (9)", "grades": "23 (2)", "contactos-0": "21 (5)",
    "tipos": "23 (8)", "detalle-0": "23 (9)", "ayuda": "25",
}
CARNET_REFERENCES = {
    "carnet": "photo_2026-11-11_22-51-08.jpg",
    "carnet-usuario": "photo_2026-11-11_22-52-14.jpg",
}


def srgb(path):
    image = Image.open(path)
    profile = image.info.get("icc_profile")
    if profile:
        image = ImageCms.profileToProfile(image, ImageCms.ImageCmsProfile(BytesIO(profile)),
                                         ImageCms.createProfile("sRGB"), outputMode="RGB")
    return image.convert("RGB")


def main():
    output = ROOT / "tools" / "comparisons"
    output.mkdir(exist_ok=True)
    thumbnails = []
    captures = ROOT / "tools" / "visual-checks"
    bounds = json.loads((captures / "bounds.json").read_text(encoding="utf-8"))
    for screen, suffix in PAIRS.items():
        capture = captures / f"{screen}.png"
        if not capture.exists():
            continue
        reference_path = (ROOT / "recursos" / "CARNET VIRTUAL" / CARNET_REFERENCES[screen]
                          if screen in CARNET_REFERENCES else
                          ROOT / "recursos" / f"photo_2026-11-11_06-55-{suffix}.jpg")
        reference = srgb(reference_path)
        actual = srgb(capture)
        # Read the real app viewport: status/cutout and navigation insets differ.
        actual = actual.crop(tuple(bounds[screen] if screen in bounds else bounds[screen.replace("-", ":")]))
        actual = actual.resize((reference.width, round(actual.height * reference.width / actual.width)), Image.Resampling.LANCZOS)
        reference = reference.crop((0, 58, reference.width, 1263))
        pair = Image.new("RGB", (reference.width * 2, max(reference.height, actual.height) + 24), "#252525")
        pair.paste(reference, (0, 24))
        pair.paste(actual, (reference.width, 24))
        ImageDraw.Draw(pair).text((10, 5), f"{screen}: referencia / proyecto", fill="white")
        pair.save(output / f"{screen}.jpg", quality=93)
        pair.thumbnail((294, 330))
        thumbnails.append(pair.copy())
    sheet = Image.new("RGB", (294 * 4, 330 * ((len(thumbnails) + 3) // 4)), "#303030")
    for index, image in enumerate(thumbnails):
        sheet.paste(image, ((index % 4) * 294, (index // 4) * 330))
    sheet.save(output / "overview.jpg", quality=94)
    print(f"{len(thumbnails)} comparisons written to {output}")


if __name__ == "__main__":
    main()
