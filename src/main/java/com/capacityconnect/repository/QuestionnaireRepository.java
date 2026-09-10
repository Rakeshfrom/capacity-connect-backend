package com.capacityconnect.repository;

import com.capacityconnect.entity.Questionnaire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionnaireRepository extends JpaRepository<Questionnaire, Long> {

    List<Questionnaire> findByTrainerId(Long trainerId);

    List<Questionnaire> findByCourseId(Long courseId);
}
