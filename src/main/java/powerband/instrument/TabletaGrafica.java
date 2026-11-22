package powerband.instrument;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import powerband.dto.DatosTableta;
import powerband.dto.EstadoPluma;
import powerband.exception.DeviceException;
import powerband.util.IConstants;

public final class TabletaGrafica implements AutoCloseable {

    private static final System.Logger LOGGER = System.getLogger(TabletaGrafica.class.getName());

    private final PointerEventSource eventSource;
    private final CopyOnWriteArrayList<TabletaListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean initialized = new AtomicBoolean();
    private final AtomicBoolean closing = new AtomicBoolean();
    private final AtomicBoolean detachStarted = new AtomicBoolean();
    private final Object lifecycleLock = new Object();
    private final AtomicBoolean contactActive = new AtomicBoolean();
    private final CompletableFuture<Void> termination = new CompletableFuture<>();
    private volatile Component component;
    private volatile HierarchyListener hierarchyListener;
    private volatile Thread captureThread;
    private volatile boolean sourceAttached;
    private volatile DeviceException failure;
    private volatile DatosTableta lastData;

    public TabletaGrafica() {
        this(new JnaPointerInputSource());
    }

    TabletaGrafica(PointerEventSource pEventSource) {
        this.eventSource = Objects.requireNonNull(pEventSource);
    }

    public CompletableFuture<Void> initialize(Component pComponent) throws DeviceException {
        Objects.requireNonNull(pComponent);
        if (!pComponent.isDisplayable() || !pComponent.isShowing()) {
            throw new DeviceException(IConstants.ERR_HUION_POINTER_NOT_DISPLAYABLE);
        }
        if (closing.get() || !initialized.compareAndSet(false, true)) {
            throw new DeviceException(IConstants.ERR_HUION_POINTER_ALREADY_STARTED);
        }
        component = pComponent;
        hierarchyListener = pEvent -> closeIfSurfaceUnavailable(pEvent);
        pComponent.addHierarchyListener(hierarchyListener);
        CompletableFuture<Void> pInitialized = new CompletableFuture<>();
        Runnable pAttach = () -> attachAndStart(pInitialized);
        if (EventQueue.isDispatchThread()) {
            pAttach.run();
        } else {
            EventQueue.invokeLater(pAttach);
        }
        return pInitialized;
    }

    public void addListener(TabletaListener pListener) {
        listeners.add(Objects.requireNonNull(pListener));
    }

    public void removeListener(TabletaListener pListener) {
        listeners.remove(Objects.requireNonNull(pListener));
    }

    public Optional<DeviceException> getFailure() {
        return Optional.ofNullable(failure);
    }

    public CompletableFuture<Void> closeAsync() {
        if (!closing.compareAndSet(false, true)) {
            return termination;
        }
        eventSource.requestStop();
        if (!initialized.get()) {
            termination.complete(null);
        } else {
            boolean pDetachBeforeCapture;
            synchronized (lifecycleLock) {
                pDetachBeforeCapture = sourceAttached && captureThread == null;
            }
            if (pDetachBeforeCapture) {
                detachInBackground();
            }
        }
        return termination;
    }

    @Override
    public void close() {
        closeAsync();
    }

    private void attachAndStart(CompletableFuture<Void> pInitialized) {
        if (closing.get()) {
            pInitialized.completeExceptionally(new DeviceException(IConstants.ERR_HUION_POINTER_CLOSE));
            detachInBackground();
            return;
        }
        try {
            eventSource.attach(component);
            synchronized (lifecycleLock) {
                sourceAttached = true;
                if (closing.get()) {
                    eventSource.requestStop();
                    pInitialized.completeExceptionally(
                            new DeviceException(IConstants.ERR_HUION_POINTER_CLOSE));
                    detachInBackground();
                    return;
                }
                Thread pThread = new Thread(this::captureEvents, IConstants.HUION_POINTER_THREAD_NAME);
                pThread.setDaemon(true);
                captureThread = pThread;
                pThread.start();
            }
            pInitialized.complete(null);
        } catch (DeviceException | RuntimeException e) {
            DeviceException pException = e instanceof DeviceException deviceException
                    ? deviceException
                    : new DeviceException(IConstants.ERR_HUION_POINTER_INITIALIZE, e);
            failure = pException;
            closing.set(true);
            reportError(pException);
            pInitialized.completeExceptionally(pException);
            detachInBackground();
        }
    }

    private void captureEvents() {
        try {
            while (!closing.get()) {
                DatosTableta pData = eventSource.pollEvent(IConstants.HUION_POINTER_POLL_TIMEOUT_MS);
                if (pData != null) {
                    updateContactState(pData);
                    notifyListeners(pData);
                }
            }
        } catch (DeviceException e) {
            failure = e;
            closing.set(true);
            reportError(e);
        } catch (RuntimeException e) {
            DeviceException pException = new DeviceException(IConstants.ERR_HUION_POINTER_EVENT, e);
            failure = pException;
            closing.set(true);
            reportError(pException);
        } finally {
            eventSource.requestStop();
            releaseActiveContact();
            detachInBackground();
        }
    }

    private void updateContactState(DatosTableta pData) {
        lastData = pData;
        if (pData.estado() == EstadoPluma.PRESIONADO
                || (pData.estado() == EstadoPluma.MOVIMIENTO && pData.pressure() > IConstants.HUION_PRESSURE_MIN)) {
            contactActive.set(true);
        } else if (pData.estado() == EstadoPluma.LIBERADO
                || pData.estado() == EstadoPluma.FUERA_SUPERFICIE) {
            contactActive.set(false);
        }
    }

    private void releaseActiveContact() {
        DatosTableta pLastData = lastData;
        if (pLastData != null && contactActive.compareAndSet(true, false)) {
            notifyListeners(new DatosTableta(
                    pLastData.x(),
                    pLastData.y(),
                    IConstants.HUION_PRESSURE_MIN,
                    EstadoPluma.LIBERADO));
        }
    }

    private void notifyListeners(DatosTableta pData) {
        for (TabletaListener pListener : listeners) {
            try {
                pListener.onDatosTableta(pData);
            } catch (RuntimeException e) {
                LOGGER.log(System.Logger.Level.ERROR, IConstants.ERR_HUION_POINTER_EVENT, e);
            }
        }
    }

    private void reportError(DeviceException pException) {
        LOGGER.log(System.Logger.Level.ERROR, pException.getMessage(), pException);
        for (TabletaListener pListener : listeners) {
            try {
                pListener.onError(pException);
            } catch (RuntimeException e) {
                LOGGER.log(System.Logger.Level.ERROR, IConstants.ERR_HUION_POINTER_EVENT, e);
            }
        }
    }

    private void closeIfSurfaceUnavailable(HierarchyEvent pEvent) {
        long pChangedFlags = pEvent.getChangeFlags();
        if (((pChangedFlags & HierarchyEvent.DISPLAYABILITY_CHANGED) != IConstants.AWT_FLAG_NOT_SET
                && !pEvent.getComponent().isDisplayable())
                || ((pChangedFlags & HierarchyEvent.SHOWING_CHANGED) != IConstants.AWT_FLAG_NOT_SET
                        && !pEvent.getComponent().isShowing())) {
            close();
        }
    }

    private void detachInBackground() {
        if (!detachStarted.compareAndSet(false, true)) {
            return;
        }
        Runnable pDetach = () -> {
            DeviceException pDetachFailure = null;
            try {
                if (sourceAttached) {
                    eventSource.detach();
                    sourceAttached = false;
                }
            } catch (DeviceException e) {
                pDetachFailure = e;
                failure = pDetachFailure;
                reportError(pDetachFailure);
            }
            DeviceException pFinalFailure = pDetachFailure;
            Runnable pFinish = () -> {
                Component pComponent = component;
                HierarchyListener pListener = hierarchyListener;
                if (pComponent != null && pListener != null) {
                    pComponent.removeHierarchyListener(pListener);
                }
                if (pFinalFailure == null) {
                    termination.complete(null);
                } else {
                    termination.completeExceptionally(pFinalFailure);
                }
            };
            if (EventQueue.isDispatchThread()) {
                pFinish.run();
            } else {
                EventQueue.invokeLater(pFinish);
            }
        };
        if (Thread.currentThread() == captureThread || !sourceAttached) {
            pDetach.run();
        } else {
            Thread pCleanupThread = new Thread(
                    pDetach,
                    IConstants.HUION_POINTER_THREAD_NAME + IConstants.HUION_POINTER_CLEANUP_THREAD_SUFFIX);
            pCleanupThread.setDaemon(true);
            pCleanupThread.start();
        }
    }
}
