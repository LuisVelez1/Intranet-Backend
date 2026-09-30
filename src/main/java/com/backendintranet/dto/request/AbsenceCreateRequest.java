package com.backendintranet.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class AbsenceCreateRequest {

    @NotBlank
    @Size(max = 100)
    private String type;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @NotNull(message = "La hora de salida es obligatoria")
    private LocalTime startTime;

    @NotNull(message = "La hora de regreso es obligatoria")
    private LocalTime endTime;

    @NotBlank
    @Size(max = 2000)
    private String reason;

    @AssertTrue(
            message =
                    "Periodo inválido: la fecha y hora de regreso deben ser posteriores a la fecha y hora de salida")
    public boolean isValidPeriod() {

        /*
         * Los @NotNull se encargan de reportar
         * los campos faltantes.
         */
        if (startDate == null
                || endDate == null
                || startTime == null
                || endTime == null) {

            return true;
        }

        if (endDate.isBefore(startDate)) {
            return false;
        }

        /*
         * Solo permitimos precisión de minutos.
         */
        if (startTime.getSecond() != 0
                || startTime.getNano() != 0
                || endTime.getSecond() != 0
                || endTime.getNano() != 0) {

            return false;
        }

        LocalDateTime departure =
                LocalDateTime.of(
                        startDate,
                        startTime);

        LocalDateTime arrival =
                LocalDateTime.of(
                        endDate,
                        endTime);

        return arrival.isAfter(departure);
    }
}
