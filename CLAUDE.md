# Project description
Power Band (Caso #3, POO) es un programa de creación e interacción musical. Java invoca, por OSC sobre UDP, tres sintetizadores activos de SuperCollider (`supercollider/myband.sc`): `saxophone`, `violin` y `organ70s`. `classicPiano` y `stringBass` se conservan únicamente para reproducir grabaciones antiguas.

El programa tiene tres partes:
1. Sintetizadores: viven en SuperCollider (scsynth, UDP 57110) y generan el sonido.
2. Instrumentos: dispositivos físicos (no teclado ni mouse) que Java lee y transforma en parámetros de los synths. Un dispositivo distinto por synth:
   - Joystick/control USB → `organ70s` (botones = noteOn/noteOff, ejes = parciales del órgano).
   - Tableta gráfica Huion → `saxophone` (X selecciona semitono, Y octava, presión volumen; levantar el lápiz termina la nota).
   - Micrófono predeterminado del sistema → `violin` (pitch detectado = nota MIDI, RMS = volumen).
3. Editor musical: monitorea todos los comandos que los instrumentos envían a SuperCollider, graba, carga y reproduce música.

# Tech stack
- Java 22 o superior (se usa la API Foreign Function & Memory para leer el joystick desde `winmm.dll`).
- Maven project. Sin `src/main/java` alternativo.
- Java Swing para la UI del editor.
- `javax.sound.sampled` para capturar audio desde el micrófono predeterminado del sistema.
- Tableta gráfica Huion mediante un adaptador de controlador que expone coordenadas y presión.
- JUnit 5 para pruebas unitarias.
- GitHub para control de versiones.
- El joystick solo funciona en Windows (winmm). En otro sistema el instrumento queda "no disponible" y el resto del programa sigue.

# Layout
- Entrypoint (`public static void main`): `src/main/java/powerband/Main.java`.
- `powerband/instrument/`: abstracción del Bridge (`Instrument` y sus dispositivos) y lectores de hardware.
- `powerband/synth/`: implementación del Bridge (`SynthCommand` y un comando por synth).
- `powerband/osc/`: cliente OSC/UDP hacia scsynth.
- `powerband/service/`: lógica del editor (grabar, cargar, reproducir).
- `powerband/repository/`: archivos binarios, índice, Balanced Merge Sort y búsqueda binaria en disco.
- `powerband/dto/`: datos que viajan entre capas (`SynthEvent`, `RecordingInfo`, ...).
- `powerband/exception/`: excepciones propias.
- `powerband/ui/`: ventana Swing del editor.
- `powerband/util/IConstants.java`: todas las constantes.

# Reglas de diseño
- Patrón Bridge: la jerarquía `Instrument` (abstracción) tiene una referencia a `SynthCommand` (implementación). Cualquier instrumento puede conectarse a cualquier synth sin cambiar ninguna de las dos jerarquías.
- Ningún valor quemado en el código: todo número, texto o ruta va en `IConstants`.
- Las excepciones de E/S se envuelven en excepciones propias (`PowerBandException` y descendientes).

# Archivos de datos (carpeta `data/`)
- `recordings.music`: registros binarios de tamaño fijo (`MUSIC_RECORD_SIZE`). Cada grabación = 1 registro encabezado (nombre, inicio, fin, fecha) + N registros comando (tiempo relativo, synth, acción, nota, duración, velocidad, 3 timbres).
- `recordings.index`: registros binarios de tamaño fijo (`INDEX_RECORD_SIZE`): nombre + posición del encabezado. Ordenado por nombre con Balanced Merge Sort en disco; se busca con búsqueda binaria en disco.
- `recordings.list`: texto, una línea por grabación (`nombre|fecha`), en orden cronológico.
- Prohibido ordenar o recorrer arreglos en memoria para el índice: todo ocurre en archivos.

# Diagramas
- Todos los diagramas son PlantUML y viven en `specs/diagrams/*.puml`; sus imágenes en `specs/diagrams/png/`.
- Diagrama de clases general: `specs/diagrams/clases-general.puml`.
- Cada spec de frontend, backend o database incluye en `Assets` el diagrama PlantUML de su feature (clases, secuencia o actividad).
- Regenerar imágenes: `java -jar plantuml.jar -charset UTF-8 -tpng -o png specs/diagrams/*.puml`.
