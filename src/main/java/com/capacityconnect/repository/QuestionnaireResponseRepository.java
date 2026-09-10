package com.capacityconnect.repository;

import com.capacityconnect.entity.QuestionnaireResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionnaireResponseRepository
        extends JpaRepository<QuestionnaireResponse, Long> {

    List<QuestionnaireResponse> findByQuestionnaireId(Long questionnaireId);

    List<QuestionnaireResponse> findByTraineeId(Long traineeId);

    List<QuestionnaireResponse> findByQuestionnaireIdAndTraineeId(
            Long questionnaireId,
            Long traineeId);
}
