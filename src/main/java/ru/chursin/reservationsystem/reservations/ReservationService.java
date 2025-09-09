package ru.chursin.reservationsystem.reservations;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReservationService {

    private final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository repository;
    private final ReservationMapper mapper;

    public ReservationService(
            ReservationRepository repository,
            ReservationMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public Reservation getReservationById(Long id) {

        ReservationEntity reservationEntity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Not found reservation by id = " + id));

        return mapper.toDomainReservation(reservationEntity);
    }

    public List<Reservation> findAllReservations() {
        List<ReservationEntity> allEntities = repository.findAll();

        return allEntities.stream()
                .map(mapper::toDomainReservation
                ).toList();
    }

    public Reservation createReservation(Reservation reservationToCreate) {
        if (reservationToCreate.status() != null) {
            throw new IllegalArgumentException("Status should be not empty");
        }
        if (!reservationToCreate.endDate().isAfter(reservationToCreate.startDate())) {
            throw new IllegalArgumentException("Start date mast one day earlier then end date");
        }

        var entityToSave = mapper.toEntityReservation(reservationToCreate);
        entityToSave.setStatus(ReservationStatus.PENDING);

        var savedEntity = repository.save(entityToSave);
        return mapper.toDomainReservation(savedEntity);
    }

    public Reservation updateReservation(
            Long id, Reservation reservationToUpdate
    ) {
        var reservationEntity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Not found reservation by id= " + id));

        if (reservationEntity.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("Cannot modify reservation status = " + reservationEntity.getStatus());
        }
        if (!reservationToUpdate.endDate().isAfter(reservationToUpdate.startDate())) {
            throw new IllegalArgumentException("Start date mast one day earlier then end date");
        }

        var reservationToSave = mapper.toEntityReservation(reservationToUpdate);
        reservationToSave.setId(reservationEntity.getId());
        reservationToSave.setStatus(ReservationStatus.PENDING);

        var updatedReservation = repository.save(reservationToSave);
        return mapper.toDomainReservation(updatedReservation) ;
    }

    @Transactional
    public void cancelReservation(Long id) {
        var reservation = repository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Not found reservation by id= " + id));
        if (reservation.getStatus().equals(ReservationStatus.APPROVED)) {
            throw new IllegalStateException("Cannot cancel approved reservation, please contact with manager " + id);
        }
        if (reservation.getStatus().equals(ReservationStatus.CANCELLED)) {
            throw new IllegalStateException("Cannot cancel the reservation, it was already cancelled " + id);
        }

        repository.setStatus(id, ReservationStatus.CANCELLED);
        log.info("Successfully cancelled reservation: id={}", id);
    }

    public Reservation approveReservation(Long id) {
        var reservationEntity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Not found reservation by id= " + id));
        if (reservationEntity.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("Cannot approve reservation status = " + reservationEntity.getStatus());
        }

        var isConflict = isReservationConflict(
                reservationEntity.getRoomId(),
                reservationEntity.getStartDate(),
                reservationEntity.getEndDate()
        );

        if (isConflict) {
            throw new IllegalArgumentException("Cannot approve reservation because of conflict");
        }

        reservationEntity.setStatus(ReservationStatus.APPROVED);
        repository.save(reservationEntity);
        return mapper.toDomainReservation(reservationEntity);
    }

    private boolean isReservationConflict(
            Long roomId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        List<Long> conflictIds = repository.findConflictReservations(
                roomId,
                startDate,
                endDate,
                ReservationStatus.APPROVED
        );

        if (conflictIds.isEmpty()) {
            return false;
        }
        log.info("Conflict with ids ={}", conflictIds);
        return true;
    }
//        return repository.findAll().stream()
//                .filter(existing -> !reservation.getId().equals(existing.getId()))
//                .filter(existing -> reservation.getRoomId().equals(existing.getRoomId()))
//                .filter(existing -> existing.getStatus().equals(ReservationStatus.APPROVED))
//                .anyMatch(existing ->
//                        reservation.getStartDate().isBefore(existing.getEndDate()) &&
//                                existing.getStartDate().isBefore(reservation.getEndDate())
//                );

}
