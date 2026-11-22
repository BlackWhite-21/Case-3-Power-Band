package powerband.instrument;

record NativePointerEvent(
        int message,
        int pressure,
        int x,
        int y,
        boolean pressureAvailable,
        boolean inContact,
        boolean coordinatesAvailable,
        double scaleX,
        double scaleY,
        double componentX,
        double componentY,
        double componentWidth,
        double componentHeight) {
}
