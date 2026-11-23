package org.example.learningprojectserver.service.realworld;

import org.example.learningprojectserver.dto.RealWorldDTOs.RealWorldOverrideRequest;
import org.example.learningprojectserver.dto.RealWorldDTOs.RealWorldTopicDTO;
import org.example.learningprojectserver.dto.RealWorldDTOs.SimulationExplorationDTO;
import org.example.learningprojectserver.entities.RealWorldOverrideEntity;
import org.example.learningprojectserver.entities.SimulationExplorationEntity;
import org.example.learningprojectserver.mappers.RealWorldOverrideEntityToDTOMapper;
import org.example.learningprojectserver.mappers.SimulationExplorationEntityToDTOMapper;
import org.example.learningprojectserver.repository.RealWorldOverrideRepository;
import org.example.learningprojectserver.repository.SimulationExplorationRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.security.AccessGuard;
import org.example.learningprojectserver.service.adaptive.SkillCatalog;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RealWorldServicesTest {

    @Test
    void catalogCoversEveryPracticeTopic() {
        for (String topic : SkillCatalog.mathTopics().keySet()) {
            RealWorldCatalog.Entry entry = RealWorldCatalog.entries().get(topic);
            assertNotNull(entry, "missing real-world text for " + topic);
            assertFalse(entry.everydayExample().isBlank());
            assertFalse(entry.careerLink().isBlank());
        }
        assertEquals(SkillCatalog.mathTopics().keySet(), RealWorldCatalog.entries().keySet());
    }

    @Test
    void subTopicResolvesToItsTopic() {
        assertEquals("שברים", RealWorldCatalog.resolveTopic("כפל שברים"));
        assertEquals("הנדסה אנליטית", RealWorldCatalog.resolveTopic("חיתוך פרבולות"));
        assertEquals("משוואות", RealWorldCatalog.resolveTopic(" משוואות "));
        assertNull(RealWorldCatalog.resolveTopic("היסטוריה"));
        assertNull(RealWorldCatalog.resolveTopic(null));
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class Cards {
        @Mock RealWorldOverrideRepository overrideRepository;
        @Mock AccessGuard accessGuard;
        @Spy RealWorldOverrideEntityToDTOMapper overrideMapper = new RealWorldOverrideEntityToDTOMapper();
        @InjectMocks RealWorldService service;

        private RealWorldOverrideEntity override(String topic, String example) {
            RealWorldOverrideEntity e = new RealWorldOverrideEntity();
            e.setSchoolCode("S1");
            e.setTopic(topic);
            e.setEverydayExample(example);
            e.setCareerLink("קריירה");
            e.setUpdatedAt(LocalDateTime.of(2026, 1, 2, 3, 4));
            return e;
        }

        @Test
        void schoolOverrideReplacesBuiltInCardOnlyForItsTopic() {
            when(accessGuard.schoolCodeOf("st1", "STUDENT")).thenReturn("S1");
            when(overrideRepository.findBySchoolCode("S1")).thenReturn(List.of(override("שברים", "פיצה")));

            List<RealWorldTopicDTO> topics = service.getTopics("st1", "STUDENT").getData();

            assertEquals(SkillCatalog.mathTopics().size(), topics.size());
            RealWorldTopicDTO fractions = topics.stream().filter(t -> t.topic().equals("שברים")).findFirst().orElseThrow();
            assertTrue(fractions.customized());
            assertEquals("פיצה", fractions.everydayExample());
            assertEquals("02/01/2026 03:04", fractions.updatedAt());
            RealWorldTopicDTO vectors = topics.stream().filter(t -> t.topic().equals("וקטורים")).findFirst().orElseThrow();
            assertFalse(vectors.customized());
            assertEquals(RealWorldCatalog.entries().get("וקטורים").everydayExample(), vectors.everydayExample());
        }

        @Test
        void callerWithoutSchoolGetsBuiltInCardsWithoutQuery() {
            when(accessGuard.schoolCodeOf("admin", "SYSTEM_ADMIN")).thenReturn(null);

            BasicResponse<RealWorldTopicDTO> res = service.getTopic("admin", "SYSTEM_ADMIN", "חילוק מספרים שלמים");

            assertTrue(res.isSuccess());
            assertEquals("מספרים שלמים", res.getData().topic());
            assertFalse(res.getData().customized());
            verifyNoInteractions(overrideRepository);
        }

        @Test
        void unknownTopicIsRejected() {
            BasicResponse<RealWorldTopicDTO> res = service.getTopic("st1", "STUDENT", "גאוגרפיה");
            assertFalse(res.isSuccess());
        }

        @Test
        void teacherSavesOverrideForOwnSchool() {
            when(accessGuard.schoolCodeOf("t1", "TEACHER")).thenReturn("S1");
            when(overrideRepository.findBySchoolCodeAndTopic("S1", "משוואות")).thenReturn(Optional.empty());
            when(overrideRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BasicResponse<RealWorldTopicDTO> res = service.saveOverride("t1",
                    new RealWorldOverrideRequest("משוואה ריבועית", "  כדור לסל  ", "מהנדסים"));

            assertTrue(res.isSuccess());
            ArgumentCaptor<RealWorldOverrideEntity> saved = ArgumentCaptor.forClass(RealWorldOverrideEntity.class);
            verify(overrideRepository).save(saved.capture());
            assertEquals("S1", saved.getValue().getSchoolCode());
            assertEquals("משוואות", saved.getValue().getTopic());
            assertEquals("כדור לסל", saved.getValue().getEverydayExample());
            assertEquals("t1", saved.getValue().getUpdatedByTeacherId());
            assertTrue(res.getData().customized());
        }

        @Test
        void overrideNeedsBothTextsAndATeacherSchool() {
            when(accessGuard.schoolCodeOf("t1", "TEACHER")).thenReturn("S1");
            assertFalse(service.saveOverride("t1", new RealWorldOverrideRequest("שברים", "", "x")).isSuccess());
            assertFalse(service.saveOverride("t1", new RealWorldOverrideRequest("שברים", "x".repeat(1001), "x")).isSuccess());

            when(accessGuard.schoolCodeOf("t2", "TEACHER")).thenReturn(null);
            assertFalse(service.saveOverride("t2", new RealWorldOverrideRequest("שברים", "a", "b")).isSuccess());
            verify(overrideRepository, never()).save(any());
        }

        @Test
        void resetDeletesOverrideAndReturnsBuiltIn() {
            RealWorldOverrideEntity existing = override("שברים", "פיצה");
            when(accessGuard.schoolCodeOf("t1", "TEACHER")).thenReturn("S1");
            when(overrideRepository.findBySchoolCodeAndTopic("S1", "שברים")).thenReturn(Optional.of(existing));

            BasicResponse<RealWorldTopicDTO> res = service.resetOverride("t1", "שברים");

            verify(overrideRepository).delete(existing);
            assertFalse(res.getData().customized());
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class Simulations {
        @Mock SimulationExplorationRepository repository;
        @Spy SimulationExplorationEntityToDTOMapper mapper = new SimulationExplorationEntityToDTOMapper();
        @InjectMocks SimulationService service;

        @Test
        void firstExplorationCreatesRowAndLaterOnesCount() {
            when(repository.findByStudentUserIdAndSimulationKey("st1", "parabola")).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SimulationExplorationDTO first = service.recordExploration("st1", "parabola").getData();
            assertEquals(1, first.exploreCount());

            SimulationExplorationEntity existing = new SimulationExplorationEntity();
            existing.setStudentUserId("st1");
            existing.setSimulationKey("parabola");
            existing.setExploreCount(4);
            existing.setFirstExploredAt(LocalDateTime.now().minusDays(1));
            when(repository.findByStudentUserIdAndSimulationKey("st1", "parabola")).thenReturn(Optional.of(existing));

            SimulationExplorationDTO later = service.recordExploration("st1", "parabola").getData();
            assertEquals(5, later.exploreCount());
            assertNotNull(later.lastExploredAt());
        }

        @Test
        void unknownSimulationIsRejected() {
            assertFalse(service.recordExploration("st1", "rocket").isSuccess());
            assertFalse(service.recordExploration(null, "parabola").isSuccess());
            verifyNoInteractions(repository);
        }
    }
}
