package ru.moysklad.concurrency.domain;

public record ReservationResult(boolean accepted, Reservation reservation, int availableAtDecision) {

    public static ReservationResult accepted(Reservation reservation) {
        return new ReservationResult(true, reservation, reservation.quantity());
    }

    public static ReservationResult rejected(int availableAtDecision) {
        return new ReservationResult(false, null, availableAtDecision);
    }
}
