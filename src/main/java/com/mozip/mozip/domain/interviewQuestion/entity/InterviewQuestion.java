package com.mozip.mozip.domain.interviewQuestion.entity;

import com.mozip.mozip.domain.mozip.entity.Mozip;
import com.mozip.mozip.global.entity.BaseTime;
import de.huxhorn.sulky.ulid.ULID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewQuestion extends BaseTime {
    @Id
    @Column(name = "interview_question_id")
    @Builder.Default
    private final String id = new ULID().nextULID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mozip_id", nullable = false)
    private Mozip mozip;

    @Column(nullable = false)
    private String question;

    private Long questionNo; // 질문지 내에서의 질문 번호

    @Lob
    private String details;

    @Column(nullable = false)
    private boolean isRequired;

    public void updateQuestion(String question, String details, boolean isRequired) {
        this.question = question;
        this.details = details;
        this.isRequired = isRequired;
    }
}