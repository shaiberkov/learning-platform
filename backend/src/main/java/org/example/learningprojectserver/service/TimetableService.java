package org.example.learningprojectserver.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.SchoolTeacherOptionDTO;
import org.example.learningprojectserver.dto.TimetableConflictDTO;
import org.example.learningprojectserver.dto.TimetableLessonDTO;
import org.example.learningprojectserver.entities.*;
import org.example.learningprojectserver.repository.ClassRoomRepository;
import org.example.learningprojectserver.repository.ScheduleRepository;
import org.example.learningprojectserver.repository.SchoolRepository;
import org.example.learningprojectserver.repository.TeacherRepository;
import org.example.learningprojectserver.repository.TimetableRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Timetable builder: school-wide lessons with conflict detection (teacher double-booked,
 * class double-booked, room occupied), and create / move / delete of single lessons.
 */
@Service
@RequiredArgsConstructor
public class TimetableService {

    static final Map<DayOfWeek, String> DAY_NAMES = Map.of(
            DayOfWeek.SUNDAY, "ראשון", DayOfWeek.MONDAY, "שני", DayOfWeek.TUESDAY, "שלישי",
            DayOfWeek.WEDNESDAY, "רביעי", DayOfWeek.THURSDAY, "חמישי", DayOfWeek.FRIDAY, "שישי",
            DayOfWeek.SATURDAY, "שבת");

    private final SchoolRepository schoolRepository;
    private final TeacherRepository teacherRepository;
    private final ClassRoomRepository classRoomRepository;
    private final ScheduleRepository scheduleRepository;
    private final TimetableRepository timetableRepository;
    private final SchoolCacheEvictor cacheEvictor;

    @Transactional
    public BasicResponse getTeacherOptions(String schoolCode) {
        SchoolEntity school = schoolRepository.findBySchoolCode(schoolCode);
        if (school == null) {
            return new BasicResponse(false, "בית הספר לא נמצא");
        }
        List<SchoolTeacherOptionDTO> teachers = school.getTeachers().stream()
                .map(t -> new SchoolTeacherOptionDTO(
                        t.getUserId(),
                        t.getUsername(),
                        new ArrayList<>(t.getTeachingSubjects()),
                        t.getTeachingClassRooms().stream().map(ClassRoomEntity::getName).sorted().toList()))
                .sorted(Comparator.comparing(SchoolTeacherOptionDTO::getUsername))
                .toList();
        BasicResponse<List<SchoolTeacherOptionDTO>> response = new BasicResponse<>(true, null);
        response.setData(teachers);
        return response;
    }

    public BasicResponse getSchoolTimetable(String schoolCode) {
        if (schoolRepository.findBySchoolCode(schoolCode) == null) {
            return new BasicResponse(false, "בית הספר לא נמצא");
        }
        BasicResponse<List<TimetableLessonDTO>> response = new BasicResponse<>(true, null);
        response.setData(timetableRepository.findSchoolLessons(schoolCode));
        return response;
    }

    /** Dry run: the conflicts a lesson would cause, without saving. */
    public BasicResponse checkLesson(String schoolCode, TimetableLessonDTO draft) {
        String invalid = validateDraft(draft);
        if (invalid != null) {
            return new BasicResponse(false, invalid);
        }
        List<TimetableConflictDTO> conflicts = findConflicts(draft, timetableRepository.findSchoolLessons(schoolCode));
        BasicResponse<List<TimetableConflictDTO>> response = new BasicResponse<>(conflicts.isEmpty(),
                conflicts.isEmpty() ? "אין התנגשויות" : "נמצאו התנגשויות");
        response.setData(conflicts);
        return response;
    }

    /** Creates a lesson (draft.id null) or moves/edits an existing one. Refused when it conflicts. */
    @Transactional
    public BasicResponse saveLesson(String schoolCode, TimetableLessonDTO draft) {
        String invalid = validateDraft(draft);
        if (invalid != null) {
            return new BasicResponse(false, invalid);
        }

        TeacherEntity teacher = teacherRepository.findByUserId(draft.getTeacherId());
        if (teacher == null || teacher.getTeachingSchool() == null
                || !schoolCode.equals(teacher.getTeachingSchool().getSchoolCode())) {
            return new BasicResponse(false, "המורה לא נמצא בבית הספר");
        }
        ClassRoomEntity classRoom = classRoomRepository.findBySchoolCodeAndClassName(schoolCode, draft.getClassRoomName());
        if (classRoom == null) {
            return new BasicResponse(false, "הכיתה " + draft.getClassRoomName() + " לא נמצאה בבית הספר");
        }
        if (!teacher.getTeachingClassRooms().contains(classRoom)) {
            return new BasicResponse(false, "המורה " + teacher.getUsername() + " אינו משויך לכיתה " + classRoom.getName());
        }
        if (!teacher.getTeachingSubjects().contains(draft.getSubject())) {
            return new BasicResponse(false, "המורה " + teacher.getUsername() + " אינו מלמד את המקצוע " + draft.getSubject());
        }

        List<TimetableConflictDTO> conflicts = findConflicts(draft, timetableRepository.findSchoolLessons(schoolCode));
        if (!conflicts.isEmpty()) {
            BasicResponse<List<TimetableConflictDTO>> response = new BasicResponse<>(false,
                    "לא ניתן לשבץ: " + conflicts.get(0).getMessage());
            response.setData(conflicts);
            return response;
        }

        LessonEntity lesson;
        String previousTeacherId = null;
        ClassRoomEntity previousClass = null;
        if (draft.getId() != null) {
            lesson = timetableRepository.findWithRelations(draft.getId());
            if (lesson == null || !schoolCode.equals(lesson.getSchedule().getClassRoom().getSchool().getSchoolCode())) {
                return new BasicResponse(false, "השיעור לא נמצא");
            }
            previousTeacherId = lesson.getTeacher().getUserId();
            previousClass = lesson.getSchedule().getClassRoom();
        } else {
            lesson = new LessonEntity();
        }

        ScheduleEntity schedule = classRoom.getSchedule();
        if (schedule == null) {
            schedule = new ScheduleEntity();
            schedule.setClassRoom(classRoom);
            schedule = scheduleRepository.save(schedule);
            classRoom.setSchedule(schedule);
        }

        lesson.setSubject(draft.getSubject());
        lesson.setDayOfWeek(draft.getDayOfWeek());
        lesson.setStartTime(LocalTime.parse(draft.getStartTime()));
        lesson.setEndTime(LocalTime.parse(draft.getEndTime()));
        lesson.setRoom(blankToNull(draft.getRoom()));
        lesson.setTeacher(teacher);
        if (lesson.getSchedule() == null || !lesson.getSchedule().getId().equals(schedule.getId())) {
            lesson.setSchedule(schedule);
            schedule.getLessons().add(lesson);
        }
        LessonEntity saved = timetableRepository.save(lesson);

        evictFor(schoolCode, teacher.getUserId(), classRoom);
        if (previousTeacherId != null && !previousTeacherId.equals(teacher.getUserId())) {
            cacheEvictor.teacherSchedule(previousTeacherId);
        }
        if (previousClass != null && !previousClass.getId().equals(classRoom.getId())) {
            evictFor(schoolCode, null, previousClass);
        }

        BasicResponse<TimetableLessonDTO> response = new BasicResponse<>(true,
                draft.getId() == null ? "השיעור נוסף למערכת" : "השיעור עודכן");
        response.setData(new TimetableLessonDTO(saved.getId(), saved.getSubject(), saved.getDayOfWeek(),
                saved.getStartTime(), saved.getEndTime(), classRoom.getName(), teacher.getUserId(),
                teacher.getUsername(), saved.getRoom()));
        return response;
    }

    @Transactional
    public BasicResponse deleteLesson(String schoolCode, Long lessonId) {
        LessonEntity lesson = lessonId == null ? null : timetableRepository.findWithRelations(lessonId);
        if (lesson == null || !schoolCode.equals(lesson.getSchedule().getClassRoom().getSchool().getSchoolCode())) {
            return new BasicResponse(false, "השיעור לא נמצא");
        }
        ClassRoomEntity classRoom = lesson.getSchedule().getClassRoom();
        TeacherEntity teacher = lesson.getTeacher();
        teacher.getLessons().remove(lesson);
        lesson.getSchedule().getLessons().remove(lesson); // orphanRemoval deletes the row

        evictFor(schoolCode, teacher.getUserId(), classRoom);
        return new BasicResponse(true, "השיעור הוסר מהמערכת");
    }

    private void evictFor(String schoolCode, String teacherId, ClassRoomEntity classRoom) {
        if (teacherId != null) {
            cacheEvictor.teacherSchedule(teacherId);
        }
        cacheEvictor.classRoomSchedule(schoolCode, classRoom.getName());
        for (StudentEntity student : classRoom.getStudents()) {
            cacheEvictor.studentSchedule(student.getUserId());
        }
    }

    /** A Hebrew error for a malformed draft, or null when it is well formed. */
    static String validateDraft(TimetableLessonDTO draft) {
        if (draft == null || isBlank(draft.getSubject()) || draft.getDayOfWeek() == null
                || isBlank(draft.getStartTime()) || isBlank(draft.getEndTime())
                || isBlank(draft.getClassRoomName()) || isBlank(draft.getTeacherId())) {
            return "יש למלא מורה, כיתה, מקצוע, יום ושעות";
        }
        if (draft.getDayOfWeek() == DayOfWeek.SATURDAY) {
            return "לא ניתן לשבץ שיעור ביום שבת";
        }
        try {
            LocalTime start = LocalTime.parse(draft.getStartTime());
            LocalTime end = LocalTime.parse(draft.getEndTime());
            if (!end.isAfter(start)) {
                return "שעת הסיום חייבת להיות אחרי שעת ההתחלה";
            }
        } catch (DateTimeParseException e) {
            return "פורמט שעה לא תקין (HH:mm)";
        }
        return null;
    }

    /**
     * Conflicts between a draft lesson and the school's existing lessons (the draft's own id is skipped).
     * Lessons that only touch (one ends when the other starts) do not conflict.
     */
    static List<TimetableConflictDTO> findConflicts(TimetableLessonDTO draft, List<TimetableLessonDTO> existing) {
        List<TimetableConflictDTO> conflicts = new ArrayList<>();
        LocalTime start = LocalTime.parse(draft.getStartTime());
        LocalTime end = LocalTime.parse(draft.getEndTime());

        for (TimetableLessonDTO other : existing) {
            if (draft.getId() != null && draft.getId().equals(other.getId())) {
                continue;
            }
            if (other.getDayOfWeek() != draft.getDayOfWeek()) {
                continue;
            }
            LocalTime otherStart = LocalTime.parse(other.getStartTime());
            LocalTime otherEnd = LocalTime.parse(other.getEndTime());
            if (!(start.isBefore(otherEnd) && end.isAfter(otherStart))) {
                continue;
            }
            String when = "ביום " + DAY_NAMES.get(other.getDayOfWeek()) + " " + other.getStartTime() + "-" + other.getEndTime();

            if (draft.getTeacherId().equals(other.getTeacherId())) {
                conflicts.add(new TimetableConflictDTO("TEACHER",
                        "המורה " + other.getTeacherName() + " כבר מלמד/ת " + other.getSubject()
                                + " בכיתה " + other.getClassRoomName() + " " + when,
                        other.getId()));
            }
            if (draft.getClassRoomName().equals(other.getClassRoomName())) {
                conflicts.add(new TimetableConflictDTO("CLASS",
                        "לכיתה " + other.getClassRoomName() + " כבר יש שיעור " + other.getSubject()
                                + " עם " + other.getTeacherName() + " " + when,
                        other.getId()));
            }
            if (!isBlank(draft.getRoom()) && !isBlank(other.getRoom())
                    && draft.getRoom().trim().equalsIgnoreCase(other.getRoom().trim())) {
                conflicts.add(new TimetableConflictDTO("ROOM",
                        "החדר " + other.getRoom() + " תפוס על ידי כיתה " + other.getClassRoomName()
                                + " (" + other.getSubject() + ") " + when,
                        other.getId()));
            }
        }
        return conflicts;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
