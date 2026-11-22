package org.example.learningprojectserver.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("TEACHER")
public class TeacherTestEntity extends TestEntity {



    @ManyToMany
    @JoinTable(
            name = "teacher_test_student",
            joinColumns = @JoinColumn(name = "test_id"),
            inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    private List<StudentEntity> students = new ArrayList<>();


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private TeacherEntity teacher;

    // Exam authoring / publishing. Nullable so rows created before these columns keep working:
    // published == null means published, template == null means not a template.
    private String title;
    private Boolean published;
    private LocalDateTime publishAt;
    private Boolean template;
    private String versionLabel;
    private String examGroupId;
    private Long sourceTestId;
    private Boolean shuffleQuestions;
    private Boolean shuffleOptions;

    public TeacherTestEntity() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Boolean getPublished() { return published; }
    public void setPublished(Boolean published) { this.published = published; }

    public LocalDateTime getPublishAt() { return publishAt; }
    public void setPublishAt(LocalDateTime publishAt) { this.publishAt = publishAt; }

    public Boolean getTemplate() { return template; }
    public void setTemplate(Boolean template) { this.template = template; }

    public String getVersionLabel() { return versionLabel; }
    public void setVersionLabel(String versionLabel) { this.versionLabel = versionLabel; }

    public String getExamGroupId() { return examGroupId; }
    public void setExamGroupId(String examGroupId) { this.examGroupId = examGroupId; }

    public Long getSourceTestId() { return sourceTestId; }
    public void setSourceTestId(Long sourceTestId) { this.sourceTestId = sourceTestId; }

    public Boolean getShuffleQuestions() { return shuffleQuestions; }
    public void setShuffleQuestions(Boolean shuffleQuestions) { this.shuffleQuestions = shuffleQuestions; }

    public Boolean getShuffleOptions() { return shuffleOptions; }
    public void setShuffleOptions(Boolean shuffleOptions) { this.shuffleOptions = shuffleOptions; }

    public boolean isPublishedNow() { return published == null || published; }
    public boolean isTemplateExam() { return Boolean.TRUE.equals(template); }




    public List<StudentEntity> getStudents() {
        return students;
    }

    public void setStudents(List<StudentEntity> students) {
        this.students = students;
    }

    public TeacherEntity getTeacher() {
        return teacher;
    }

    public void setTeacher(TeacherEntity teacher) {
        this.teacher = teacher;
    }
}
