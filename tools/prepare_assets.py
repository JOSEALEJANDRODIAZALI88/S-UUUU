"""Reproduce the bundled artwork from the user-supplied reference APK.

Only explicitly selected images/fonts are copied, never code or APK configuration.
Requires Pillow for the portrait crop; does not change user data or Gradle files.
"""
from io import BytesIO
from pathlib import Path
from zipfile import ZipFile
from PIL import Image, ImageCms

root = Path(__file__).resolve().parents[1]
main = root / 'app/src/main'
assets = main / 'assets'
resources = main / 'res'

def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)

with ZipFile(root / 'recursos/SIU_1.82_APKPure.apk') as apk:
    for font in ('FontAwesomeSolid.otf', 'OpenSans-Regular.ttf', 'OpenSans-Semibold.ttf'):
        write(assets / 'fonts' / font, apk.read('assets/' + font))
    artwork = ('bandera_bolivia', 'logo', 'logo_actual_small', 'logo_carnet',
               'logo_untext', 'logo_varios', 'materia_mensaje',
               'mensaje_abierto', 'mensaje_cerrado', 'mensaje_pendiente',
               'univalle_login_imagen', 'univalle_login_imagen_oscuro',
               'univalle_papel', 'usuario')
    for name in artwork:
        write(assets / 'artwork' / (name + '.png'), apk.read('res/drawable/' + name + '.png'))
    for density in ('mdpi', 'hdpi', 'xhdpi', 'xxhdpi', 'xxxhdpi'):
        for name in ('icon_social', 'icon_materias', 'icon_academica', 'icon_recursos', 'icon_tips'):
            write(resources / ('drawable-' + density) / (name + '.png'),
                  apk.read('res/drawable-' + density + '-v4/' + name + '.png'))
        for name in ('appicon', 'appicon_background', 'appicon_foreground'):
            write(resources / ('mipmap-' + density) / (name + '.png'),
                  apk.read('res/mipmap-' + density + '-v4/' + name + '.png'))

# The APK has no personal photo. Keep the portrait from the supplied reference.
photo = Image.open(root / 'recursos/photo_2026-11-11_06-55-20 (2).jpg')
photo.crop((34, 177, 182, 325)).save(assets / 'portrait.jpg', quality=95)
# Preserve the distinct framing of the newer virtual-card reference.
card_photo = Image.open(root / 'recursos/CARNET VIRTUAL/photo_2026-11-11_22-51-08.jpg')
profile = card_photo.info.get('icc_profile')
if profile:
    card_photo = ImageCms.profileToProfile(card_photo, ImageCms.ImageCmsProfile(BytesIO(profile)),
                                          ImageCms.createProfile('sRGB'), outputMode='RGB')
card_photo.crop((206, 235, 384, 413)).save(assets / 'portrait_carnet.png')
print('Original artwork, launcher layers, fonts and reference portraits prepared.')
