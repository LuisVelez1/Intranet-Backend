package com.backendintranet.service;

import com.backendintranet.dto.request.ReservationRequest;
import com.backendintranet.dto.response.ReservationResponse;
import com.backendintranet.entity.Reservation;
import com.backendintranet.entity.Role;
import com.backendintranet.entity.User;
import com.backendintranet.repository.ReservationRepository;
import com.backendintranet.repository.UserRepository;
import com.backendintranet.service.impl.ReservationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    @Mock ReservationRepository reservationRepository;
    @Mock UserRepository userRepository;
    @InjectMocks ReservationServiceImpl service;

    @Test
    void createResolvesUserChecksConflictsAndReturnsConfirmedReservation() {
        User user = user("u1", "JDOE", "USER");
        ReservationRequest request = request();
        when(userRepository.findByUsername("JDOE")).thenReturn(Optional.of(user));
        when(reservationRepository.findConflicts("Room A", request.getDate(), request.getStartTime(), request.getEndTime()))
                .thenReturn(List.of());
        when(reservationRepository.save(any())).thenAnswer(invocation -> { Reservation r = invocation.getArgument(0); r.setId("r1"); return r; });

        ReservationResponse response = service.create(request, "JDOE");

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertThat(captor.getValue().getBookedBy()).isSameAs(user);
        assertThat(captor.getValue().getStatus()).isEqualTo("confirmada");
        assertThat(response.getId()).isEqualTo("r1");
        assertThat(response.getBookedById()).isEqualTo("u1");
    }

    @Test
    void createRejectsConflictWithoutSaving() {
        User user = user("u1", "JDOE", "USER");
        Reservation conflict = Reservation.builder().startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0)).build();
        ReservationRequest request = request();
        when(userRepository.findByUsername("JDOE")).thenReturn(Optional.of(user));
        when(reservationRepository.findConflicts(any(), any(), any(), any())).thenReturn(List.of(conflict));

        assertThatThrownBy(() -> service.create(request, "JDOE"))
                .isInstanceOf(IllegalStateException.class);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void superAdminCanCancelAndCancelledReservationIsSaved() {
        User admin = user("admin-id", "ADMINUSER", "SUPER_ADMIN");
        Reservation reservation = Reservation.builder().id("r1").bookedBy(user("owner-id", "OWNER", "USER"))
                .date(LocalDate.of(2026, 9, 20)).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0))
                .room("Room A").status("confirmada").build();
        when(reservationRepository.findById("r1")).thenReturn(Optional.of(reservation));
        when(userRepository.findByUsername("ADMINUSER")).thenReturn(Optional.of(admin));
        when(reservationRepository.save(reservation)).thenReturn(reservation);

        ReservationResponse response = service.cancel("r1", "ADMINUSER");

        assertThat(reservation.getStatus()).isEqualTo("cancelada");
        assertThat(response.getStatus()).isEqualTo("cancelada");
        verify(reservationRepository).save(reservation);
    }

    @Test
    void ownerCanCancelOwnReservationWhenIdDiffersFromUsername() {
        User owner = user("owner-id", "OWNER", "USER");
        Reservation reservation = Reservation.builder().id("r1").bookedBy(owner)
                .status("confirmada").build();
        when(reservationRepository.findById("r1")).thenReturn(Optional.of(reservation));
        when(userRepository.findByUsername("OWNER")).thenReturn(Optional.of(owner));
        when(reservationRepository.save(reservation)).thenReturn(reservation);

        ReservationResponse response = service.cancel("r1", "OWNER");

        assertThat(reservation.getStatus()).isEqualTo("cancelada");
        assertThat(response.getStatus()).isEqualTo("cancelada");
        verify(reservationRepository).save(reservation);
    }

    @Test
    void unrelatedRegularUserCannotCancelReservation() {
        User user = user("other-id", "OTHER", "USER");
        Reservation reservation = Reservation.builder().id("r1").bookedBy(user("owner-id", "OWNER", "USER"))
                .status("confirmada").build();
        when(reservationRepository.findById("r1")).thenReturn(Optional.of(reservation));
        when(userRepository.findByUsername("OTHER")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.cancel("r1", "OTHER"))
                .isInstanceOf(AccessDeniedException.class);
        verify(reservationRepository, never()).save(any());
    }

    private static ReservationRequest request() {
        ReservationRequest request = new ReservationRequest();
        request.setDate(LocalDate.of(2026, 9, 20)); request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0)); request.setRoom("Room A");
        request.setPurpose("Planning"); request.setAttendees(4);
        return request;
    }

    private static User user(String id, String username, String roleName) {
        return User.builder().id(id).username(username).firstName("Test").lastName("User")
                .roles(java.util.Set.of(Role.builder().name(roleName).build())).build();
    }
}