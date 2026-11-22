# Power Band (Caso #3)

Programa de creación musical: tres dispositivos físicos tocan tres sintetizadores de SuperCollider, y un editor musical monitorea, graba, carga y reproduce la música.

| Dispositivo | Synth | Cómo se toca |
|---|---|---|
| Joystick / control USB | `organ70s` | Botones 1 a 8 = notas de la escala de Do mayor (presionar = suena, soltar = calla). Ejes X, Y, Z = timbre (parciales del órgano). |
| Tableta gráfica Huion | `saxophone` | X selecciona uno de los 12 semitonos, Y una de dos octavas (MIDI 48–71), y la presión controla el volumen. Levantar el lápiz termina la nota. |
| Micrófono predeterminado del sistema | `violin` | El pitch detectado selecciona la nota y el nivel RMS controla el volumen; silencio termina la nota. |

`classicPiano` y `stringBass` permanecen como synths legacy para reproducir grabaciones previas. El adaptador `TabletaGrafica` usa JNA para leer Windows Pointer API y entregar coordenadas, presión real y estado del lápiz a consumidores Swing. No usa eventos de mouse para inferir presión ni requiere código JNI/C++ propio.

## Prerrequisitos

| Herramienta | Versión | Verificar |
|---|---|---|
| JDK | 25 o superior (LTS) | `java -version` |
| Apache Maven | 3.9 o superior | `mvn -version` |
| SuperCollider | 3.13 o superior | https://supercollider.github.io/downloads.html |
| Sistema | Windows (el joystick usa `winmm.dll`) | |

Las dependencias JNA se resuelven desde Maven; no se necesita instalar CMake ni un compilador C++.
Un diagnóstico previo en Windows 11 confirmó que la Kamvas 13 GS1331 y el controlador HUION HID 6.1.7600.16905 exponen eventos `PT_PEN` y presión variable. La integración física del hook JNA y la validación en Windows 10 siguen pendientes antes de distribuir el adaptador.

## Correr

1. Abra `supercollider/myband.sc` en SuperCollider, ponga el cursor dentro del bloque grande `( ... )` y presione **Ctrl+Enter**. Luego evalúe la línea `~myband.load;` (Ctrl+Enter). En la consola debe salir `myband: classicPiano, stringBass, saxophone, violin, organ70s loaded`.
2. Conecte el joystick.
3. En la carpeta del proyecto (donde está `pom.xml`):

```powershell
mvn compile exec:java
```

Si SuperCollider no está arriba, el programa avisa y sigue abierto. Si un dispositivo no está, su línea dice `no disponible: ...` y los otros dos siguen funcionando.

## Pruebas

```powershell
mvn test
```

Las pruebas no necesitan SuperCollider ni los dispositivos (usan un transporte OSC falso y carpetas temporales).

## Archivos de datos (`data/`)

| Archivo | Contenido |
|---|---|
| `recordings.music` | Registros binarios de 96 bytes: encabezado (nombre, inicio, fin, fecha) + comandos de cada grabación. |
| `recordings.index` | Registros binarios de 72 bytes (nombre + posición del encabezado), ordenados por nombre con Balanced Merge Sort. Se busca con búsqueda binaria en disco. |
| `recordings.list` | Texto `nombre|fecha`, en orden cronológico. |

## Desarrollo con IA (spec driven development)

- `CLAUDE.md` y `.claude/rules/`: stack y reglas del proyecto.
- `.claude/skills/generate-feature-spec`: genera los specs de una feature en `specs/<capa>/ft-N-*.md` (status `draft`).
- `.claude/skills/develop-feature`: implementa un `ft-N` cuando todos sus specs están `approved`.
- `.claude/skills/update-current-development`: reescribe los mapas `.claude/skills/<capa>/references/map.md`.

## Estructura

```
src/main/java/powerband/
├── Main.java          # arranque
├── instrument/        # Bridge: abstracción (Instrument, Joystick, Huion, Micrófono)
├── synth/             # Bridge: implementación (SynthCommand, Saxophone, Violin, Organ)
├── osc/               # cliente OSC/UDP hacia scsynth
├── service/           # editor: grabar, cargar, reproducir
├── repository/        # archivos binarios, Balanced Merge Sort, búsqueda binaria
├── dto/               # datos entre capas
├── exception/         # excepciones propias
├── ui/                # ventana Swing
└── util/IConstants.java
```
