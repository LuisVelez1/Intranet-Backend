package com.backendintranet.service;

import com.backendintranet.dto.request.RequirementCreateRequest;
import com.backendintranet.dto.request.RequirementUpdateRequest;
import com.backendintranet.dto.response.RequirementResponse;
import com.backendintranet.entity.Area;
import com.backendintranet.entity.Requirement;
import com.backendintranet.entity.RequirementType;
import com.backendintranet.entity.User;
import com.backendintranet.repository.AreaRepository;
import com.backendintranet.repository.RequirementRepository;
import com.backendintranet.repository.RequirementTypeRepository;
import com.backendintranet.repository.UserRepository;
import com.backendintranet.service.impl.RequirementServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequirementServiceImplTest {

    @Mock RequirementRepository requirementRepository;
    @Mock RequirementTypeRepository requirementTypeRepository;
    @Mock AreaRepository areaRepository;
    @Mock UserRepository userRepository;
    @InjectMocks RequirementServiceImpl service;

    @Test
    void createResolvesRelationshipsAndInitializesPendingRequirement() {
        User creator = User.builder().id("u1").firstName("Jane").lastName("Doe").build();
        Area area = Area.builder().id(2).name("Operations").build();
        RequirementType type = RequirementType.builder().id(3).name("Access").area(area).build();
        RequirementCreateRequest request = new RequirementCreateRequest();
        request.setTitle("Laptop access"); request.setDescription("Need access"); request.setAreaId(2);
        request.setTypeId(3); request.setPriority("HIGH"); request.setDueDate(LocalDate.of(2026, 10, 1));
        when(userRepository.findById("u1")).thenReturn(Optional.of(creator));
        when(areaRepository.findById(2)).thenReturn(Optional.of(area));
        when(requirementTypeRepository.findById(3)).thenReturn(Optional.of(type));
        when(requirementRepository.save(any())).thenAnswer(invocation -> { Requirement r = invocation.getArgument(0); r.setId(10); return r; });

        RequirementResponse response = service.create(request, "u1");

        verify(requirementRepository).save(argThat(r -> r.getStatus().equals("PENDING") && r.getActive()
                && r.getCreatedBy() == creator && r.getArea() == area && r.getType() == type));
        assertThat(response.getId()).isEqualTo(10);
        assertThat(response.getTitle()).isEqualTo("Laptop access");
        assertThat(response.getAreaName()).isEqualTo("Operations");
        assertThat(response.getTypeName()).isEqualTo("Access");
        assertThat(response.getCreatedByName()).isEqualTo("Jane Doe");
    }

    @Test
    void updateChangesOnlyProvidedFieldsAndResolvesAssignedUser() {
        Area oldArea = Area.builder().id(1).name("Old").build();
        Area newArea = Area.builder().id(2).name("New").build();
        RequirementType type = RequirementType.builder().id(4).name("Hardware").build();
        User assigned = User.builder().id("u2").firstName("Alex").lastName("Smith").build();
        Requirement existing = Requirement.builder().id(7).title("Original").description("Keep")
                .area(oldArea).type(type).priority("LOW").status("PENDING").active(true).build();
        RequirementUpdateRequest request = new RequirementUpdateRequest();
        request.setTitle("Updated"); request.setAreaId(2); request.setAssignedTo("u2");
        when(requirementRepository.findById(7)).thenReturn(Optional.of(existing));
        when(areaRepository.findById(2)).thenReturn(Optional.of(newArea));
        when(userRepository.findById("u2")).thenReturn(Optional.of(assigned));
        when(requirementRepository.save(existing)).thenReturn(existing);

        service.update(7, request);

        assertThat(existing.getTitle()).isEqualTo("Updated");
        assertThat(existing.getDescription()).isEqualTo("Keep");
        assertThat(existing.getPriority()).isEqualTo("LOW");
        assertThat(existing.getArea()).isSameAs(newArea);
        assertThat(existing.getAssignedTo()).isSameAs(assigned);
        verify(requirementRepository).save(existing);
    }

    @Test
    void deleteSoftDeletesAndSavesRequirement() {
        Requirement existing = Requirement.builder().id(8).active(true).build();
        when(requirementRepository.findById(8)).thenReturn(Optional.of(existing));

        service.delete(8);

        assertThat(existing.getActive()).isFalse();
        verify(requirementRepository).save(existing);
    }
}