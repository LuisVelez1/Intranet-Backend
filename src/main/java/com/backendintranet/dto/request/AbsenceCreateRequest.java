package com.backendintranet.dto.request;

import jakarta.validation.constraints.*;

import lombok.Data;

import java.time.*;

@Data
public class AbsenceCreateRequest {
    @NotBlank
    @Size(max = 100)
    private String type;

    @NotNull private LocalDate startDate;
    @NotNull private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;

    @NotBlank
    @Size(max = 2000)
    private String reason;

    @AssertTrue(
            message =
                    "Fechas inválidas: indique ambas horas con precisión de minutos y un regreso"
                        + " posterior, o ninguna hora para días completos")
    public boolean isValidPeriod() {
        if (startDate == null || endDate == null) return true;
        if (endDate.isBefore(startDate)) return false;
        if (startTime == null && endTime == null) return true;
        if (startTime == null || endTime == null) return false;
        if (startTime.getSecond() != 0
                || startTime.getNano() != 0
                || endTime.getSecond() != 0
                || endTime.getNano() != 0) return false;
        return LocalDateTime.of(endDate, endTime).isAfter(LocalDateTime.of(startDate, startTime));
    }
}
