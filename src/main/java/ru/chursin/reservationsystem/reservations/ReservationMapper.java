package ru.chursin.reservationsystem.reservations;

import org.springframework.stereotype.Component;

@Component
public class ReservationMapper {

    public Reservation toDomain(
            ReservationEntity reservation
    ) {
        return new Reservation(
                reservation.getId(),
                reservation.getUserId(),
                reservation.getRoomId(),
                reservation.getStartDate(),
                reservation.getEndDate(),
                reservation.getStatus()
        );
    }

    public ReservationEntity toEntity(
            Reservation reservation
    ) {
        return new ReservationEntity(
                reservation.id(),
                reservation.id(),
                reservation.id(),
                reservation.startDate(),
                reservation.endDate(),
                reservation.status()
        );
    }
}
