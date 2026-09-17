package com.backendintranet.service;

import com.backendintranet.dto.request.FixedAssetRequest;
import com.backendintranet.dto.response.FixedAssetResponse;
import com.backendintranet.entity.Area;
import com.backendintranet.entity.FixedAsset;
import com.backendintranet.entity.User;
import com.backendintranet.repository.AreaRepository;
import com.backendintranet.repository.FixedAssetRepository;
import com.backendintranet.repository.UserRepository;
import com.backendintranet.service.impl.FixedAssetServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FixedAssetServiceImplTest {

    @Mock FixedAssetRepository fixedAssetRepository;
    @Mock AreaRepository areaRepository;
    @Mock UserRepository userRepository;
    @InjectMocks FixedAssetServiceImpl service;

    @Test
    void createResolvesRelationshipsGeneratesCodeAndMapsResponse() {
        Area area = Area.builder().id(2).name("Operations").build();
        User assigned = User.builder().id("u1").firstName("Jane").lastName("Doe").position("Analyst").build();
        FixedAssetRequest request = FixedAssetRequest.builder().name("Laptop").category("Monitor").areaId(2).assignedToId("u1").status("ACTIVE").build();
        when(areaRepository.findById(2)).thenReturn(Optional.of(area));
        when(userRepository.findById("u1")).thenReturn(Optional.of(assigned));
        when(fixedAssetRepository.findByCodeStartingWith("MON-LICO-")).thenReturn(List.of());
        when(fixedAssetRepository.save(any())).thenAnswer(invocation -> { FixedAsset asset = invocation.getArgument(0); asset.setId("a1"); return asset; });

        FixedAssetResponse response = service.create(request);

        ArgumentCaptor<FixedAsset> captor = ArgumentCaptor.forClass(FixedAsset.class);
        verify(fixedAssetRepository).save(captor.capture());
        assertThat(captor.getValue().getArea()).isSameAs(area);
        assertThat(captor.getValue().getAssignedTo()).isSameAs(assigned);
        assertThat(captor.getValue().getCode()).isEqualTo("MON-LICO-0001");
        assertThat(response.getAreaName()).isEqualTo("Operations");
        assertThat(response.getAssignedToFullName()).isEqualTo("Jane Doe");
    }

    @Test
    void updateResolvesChangedAreaAndClearsAssignmentWhenNotProvided() {
        FixedAsset existing = FixedAsset.builder().id("a1").name("Old").assignedTo(User.builder().id("u1").build()).build();
        Area area = Area.builder().id(3).name("Finance").build();
        FixedAssetRequest request = FixedAssetRequest.builder().name("Updated").category("Monitor").areaId(3).assignedToId(null).build();
        when(fixedAssetRepository.findById("a1")).thenReturn(Optional.of(existing));
        when(areaRepository.findById(3)).thenReturn(Optional.of(area));
        when(fixedAssetRepository.save(existing)).thenReturn(existing);

        service.update("a1", request);

        assertThat(existing.getName()).isEqualTo("Updated");
        assertThat(existing.getArea()).isSameAs(area);
        assertThat(existing.getAssignedTo()).isNull();
        verify(userRepository, never()).findById(any());
    }

    @Test
    void missingAssetThrowsEntityNotFoundException() {
        when(fixedAssetRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("missing"))
                .isInstanceOf(EntityNotFoundException.class);
    }
}