package com.capacityconnect.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "questionnaire_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionnaireQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "questionnaire_id", nullable = false)
    private Long questionnaireId;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "option_a")
    private String optionA;

    @Column(name = "option_b")
    private String optionB;

    @Column(name = "option_c")
    private String optionC;

    @Column(name = "option_d")
    private String optionD;

    @Column(name = "question_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private QuestionType questionType = QuestionType.MCQ;

    public enum QuestionType {
        MCQ, TEXT
    }
}
