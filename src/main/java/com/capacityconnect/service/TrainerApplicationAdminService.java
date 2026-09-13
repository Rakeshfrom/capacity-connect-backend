package com.capacityconnect.service;

import com.capacityconnect.dto.TrainerApplicationResponse;
import com.capacityconnect.entity.TrainerApplication;
import com.capacityconnect.entity.TrainerProfile;
import com.capacityconnect.entity.User;
import com.capacityconnect.exception.ResourceNotFoundException;
import com.capacityconnect.repository.TrainerApplicationRepository;
import com.capacityconnect.repository.TrainerProfileRepository;
import com.capacityconnect.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Service
public class TrainerApplicationAdminService {
    private final TrainerApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final TrainerProfileRepository trainerProfileRepository;
    private final KeycloakRoleService keycloakRoleService;
    private final AiChatService aiChatService;

    public TrainerApplicationAdminService(TrainerApplicationRepository applicationRepository,
                                          UserRepository userRepository,
                                          TrainerProfileRepository trainerProfileRepository,
                                          KeycloakRoleService keycloakRoleService,
                                          AiChatService aiChatService) {
        this.applicationRepository=applicationRepository;this.userRepository=userRepository;this.trainerProfileRepository=trainerProfileRepository;this.keycloakRoleService=keycloakRoleService;this.aiChatService=aiChatService;
    }

    public List<TrainerApplicationResponse> getPendingApplications(){return applicationRepository.findAll().stream().filter(a->a.getStatus()!=TrainerApplication.Status.APPROVED&&a.getStatus()!=TrainerApplication.Status.REJECTED).map(this::toResponse).toList();}

    public TrainerApplicationResponse assignAssessment(Long id,String difficulty,int count){
        TrainerApplication app=find(id); if(app.getStatus()!=TrainerApplication.Status.PENDING) throw new IllegalStateException("Assessment can only be assigned to pending applications");
        User u=userRepository.findById(app.getUserId()).orElseThrow(()->new ResourceNotFoundException("User not found: "+app.getUserId()));
        String ctx="Candidate trainer\nCurrent role: "+u.getRole()+"\nDepartment: "+v(u.getDepartment())+"\nQualification: "+v(u.getQualifications())+"\nExperience years: "+(u.getExperienceYears()==null?"Not provided":u.getExperienceYears())+"\nSkills: "+v(u.getSkills())+"\nInterests: "+v(u.getInterests())+"\nApplication reason: "+v(app.getReason())+"\nAssess knowledge, instructional ability, judgement, communication and trainer readiness.";
        String raw=aiChatService.generateAssessmentQuestions("Trainer role competency assessment",ctx,count,difficulty==null?"MEDIUM":difficulty);
        try{
            var mapper=new com.fasterxml.jackson.databind.ObjectMapper(); var parsed=mapper.readTree(raw); var qs=parsed.path("questions"); if(!qs.isArray()||qs.size()<5) throw new IllegalStateException("AI did not generate enough assessment questions");
            for(int i=0;i<qs.size();i++)((com.fasterxml.jackson.databind.node.ObjectNode)qs.get(i)).put("id",String.valueOf(i+1));
            app.setAssessmentJson(mapper.writeValueAsString(mapper.convertValue(qs,new com.fasterxml.jackson.core.type.TypeReference<List<java.util.Map<String,Object>>>(){})));
            app.setAssessmentScore(null);app.setAssessmentPassed(false);app.setAssessmentAssignedAt(LocalDateTime.now());app.setAssessmentCompletedAt(null);app.setStatus(TrainerApplication.Status.ASSESSMENT_REQUIRED);
            return toResponse(applicationRepository.save(app));
        }catch(Exception e){throw new IllegalStateException("Unable to store AI trainer assessment",e);}
    }

    public TrainerApplicationResponse approve(Long id,String comment){
        TrainerApplication app=find(id); if(app.getStatus()!=TrainerApplication.Status.ASSESSMENT_SUBMITTED||!app.isAssessmentPassed()) throw new IllegalStateException("Trainer must pass the assessment before approval");
        User user=userRepository.findById(app.getUserId()).orElseThrow(()->new ResourceNotFoundException("User not found: "+app.getUserId()));
        if(user.getRoles()==null)user.setRoles(new HashSet<>());user.getRoles().add(User.Role.TRAINER);user.setRole(User.Role.TRAINER);userRepository.save(user);
        keycloakRoleService.assignTrainerRoleByEmail(user.getEmail());
        if(!trainerProfileRepository.existsByTrainerId(user.getId())) trainerProfileRepository.save(TrainerProfile.builder().trainerId(user.getId()).department(user.getDepartment()).experienceYears(user.getExperienceYears()).qualifications(user.getQualifications()).build());
        app.setStatus(TrainerApplication.Status.APPROVED);app.setAdminComment(comment);app.setReviewedAt(LocalDateTime.now());return toResponse(applicationRepository.save(app));
    }

    public TrainerApplicationResponse reject(Long id,String comment){TrainerApplication app=find(id);if(app.getStatus()==TrainerApplication.Status.APPROVED)throw new IllegalStateException("Approved applications cannot be rejected");app.setStatus(TrainerApplication.Status.REJECTED);app.setAdminComment(comment);app.setReviewedAt(LocalDateTime.now());return toResponse(applicationRepository.save(app));}
    private TrainerApplication find(Long id){return applicationRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Trainer application not found: "+id));}
    private String v(String x){return x==null||x.isBlank()?"Not provided":x;}
    private TrainerApplicationResponse toResponse(TrainerApplication a){return TrainerApplicationResponse.builder().id(a.getId()).userId(a.getUserId()).reason(a.getReason()).supportingDocumentUrl(a.getSupportingDocumentUrl()).supportingDocumentKey(a.getSupportingDocumentKey()).status(a.getStatus()).adminComment(a.getAdminComment()).submittedAt(a.getSubmittedAt()).reviewedAt(a.getReviewedAt()).assessmentScore(a.getAssessmentScore()).assessmentPassed(a.isAssessmentPassed()).assessmentAssignedAt(a.getAssessmentAssignedAt()).assessmentCompletedAt(a.getAssessmentCompletedAt()).build();}
}
