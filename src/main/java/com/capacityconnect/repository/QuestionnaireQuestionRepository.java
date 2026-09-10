package com.capacityconnect.repository;

import com.capacityconnect.entity.QuestionnaireQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionnaireQuestionRepository
        extends JpaRepository<QuestionnaireQuestion, Long> {

    List<QuestionnaireQuestion> findByQuestionnaireId(Long questionnaireId);
}
