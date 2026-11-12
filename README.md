# SIU Local - proyecto Android Studio

Aplicación Android nativa en **Java**, basada en las capturas y la imagen del icono de `recursos/`, incluidas las dos vistas de `CARNET VIRTUAL/`. Integra imágenes, iconos y la fuente de símbolos del APK original proporcionado. Funciona sin servidor y conserva los datos e imágenes en el almacenamiento privado del teléfono.

## Abrir y ejecutar

1. En Android Studio, selecciona **Open** y abre esta carpeta: `C:\Users\DIAZ\Desktop\siu`.
2. Deja que termine la sincronización de Gradle.
3. Selecciona el módulo **app** y pulsa **Run** con un emulador o un teléfono conectado.

Requisitos: JDK 21 o superior, Android SDK 36 y Android 8.0 (API 26) o posterior en el dispositivo. Se incluyen Gradle Wrapper 9.5.0 y Android Gradle Plugin 9.3.2. `local.properties` contiene la ruta del SDK de esta máquina; Android Studio puede regenerarlo en otra máquina.

**APK:** `app/build/outputs/apk/debug/app-debug.apk`.

## Cambiar los datos, fotos y QR

Abre **☰ -> Ajustes -> ⋮ (Herramientas locales) -> Editar contenido**. El botón del lápiz del perfil abre directamente sus campos.

- **Perfil, foto y QR:** datos personales, foto de galería, contenido del QR generado o imagen QR propia. Si eliges una imagen QR, se muestra esa imagen; al quitarla vuelve a generarse el QR desde el texto.
- **Materias:** nombres, grupos, docentes, imagen y calificaciones.
- **Horarios:** día, horas, aula, grupo, docente y materia. Usa los nombres de los días que aparecen en las pestañas.
- **Documentos:** cantidad, fechas, tipo y estado pendiente/entregado.
- **Pagos / Económico:** cuotas, importes, vencimientos, estados y aviso del plan de pagos.
- **Contactos:** sedes y enlaces. El campo ciudad debe coincidir con Cochabamba, La Paz, Sucre, Trinidad o Santa Cruz.
- **Tipos de trámites, libros, descuentos, bancos, cajas y ayuda:** contenido de sus respectivas pantallas.
- **Imágenes de la aplicación:** sello y marca de agua.

Los campos se guardan automáticamente al editarlos. Las listas permiten añadir, abrir y eliminar registros. El editor es una pantalla independiente para preservar el diseño de las vistas principales.

**Ver el QR:** abre **Carnet virtual**. La pestaña **Carnet** muestra el QR dentro de la tarjeta, con foto circular, nombre, carrera, sede y botón para actualizarlo desde los datos locales. La pestaña **Usuario** muestra los datos personales y el estado del lector. El diseño sigue las dos referencias de `recursos/CARNET VIRTUAL/`; la pestaña seleccionada se conserva al girar el teléfono. El QR también está disponible desde el editor del perfil. Actualizarlo no solicita ni valida una credencial institucional.

## Copias de seguridad

En **Ajustes -> ⋮ (Herramientas locales)** puedes **exportar** o **importar** un ZIP con los datos y las imágenes. Usa el selector de archivos de Android para elegir dónde guardarlo. Importar reemplaza el contenido actual; exporta antes si quieres conservarlo. Al desinstalar la aplicación se eliminan sus datos privados, por lo que conviene exportar una copia.

## Comportamiento local

Las consultas, sugerencias y participaciones se guardan en el teléfono. El restablecimiento de contraseña cambia solo un dato de demostración. El QR es generado o importado localmente y no tiene validación institucional. Los enlaces de mapas, correo, teléfono y Outlook abren la aplicación externa correspondiente.

Las fotos determinan la apariencia y los datos iniciales disponibles. Las pantallas vacías de biblioteca, descuentos, bancos o trámites pueden llenarse desde el editor. El estilo inicial es oscuro; en Ajustes se pueden seleccionar Claro, Oscuro o Sistema. Los colores y las dimensiones del diseño se definen en el código, separados del contenido editable.

## Compilar y verificar

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
adb install -r "app/build/outputs/apk/debug/app-debug.apk"
adb install -r "app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
adb shell am instrument -w com.siu.local.test/com.siu.local.SmokeInstrumentation
```

La instrumentación comprueba las pantallas, navegación por menú y perfil, cambios lista/cuadrícula y tema, persistencia de perfil y notas, importación de imágenes, copia ZIP con imágenes, rechazo de rutas no válidas, recursos originales, icono adaptable y lectura del QR realmente mostrado en el carnet. También verifica ambas pestañas, selección guardada, actualización, imagen QR personalizada y errores de contenido vacío o demasiado largo. Usa un directorio de pruebas independiente del contenido del usuario.

### Recursos y comparación visual

- El icono adaptable conserva las capas originales por densidad. Las tarjetas de la cuadrícula usan sus PNG originales; la lista y las barras usan los símbolos de `FontAwesomeSolid.otf` del APK.
- Los sellos y las banderas proceden del APK, sin ampliaciones de recortes de pantalla. La foto personal procede de las capturas aportadas, con el encuadre de la referencia correspondiente en perfil y carnet; los datos y las imágenes personalizadas existentes tienen prioridad.
- `python tools/prepare_assets.py` reproduce los recursos seleccionados desde `recursos/SIU_1.82_APKPure.apk`. Requiere Python y Pillow (`python -m pip install Pillow`). No copia código del APK ni modifica los datos guardados.
- El APK original se abrió en el emulador: solicita autenticación. Sin credenciales, la comparación de las pantallas internas se realiza contra las capturas proporcionadas, no contra una sesión institucional.
- Las barras del sistema y algunos controles nativos pueden variar entre versiones y fabricantes de Android; no se garantiza una coincidencia píxel por píxel.

Para generar capturas de 21 vistas (incluidas ambas pestañas del carnet) con los datos de referencia **solo dentro de las pruebas**:

```powershell
adb shell am instrument -w -e screenshots true com.siu.local.test/com.siu.local.SmokeInstrumentation
adb pull /sdcard/Android/data/com.siu.local/files/visual-checks tools
python tools/compare_screens.py
```

Las capturas quedan en `tools/visual-checks/` y las comparaciones en `tools/comparisons/overview.jpg` y archivos individuales. La herramienta convierte las referencias Display P3 a sRGB y excluye las barras del sistema usando los límites reales de la ventana. No estira verticalmente las capturas; las diferencias de altura se muestran en gris. Las previsualizaciones antiguas `tools/preview-*.png` no son el resultado actualizado.

### Archivos principales

- `app/src/main/java/com/siu/local/MainActivity.java`: pantallas, navegación y editor.
- `app/src/main/java/com/siu/local/LocalStore.java`: persistencia, imágenes y copias.
- `app/src/main/java/com/siu/local/SiuIcon.java`: símbolos de la fuente original y flecha de navegación Android.
- `app/src/main/assets/artwork/`, `app/src/main/assets/fonts/`, `app/src/main/res/mipmap-*/`: recursos originales seleccionados.
- `app/src/main/assets/defaults.json`: contenido inicial.
- `app/src/androidTest/java/com/siu/local/SmokeInstrumentation.java`: verificaciones en dispositivo.
