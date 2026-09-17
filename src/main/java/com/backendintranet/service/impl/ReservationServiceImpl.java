package com.backendintranet.service.impl;

import com.backendintranet.dto.request.ReservationRequest;
import com.backendintranet.dto.response.ReservationResponse;
import com.backendintranet.entity.Reservation;
import com.backendintranet.entity.User;
import com.backendintranet.repository.ReservationRepository;
import com.backendintranet.repository.UserRepository;
import com.backendintranet.service.ReservationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;

    @Override
    public ReservationResponse create(ReservationRequest dto, String userId) {
        User user = userRepository.findByUsername(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + userId));

        List<Reservation> conflicts = reservationRepository.findConflicts(
                dto.getRoom(), dto.getDate(), dto.getStartTime(), dto.getEndTime()
        );
        if (!conflicts.isEmpty()) {
            Reservation c = conflicts.get(0);
            throw new IllegalStateException(
                    "Conflicto: ya existe una reserva de " + c.getStartTime() + " a " + c.getEndTime()
            );
        }

        Reservation reservation = Reservation.builder()
                .date(dto.getDate())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .bookedBy(user)
                .purpose(dto.getPurpose())
                .attendees(dto.getAttendees())
                .room(dto.getRoom())
                .status("confirmada")
                .build();

        return toDTO(reservationRepository.save(reservation));
    }

    @Override
    public List<ReservationResponse> getByDate(LocalDate date) {
        return reservationRepository.findByDateAndStatusNot(date, "cancelada")
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<ReservationResponse> getUpcoming() {
        return reservationRepository.findUpcoming(LocalDate.now())
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public ReservationResponse cancel(String id, String userId) {
        Reservation r = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reserva no encontrada: " + id));

        User user = userRepository.findByUsername(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + userId));

        boolean isSuperAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals("SUPER_ADMIN"));
        boolean isOwner = r.getBookedBy().getId().equals(user.getId());

        if (!isSuperAdmin && !isOwner) {
            throw new AccessDeniedException("No tienes permiso para cancelar esta reserva");
        }

        r.setStatus("cancelada");
        return toDTO(reservationRepository.save(r));
    }

    private ReservationResponse toDTO(Reservation r) {
        return ReservationResponse.builder()
                .id(r.getId())
                .date(r.getDate())
                .startTime(r.getStartTime())
                .endTime(r.getEndTime())
                .bookedById(r.getBookedBy().getId())
                .bookedBy(r.getBookedBy().getFirstName() + " " + r.getBookedBy().getLastName())
                .purpose(r.getPurpose())
                .attendees(r.getAttendees())
                .room(r.getRoom())
                .status(r.getStatus())
                .build();
    }
}