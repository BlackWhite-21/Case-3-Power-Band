package powerband.util;

import java.awt.Color;

/**
 * Biblioteca de constantes de Power Band. Ningún otro archivo tiene valores quemados.
 */
public interface IConstants {

    public static final int ZERO_INT = 0;
    public static final int ONE_INT = 1;
    public static final long ZERO_LONG = 0L;
    public static final byte PADDING_BYTE = 0;
    public static final String EMPTY_STRING = "";

    // ------------------------------------------------------------ SuperCollider / OSC
    public static final String SC_DEFAULT_HOST = "127.0.0.1";
    public static final int SC_DEFAULT_PORT = 57110;
    public static final int SC_PING_TIMEOUT_MS = 1500;
    public static final int OSC_RECEIVE_BUFFER = 1024;
    public static final String OSC_STATUS = "/status";
    public static final String OSC_STATUS_REPLY = "/status.reply";
    public static final String OSC_SYNTH_NEW = "/s_new";
    public static final String OSC_NODE_SET = "/n_set";
    public static final char OSC_TYPE_TAG_START = ',';
    public static final char OSC_TYPE_INT = 'i';
    public static final char OSC_TYPE_FLOAT = 'f';
    public static final char OSC_TYPE_STRING = 's';
    public static final int OSC_ALIGNMENT = 4;
    public static final int OSC_ADD_TO_HEAD = 0;
    public static final int OSC_DEFAULT_GROUP = 1;
    public static final int OSC_FIRST_NODE_ID = 10000;
    public static final int OSC_SNEW_FIXED_ARGS = 4;            // defName, nodeId, addAction, target
    public static final String OSC_GATE = "gate";
    public static final int OSC_GATE_OFF = 0;

    // ------------------------------------------------------------ Synths (ids guardados en disco)
    public static final int SYNTH_PIANO_ID = 1;
    public static final int SYNTH_BASS_ID = 2;
    public static final int SYNTH_ORGAN_ID = 3;
    public static final int SYNTH_SAXOPHONE_ID = 4;
    public static final int SYNTH_VIOLIN_ID = 5;
    public static final String SYNTH_PIANO_NAME = "classicPiano";
    public static final String SYNTH_BASS_NAME = "stringBass";
    public static final String SYNTH_ORGAN_NAME = "organ70s";
    public static final String SYNTH_SAXOPHONE_NAME = "saxophone";
    public static final String SYNTH_VIOLIN_NAME = "violin";

    // Parámetros de los SynthDefs (nombres tal cual en myband.sc)
    public static final String PARAM_NOTE = "note";
    public static final String PARAM_DUR_MS = "durMs";
    public static final String PARAM_THIRD = "third";
    public static final String PARAM_AMP = "amp";
    public static final String PARAM_FREQ = "freq";
    public static final String PARAM_R1 = "r1";
    public static final String PARAM_R2 = "r2";
    public static final String PARAM_R3 = "r3";
    public static final String PARAM_A1 = "a1";
    public static final String PARAM_A2 = "a2";
    public static final String PARAM_A3 = "a3";
    public static final String PARAM_GATE = "gate";

    // Acciones de un comando
    public static final int ACTION_PLAY = 1;   // nota con duración (one-shot)
    public static final int ACTION_ON = 2;     // nota sostenida, empieza
    public static final int ACTION_OFF = 3;    // nota sostenida, termina

    // Piano
    public static final int PIANO_MAJOR_THIRD = 4;
    public static final int PIANO_MINOR_THIRD = 3;
    public static final float PIANO_MAJOR_THRESHOLD = 0.5f;
    public static final int PIANO_DEFAULT_DUR_MS = 1500;
    public static final float PIANO_AMP_MIN = 0.1f;
    public static final float PIANO_AMP_MAX = 0.5f;

    // Bajo
    public static final int BASS_DEFAULT_DUR_MS = 700;
    public static final float BASS_AMP_MIN = 0.2f;
    public static final float BASS_AMP_MAX = 0.8f;

    // Saxophone and violin
    public static final int DEFAULT_SUSTAINED_DUR_MS = 800;
    public static final float SUSTAINED_AMP_MAX = 0.8f;

    // Órgano
    public static final float ORGAN_R1 = 1f;
    public static final float ORGAN_R2_MIN = 1.5f;
    public static final float ORGAN_R2_MAX = 4f;
    public static final float ORGAN_R3_MIN = 2f;
    public static final float ORGAN_R3_MAX = 6f;
    public static final float ORGAN_A1 = 1f;
    public static final float ORGAN_A2_MIN = 0.1f;
    public static final float ORGAN_A2_MAX = 0.9f;
    public static final float ORGAN_A3_FACTOR = 0.5f;
    public static final float ORGAN_AMP_MIN = 0.1f;
    public static final float ORGAN_AMP_MAX = 0.4f;
    public static final int ORGAN_DEFAULT_DUR_MS = 800;

    // MIDI
    public static final int MIDI_A4_NOTE = 69;
    public static final double MIDI_A4_FREQ = 440.0;
    public static final double SEMITONES_PER_OCTAVE = 12.0;
    public static final int NOTES_PER_OCTAVE = 12;
    public static final int[] MAJOR_SCALE = { 0, 2, 4, 5, 7, 9, 11 };

    // ------------------------------------------------------------ Instrumentos
    public static final String STATUS_STARTING = "iniciando...";
    public static final String STATUS_ACTIVE = "activo";
    public static final String STATUS_STOPPED = "detenido";
    public static final String STATUS_UNAVAILABLE = "no disponible: ";

    // Joystick (winmm.dll, JOYINFOEX)
    public static final String JOY_DEVICE_NAME = "Joystick USB";
    public static final String JOY_LIBRARY = "winmm";
    public static final String JOY_FUNCTION = "joyGetPosEx";
    public static final String OS_NAME_PROPERTY = "os.name";
    public static final String OS_ARCH_PROPERTY = "os.arch";
    public static final String OS_WINDOWS_PREFIX = "windows";
    public static final int JOY_MAX_DEVICES = 16;
    public static final int JOY_NO_ERROR = 0;
    public static final int JOY_RETURN_ALL = 0xFF;
    public static final int JOYINFOEX_SIZE = 52;
    public static final int JOYINFOEX_OFFSET_SIZE = 0;
    public static final int JOYINFOEX_OFFSET_FLAGS = 4;
    public static final int JOYINFOEX_OFFSET_X = 8;
    public static final int JOYINFOEX_OFFSET_Y = 12;
    public static final int JOYINFOEX_OFFSET_Z = 16;
    public static final int JOYINFOEX_OFFSET_BUTTONS = 32;
    public static final float JOY_AXIS_MAX = 65535f;
    public static final int JOY_BUTTON_COUNT = 8;
    public static final int JOY_BASE_NOTE = 48;
    public static final int JOY_POLL_MS = 15;
    public static final float JOY_VELOCITY = 0.8f;
    public static final String JOY_STATUS_FORMAT = "Joystick: %s";

    // Micrófono
    public static final String MIC_DEVICE_NAME = "Micrófono";
    public static final float MIC_SAMPLE_RATE = 44100f;
    public static final int MIC_SAMPLE_BITS = 16;
    public static final int MIC_CHANNELS = 1;
    public static final boolean MIC_SIGNED = true;
    public static final boolean MIC_BIG_ENDIAN = false;
    public static final int MIC_FRAME_SAMPLES = 2048;
    public static final int MIC_BYTES_PER_SAMPLE = 2;
    public static final int MIC_FRAME_BYTES = MIC_FRAME_SAMPLES * MIC_BYTES_PER_SAMPLE * MIC_CHANNELS;
    public static final int MIC_LINE_BUFFER_FRAMES = 2;
    public static final int BYTE_MASK = 0xFF;
    public static final int BITS_PER_BYTE = 8;
    public static final double MIC_SAMPLE_FULL_SCALE = 32768.0;
    public static final double MIC_ONSET_RMS = 0.06;
    public static final double MIC_RELEASE_RMS = 0.03;
    public static final double MIC_RMS_FULL = 0.4;
    public static final long MIC_COOLDOWN_MS = 150;
    public static final double PITCH_MIN_FREQ = 50.0;
    public static final double PITCH_MAX_FREQ = 600.0;
    public static final double PITCH_MIN_CORRELATION = 0.5;
    public static final int MIDI_MIN_NOTE = 0;
    public static final int MIDI_MAX_NOTE = 127;
    public static final int BASS_MIN_NOTE = 28;
    public static final int BASS_MAX_NOTE = 52;
    public static final int BASS_FALLBACK_NOTE = 36;

    // Huion tablet
    public static final String HUION_DEVICE_NAME = "Tableta gráfica Huion";
    public static final int HUION_BASE_NOTE = 48;
    public static final int HUION_OCTAVE_COUNT = 2;
    public static final int NO_ACTIVE_NOTE = -1;
    public static final String HUION_INPUT_THREAD_NAME = "powerband-huion-input";
    public static final String HUION_POINTER_THREAD_NAME = "powerband-huion-pointer";
    public static final String HUION_POINTER_OS_WINDOWS = "windows";
    public static final String HUION_POINTER_ARCH_AMD64 = "amd64";
    public static final String HUION_POINTER_LIBRARY = "user32";
    public static final String HUION_POINTER_CLEANUP_THREAD_SUFFIX = "-cleanup";
    public static final int HUION_POINTER_POLL_TIMEOUT_MS = 100;
    public static final int HUION_POINTER_PRESSURE_MAX = 1024;
    public static final int HUION_POINTER_RAW_PRESSURE_MIN = 0;
    public static final int HUION_POINTER_PRESSURE_MASK = 0x00000001;
    public static final int HUION_POINTER_TYPE_PEN = 3;
    public static final int HUION_POINTER_HOOK_GET_MESSAGE = 3;
    public static final int HUION_POINTER_HOOK_MIN_CODE = 0;
    public static final int HUION_POINTER_HOOK_REMOVE_MESSAGE = 1;
    public static final long HUION_POINTER_ID_MASK = 0xFFFFL;
    public static final long HUION_POINTER_COORDINATE_MASK = 0xFFFFL;
    public static final int HUION_POINTER_COORDINATE_SHIFT = 16;
    public static final int HUION_POINTER_IN_CONTACT_FLAG = 0x00000004;
    public static final int HUION_POINTER_FLAG_NOT_SET = 0;
    public static final int HUION_POINTER_QUEUE_CAPACITY = 4096;
    public static final int HUION_NO_ACTIVE_POINTER = -1;
    public static final int HUION_COORDINATE_MIN = 0;
    public static final float HUION_COORDINATE_MAX = 1f;
    public static final int AWT_FLAG_NOT_SET = 0;
    public static final double HUION_POINTER_INVALID_SCALE = 0.0;
    public static final int NATIVE_ZERO = 0;
    public static final long NATIVE_NULL_POINTER = 0L;
    public static final float HUION_PRESSURE_MIN = 0f;
    public static final float HUION_PRESSURE_MAX = 1f;
    public static final int HUION_WM_POINTER_UPDATE = 0x0245;
    public static final int HUION_WM_POINTER_DOWN = 0x0246;
    public static final int HUION_WM_POINTER_UP = 0x0247;
    public static final int HUION_WM_POINTER_LEAVE = 0x024A;
    public static final int HUION_WM_POINTER_CAPTURE_CHANGED = 0x024C;
    public static final String MICROPHONE_THREAD_NAME = "powerband-microphone-input";
    public static final int DEVICE_STOP_TIMEOUT_MS = 1000;

    // ------------------------------------------------------------ Archivos
    public static final String DATA_DIR = "data";
    public static final String MUSIC_FILE = "recordings.music";
    public static final String INDEX_FILE = "recordings.index";
    public static final String LIST_FILE = "recordings.list";
    public static final String SORT_TAPE_PREFIX = "recordings.index.tape";
    public static final String[] SORT_TAPES_A = { "A0", "A1" };
    public static final String[] SORT_TAPES_B = { "B0", "B1" };
    public static final String FILE_MODE_RW = "rw";
    public static final String FILE_MODE_R = "r";

    public static final int NAME_LENGTH = 32;                  // caracteres fijos del nombre
    public static final int MUSIC_RECORD_SIZE = 96;            // bytes por registro en recordings.music
    public static final int INDEX_RECORD_SIZE = 72;            // 32 chars * 2 bytes + long de 8 bytes
    public static final int MUSIC_HEADER_FIXED_BYTES =
            Byte.BYTES + NAME_LENGTH * Character.BYTES + 3 * Long.BYTES;
    public static final int MUSIC_COMMAND_FIXED_BYTES =
            Byte.BYTES + Long.BYTES + 4 * Integer.BYTES + 4 * Float.BYTES;
    public static final byte RECORD_TYPE_HEADER = 'H';
    public static final byte RECORD_TYPE_COMMAND = 'C';
    public static final char PADDING_CHAR = '\0';
    public static final long NOT_FOUND = -1L;
    public static final int MERGE_TAPES = 2;                   // cintas por lado en el Balanced Merge
    public static final int FIRST_RUN_LENGTH = 1;

    public static final String NAME_PATTERN = "[\\p{L}0-9 _\\-]+";
    public static final String LIST_SEPARATOR = "|";
    public static final String LIST_SEPARATOR_REGEX = "\\|";
    public static final int LIST_FIELDS = 2;
    public static final String LIST_DISPLAY_FORMAT = "%s   %s";
    public static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    // ------------------------------------------------------------ Reproducción
    public static final int PLAYBACK_BUFFER_SIZE = 64;
    public static final long NANOS_PER_MILLI = 1_000_000L;
    public static final long MAX_PLAYBACK_OFFSET_MS = Long.MAX_VALUE / NANOS_PER_MILLI;

    // ------------------------------------------------------------ UI
    public static final String APP_TITLE = "Power Band - Editor musical";
    public static final int WINDOW_WIDTH = 820;
    public static final int WINDOW_HEIGHT = 560;
    public static final int UI_COMPONENT_GAP = 12;
    public static final int MONITOR_MAX_LINES = 400;
    public static final int MONITOR_ROWS = 20;
    public static final int MONITOR_COLUMNS = 70;
    public static final Color RECORDING_COLOR = new Color(200, 30, 30);
    public static final Color IDLE_COLOR = new Color(60, 60, 60);
    public static final String BTN_RECORD = "● Grabar";
    public static final String BTN_STOP_RECORD = "■ Terminar grabación";
    public static final String BTN_PAUSE_RECORDING = "Ⅱ Pausar";
    public static final String BTN_RESUME_RECORDING = "▶ Reanudar";
    public static final String BTN_YES_SAVE_RECORDING = "Sí, guardar";
    public static final String BTN_NO_DISCARD_RECORDING = "No, borrar";
    public static final String BTN_LOAD = "Cargar...";
    public static final String BTN_PLAY = "▶ Play";
    public static final String BTN_STOP_PLAY = "■ Stop";
    public static final String LBL_NO_RECORDING = "Ninguna música cargada";
    public static final String LBL_WAITING_FOR_NAME = "Grabación detenida; decida si desea guardarla.";
    public static final String LBL_RECORDING_DISCARDED = "Grabación descartada.";
    public static final String LBL_LOADED = "Cargada: %s (bytes %d a %d, %d comandos)";
    public static final String LBL_RECORDING = "GRABANDO...";
    public static final String LBL_RECORDING_PAUSED = "GRABACIÓN EN PAUSA";
    public static final String LBL_PLAYING = "Reproduciendo: %s";
    public static final String MSG_ASK_NAME = "Nombre de la música grabada:";
    public static final String MSG_CONFIRM_SAVE_RECORDING = "¿Desea guardar la grabación?";
    public static final String MSG_SAVED_RECORDING = "Grabación \"%s\" guardada.";
    public static final String MSG_DISCARD = "¿Descartar la grabación?";
    public static final String MSG_DISCARD_AND_EXIT =
            "Hay una grabación sin guardar. ¿Descartarla y salir?";
    public static final String BTN_DISCARD_AND_EXIT = "Descartar y salir";
    public static final String BTN_KEEP_RECORDING = "Seguir grabando";
    public static final String MSG_SELECT = "Seleccione una grabación (orden cronológico):";
    public static final String MSG_EMPTY_LIST = "No hay grabaciones todavía.";
    public static final String MSG_SC_DOWN = "SuperCollider no responde en %s:%d.\nAbra myband.sc y ejecute ~myband.load;\nEl programa sigue, pero no sonará hasta que el servidor esté arriba.";
    public static final String MSG_ERROR_TITLE = "Power Band";
    public static final String MONITOR_LIVE = "LIVE";
    public static final String MONITOR_PLAYBACK = "PLAY";
    public static final String MONITOR_FORMAT = "[%s] %6d ms  %-12s %-4s nota=%d dur=%d vel=%.2f t=(%.2f, %.2f, %.2f)";
    public static final String INSTRUMENT_FORMAT = "%s → %s: %s";
    public static final String[] ACTION_NAMES = { "", "PLAY", "ON", "OFF" };

    // ------------------------------------------------------------ Editor musical
    public static final String LOGO_TEXT = "POWER BAND";
    public static final String PAGE_EDITOR = "editor";
    public static final String PAGE_RECORDER = "recorder";
    public static final String PAGE_PLAYER = "player";
    public static final String TAB_EDITOR = "Editor";
    public static final String TAB_RECORDER = "Grabadora";
    public static final String TAB_PLAYER = "Reproductor";
    public static final String BTN_SAVE_COPY = "Guardar cambios como copia...";
    public static final String BTN_LOAD_SELECTED = "Cargar selección";
    public static final String BTN_PLAY_SELECTED = "Reproducir selección";
    public static final String BTN_SEARCH_RECORDING = "Buscar";
    public static final String FLD_SEARCH_RECORDING = "Nombre exacto";
    public static final String MSG_SAVE_COPY = "Nombre de la copia editada:";
    public static final String MSG_COPY_SAVED = "Copia \"%s\" guardada.";
    public static final String MSG_NO_EVENTS = "La grabación no contiene comandos.";
    public static final String MSG_EDITED_RECORDING = "Edite tiempo, nota, duración, volumen y timbres.";
    public static final String MSG_SELECT_RECORDING = "Seleccione una grabación para cargar.";
    public static final String MSG_EDITED_COPY_ONLY = "Los cambios se guardan en una copia; el original no se modifica.";
    public static final String MSG_INVALID_EDIT = "El valor no es válido para este comando.";
    public static final String LBL_READY = "Listo";
    public static final String LBL_LOADED_RECORDING = "Grabación cargada: %s";
    public static final String LBL_COPY_SAVED = "Copia guardada: %s";
    public static final String LBL_PLAYBACK_STOPPED = "Reproducción detenida.";
    public static final String LBL_PLAYBACK_COMPLETE = "Reproducción finalizada.";
    public static final String LBL_RECORDING_PAGE =
            "Inicie o termine la grabación y páusela o reanúdela. Al terminar, confirme si desea guardarla.";
    public static final String LBL_PLAYER_PAGE = "Las grabaciones se muestran en orden cronológico.";
    public static final String COL_TIME = "Tiempo (ms)";
    public static final String COL_SYNTH = "Sintetizador";
    public static final String COL_ACTION = "Acción";
    public static final String COL_NOTE = "Nota";
    public static final String COL_DURATION = "Duración (ms)";
    public static final String COL_VOLUME = "Volumen";
    public static final String COL_TIMBRE_1 = "Timbre 1";
    public static final String COL_TIMBRE_2 = "Timbre 2";
    public static final String COL_TIMBRE_3 = "Timbre 3";
    public static final int TIMELINE_COLUMN_COUNT = 9;
    public static final int TIMELINE_TIME_COLUMN = 0;
    public static final int TIMELINE_SYNTH_COLUMN = 1;
    public static final int TIMELINE_ACTION_COLUMN = 2;
    public static final int TIMELINE_NOTE_COLUMN = 3;
    public static final int TIMELINE_DURATION_COLUMN = 4;
    public static final int TIMELINE_VOLUME_COLUMN = 5;
    public static final int TIMELINE_TIMBRE_1_COLUMN = 6;
    public static final int TIMELINE_TIMBRE_2_COLUMN = 7;
    public static final int TIMELINE_TIMBRE_3_COLUMN = 8;
    public static final int UI_SMALL_GAP = 6;
    public static final int UI_LOGO_FONT_SIZE = 30;
    public static final String UI_FONT_FAMILY = "SansSerif";
    public static final Color UI_BACKGROUND_COLOR = new Color(65, 68, 74);
    public static final Color UI_PANEL_COLOR = new Color(78, 82, 89);
    public static final Color UI_ACCENT_COLOR = new Color(42, 116, 190);
    public static final Color UI_TEXT_COLOR = Color.WHITE;
    public static final Color UI_LOGO_COLOR = new Color(205, 48, 48);
    public static final Color UI_TABLE_GRID_COLOR = new Color(110, 115, 123);
    public static final String PLAYBACK_THREAD_NAME = "powerband-playback";

    // ------------------------------------------------------------ Mensajes de error
    public static final String ERR_SC_SEND = "No se pudo enviar el comando a SuperCollider";
    public static final String ERR_SC_TIMEOUT = "SuperCollider no respondió a /status";
    public static final String ERR_OSC_TYPE = "Tipo OSC no soportado: ";
    public static final String ERR_UNKNOWN_SYNTH = "No existe un synth con id ";
    public static final String ERR_NO_JOYSTICK = "no se encontró ningún joystick conectado";
    public static final String ERR_JOY_NOT_WINDOWS = "el joystick usa winmm.dll y solo funciona en Windows";
    public static final String ERR_JOY_READ = "error leyendo el joystick";
    public static final String ERR_MIC_OPEN = "no se pudo abrir el micrófono";
    public static final String ERR_MIC_READ = "error leyendo el micrófono";
    public static final String ERR_MIC_STOP = "error cerrando el micrófono";
    public static final String ERR_HUION_STOP = "error deteniendo la tableta Huion";
    public static final String ERR_HUION_COMMAND = "no se pudo enviar el comando de la tableta a SuperCollider";
    public static final String ERR_HUION_POINTER_NOT_WINDOWS = "la captura de presión Huion requiere Windows x64";
    public static final String ERR_HUION_POINTER_NOT_DISPLAYABLE = "el componente Swing de la tableta debe estar visible y displayable";
    public static final String ERR_HUION_POINTER_ALREADY_STARTED = "la captura de la tableta Huion ya fue inicializada";
    public static final String ERR_HUION_POINTER_API = "no se pudo acceder a Windows Pointer API mediante JNA";
    public static final String ERR_HUION_POINTER_INITIALIZE = "no se pudo inicializar Windows Pointer API para la tableta Huion";
    public static final String ERR_HUION_POINTER_UNSUPPORTED = "Windows Pointer API no reportó un lápiz Huion con presión real";
    public static final String ERR_HUION_POINTER_EVENT = "Windows Pointer API devolvió un evento de lápiz inválido";
    public static final String ERR_HUION_POINTER_COORDINATES = "no se pudieron convertir las coordenadas del lápiz a Swing";
    public static final String ERR_HUION_POINTER_CLOSE = "no se pudo cerrar el adaptador JNA de la tableta Huion";
    public static final String ERR_NAME_EMPTY = "El nombre no puede estar vacío.";
    public static final String ERR_NAME_LONG = "El nombre puede tener máximo " + NAME_LENGTH + " caracteres.";
    public static final String ERR_NAME_CHARS = "Use solo letras, números, espacios, guiones y guion bajo.";
    public static final String ERR_NAME_DUPLICATE = "Ya existe una grabación llamada \"%s\". Escoja otro nombre.";
    public static final String ERR_NOT_FOUND = "No existe la grabación \"%s\" en el índice.";
    public static final String ERR_ALREADY_RECORDING = "Ya se está grabando.";
    public static final String ERR_NOT_RECORDING = "No hay una grabación pendiente.";
    public static final String ERR_MUSIC_IO = "Error de E/S en recordings.music";
    public static final String ERR_INDEX_IO = "Error de E/S en recordings.index";
    public static final String ERR_LIST_IO = "Error de E/S en recordings.list";
    public static final String ERR_SORT_IO = "Error de E/S durante el Balanced Merge Sort";
    public static final String ERR_RECORD_SIZE = "El registro excede el tamaño fijo de ";
    public static final String ERR_RECORD_TYPE = "Tipo de registro inesperado: ";
    public static final String ERR_NOTHING_LOADED = "Primero cargue una grabación.";
    public static final String ERR_UNKNOWN_RECORDING = "Error inesperado al guardar la grabación.";
    public static final String ERR_UNKNOWN_APPLICATION = "Error inesperado en Power Band.";
    public static final String ERR_RECORDING_BUSY = "Termine la grabación actual antes de realizar esta operación.";
    public static final String ERR_PLAYBACK_UNAVAILABLE = "El reproductor de sintetizadores no está configurado.";
    public static final String ERR_PLAYBACK_ACTIVE = "Ya hay una reproducción en curso.";
    public static final String ERR_PLAYBACK_NOT_LOADED = "Primero cargue una grabación para reproducirla.";
    public static final String ERR_INVALID_EDITED_RECORDING = "Los cambios contienen valores inválidos o tiempos desordenados.";
}
