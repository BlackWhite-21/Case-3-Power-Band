package powerband.instrument;

import java.awt.Component;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import powerband.dto.DatosTableta;
import powerband.dto.EstadoPluma;
import powerband.dto.TabletReading;
import powerband.exception.DeviceException;
import powerband.util.IConstants;

public final class TabletaHuionInputSource implements HuionInputSource {

    private final Component component;
    private final TabletaGrafica tablet;
    private final AtomicBoolean startAttempted = new AtomicBoolean();
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicBoolean failureReported = new AtomicBoolean();
    private final TabletaListener listener = new TabletaListener() {
        @Override
        public void onDatosTableta(DatosTableta pData) {
            publishReading(pData);
        }

        @Override
        public void onError(DeviceException pException) {
            reportFailure(pException);
        }
    };
    private volatile Consumer<TabletReading> readingConsumer;
    private volatile Consumer<DeviceException> failureConsumer;

    public TabletaHuionInputSource(Component pComponent) {
        this(pComponent, new TabletaGrafica());
    }

    TabletaHuionInputSource(Component pComponent, TabletaGrafica pTablet) {
        component = Objects.requireNonNull(pComponent);
        tablet = Objects.requireNonNull(pTablet);
    }

    @Override
    public void start(
            Consumer<TabletReading> pReadingConsumer,
            Consumer<DeviceException> pFailureConsumer) throws DeviceException {
        Objects.requireNonNull(pReadingConsumer);
        Objects.requireNonNull(pFailureConsumer);
        if (!startAttempted.compareAndSet(false, true)) {
            throw new DeviceException(IConstants.ERR_HUION_POINTER_ALREADY_STARTED);
        }
        readingConsumer = pReadingConsumer;
        failureConsumer = pFailureConsumer;
        running.set(true);
        tablet.addListener(listener);
        try {
            tablet.initialize(component);
        } catch (DeviceException | RuntimeException pException) {
            running.set(false);
            startAttempted.set(false);
            tablet.removeListener(listener);
            if (pException instanceof DeviceException pDeviceException) {
                throw pDeviceException;
            }
            throw new DeviceException(IConstants.ERR_HUION_POINTER_INITIALIZE, pException);
        }
    }

    @Override
    public void stop() {
        if (!startAttempted.get()) {
            return;
        }
        running.set(false);
        tablet.closeAsync();
    }

    private void publishReading(DatosTableta pData) {
        if (!running.get()) {
            return;
        }
        Consumer<TabletReading> pConsumer = readingConsumer;
        if (pConsumer == null) {
            return;
        }
        try {
            pConsumer.accept(normalize(pData));
        } catch (DeviceException pException) {
            reportFailure(pException);
        }
    }

    private TabletReading normalize(DatosTableta pData) throws DeviceException {
        int pWidth = component.getWidth();
        int pHeight = component.getHeight();
        if (pWidth <= IConstants.HUION_COORDINATE_MIN
                || pHeight <= IConstants.HUION_COORDINATE_MIN) {
            throw new DeviceException(IConstants.ERR_HUION_POINTER_COORDINATES);
        }
        float pX = Math.clamp(
                (float) (pData.x() / pWidth),
                IConstants.HUION_PRESSURE_MIN,
                IConstants.HUION_COORDINATE_MAX);
        float pY = Math.clamp(
                (float) (pData.y() / pHeight),
                IConstants.HUION_PRESSURE_MIN,
                IConstants.HUION_COORDINATE_MAX);
        float pPressure = pData.estado() == EstadoPluma.LIBERADO
                || pData.estado() == EstadoPluma.FUERA_SUPERFICIE
                        ? IConstants.HUION_PRESSURE_MIN
                        : pData.pressure();
        return new TabletReading(pX, pY, pPressure);
    }

    private void reportFailure(DeviceException pException) {
        if (failureReported.compareAndSet(false, true)) {
            Consumer<DeviceException> pConsumer = failureConsumer;
            if (pConsumer != null) {
                pConsumer.accept(pException);
            }
        }
    }
}
