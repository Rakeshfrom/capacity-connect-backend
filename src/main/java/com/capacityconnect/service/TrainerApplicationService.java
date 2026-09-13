package com.capacityconnect.service;

import com.capacityconnect.dto.*;
import com.capacityconnect.entity.TrainerApplication;
import com.capacityconnect.entity.User;
import com.capacityconnect.repository.TrainerApplicationRepository;
import com.capacityconnect.repository.TrainerProfileRepository;
import com.capacityconnect.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class TrainerApplicationService {
    private static final int PASSING_SCORE = 70;
    private final TrainerApplicationRepository repository;
    private final UserRepository userRepository;
    private final TrainerProfileRepository trainerProfileRepository;
    private final CurrentUserService currentUserService;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TrainerApplicationService(TrainerApplicationRepository repository,
                                     UserRepository userRepository,
                                     TrainerProfileRepository trainerProfileRepository,
                                     CurrentUserService currentUserService,
                                     FileStorageService fileStorageService) {
        this.repository=repository; this.userRepository=userRepository;
        this.trainerProfileRepository=trainerProfileRepository;
        this.currentUserService=currentUserService; this.fileStorageService=fileStorageService;
    }

    public TrainerApplicationResponse submit(Authentication authentication,
                                             TrainerApplicationRequest request,
                                             MultipartFile cv) {
        User user=currentUserService.getCurrentUser(authentication);
        if (user.getRoles()!=null && user.getRoles().contains(User.Role.TRAINER)) throw new IllegalStateException("User is already a trainer");
        if (request.getQualification()==null || request.getQualification().isBlank()) throw new IllegalArgumentException("Qualification is required");
        if (request.getExperienceYears()==null || request.getExperienceYears()<0) throw new IllegalArgumentException("Valid experience is required");
        if (cv==null || cv.isEmpty()) throw new IllegalArgumentException("CV is required");
        String type=cv.getContentType();
        boolean allowed="application/pdf".equalsIgnoreCase(type)
                || "application/msword".equalsIgnoreCase(type)
                || "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equalsIgnoreCase(type);
        if(!allowed) throw new IllegalArgumentException("CV must be a PDF, DOC or DOCX file");
        if(cv.getSize()>10*1024*1024) throw new IllegalArgumentException("CV must be smaller than 10 MB");

        TrainerApplication existing=repository.findByUserId(user.getId()).orElse(null);
        if(existing!=null && existing.getStatus()!=TrainerApplication.Status.REJECTED) throw new IllegalStateException("Trainer application is already in progress");
        if(existing!=null && existing.getStatus()==TrainerApplication.Status.APPROVED) throw new IllegalStateException("Trainer application is already approved");

        user.setQualifications(request.getQualification().trim());
        user.setExperienceYears(request.getExperienceYears());
        userRepository.save(user);
        String key=fileStorageService.store(cv,user.getId());

        TrainerApplication app=existing!=null?existing:TrainerApplication.builder().userId(user.getId()).build();
        app.setReason(request.getReason()); app.setSupportingDocumentUrl(request.getSupportingDocumentUrl());
        app.setSupportingDocumentKey(key); app.setStatus(TrainerApplication.Status.PENDING); app.setAdminComment(null); app.setReviewedAt(null);
        app.setAssessmentJson(null); app.setAssessmentScore(null); app.setAssessmentPassed(false); app.setAssessmentAssignedAt(null); app.setAssessmentCompletedAt(null);
        return toResponse(repository.save(app));
    }

    public TrainerApplicationResponse getMyApplication(Authentication authentication){
        User user=currentUserService.getCurrentUser(authentication);
        return repository.findByUserId(user.getId()).map(this::toResponse).orElse(null);
    }

    public TrainerAssessmentResponse getMyAssessment(Authentication authentication){
        User user=currentUserService.getCurrentUser(authentication);
        TrainerApplication app=repository.findByUserId(user.getId()).orElseThrow(()->new IllegalStateException("Trainer application not found"));
        if(app.getStatus()!=TrainerApplication.Status.ASSESSMENT_REQUIRED && app.getStatus()!=TrainerApplication.Status.ASSESSMENT_SUBMITTED) throw new IllegalStateException("Trainer assessment is not available");
        try{
            List<Map<String,Object>> stored=objectMapper.readValue(app.getAssessmentJson(),new TypeReference<>(){});
            List<Map<String,Object>> safe=new ArrayList<>();
            for(Map<String,Object> q:stored){Map<String,Object> x=new LinkedHashMap<>(); x.put("id",String.valueOf(q.get("id"))); x.put("questionText",q.get("questionText")); x.put("optionA",q.get("optionA")); x.put("optionB",q.get("optionB")); x.put("optionC",q.get("optionC")); x.put("optionD",q.get("optionD")); safe.add(x);} 
            return TrainerAssessmentResponse.builder().status(app.getStatus().name()).score(app.getAssessmentScore()).passed(app.isAssessmentPassed()).questions(app.getStatus()==TrainerApplication.Status.ASSESSMENT_REQUIRED?safe:List.of()).build();
        }catch(Exception e){throw new IllegalStateException("Unable to load trainer assessment",e);}
    }

    public TrainerAssessmentResponse submitMyAssessment(Authentication authentication, TrainerAssessmentSubmissionRequest request){
        User user=currentUserService.getCurrentUser(authentication);
        TrainerApplication app=repository.findByUserId(user.getId()).orElseThrow(()->new IllegalStateException("Trainer application not found"));
        if(app.getStatus()!=TrainerApplication.Status.ASSESSMENT_REQUIRED) throw new IllegalStateException("Trainer assessment cannot be submitted");
        if(request==null || request.getAnswers()==null) throw new IllegalArgumentException("Assessment answers are required");
        try{
            List<Map<String,Object>> qs=objectMapper.readValue(app.getAssessmentJson(),new TypeReference<>(){});
            int total=0, earned=0;
            for(int i=0;i<qs.size();i++){
                Map<String,Object> q=qs.get(i); String id=String.valueOf(q.getOrDefault("id",i+1)); int marks=Integer.parseInt(String.valueOf(q.getOrDefault("marks",1))); total+=marks;
                if(String.valueOf(q.getOrDefault("correctOption","")).equalsIgnoreCase(String.valueOf(request.getAnswers().get(id)))) earned+=marks;
            }
            int score=total==0?0:Math.round(earned*100f/total); boolean passed=score>=PASSING_SCORE;
            app.setAssessmentScore(score); app.setAssessmentPassed(passed); app.setAssessmentCompletedAt(LocalDateTime.now()); app.setStatus(TrainerApplication.Status.ASSESSMENT_SUBMITTED);
            repository.save(app);
            return TrainerAssessmentResponse.builder().status(app.getStatus().name()).score(score).passed(passed).questions(List.of()).build();
        }catch(Exception e){throw new IllegalStateException("Unable to evaluate trainer assessment",e);}
    }

    public TrainerApplicationResponse assignAssessment(Long id,int questionCount,String difficulty,AiChatService ai){
        TrainerApplication app=repository.findById(id).orElseThrow(()->new IllegalArgumentException("Trainer application not found: "+id));
        if(app.getStatus()!=TrainerApplication.Status.PENDING) throw new IllegalStateException("Assessment can only be assigned to a pending application");
        if(questionCount<5||questionCount>20) throw new IllegalArgumentException("Question count must be between 5 and 20");
        User u=userRepository.findById(app.getUserId()).orElseThrow(()->new IllegalArgumentException("User not found: "+app.getUserId()));
        String ctx=("Candidate trainer\nCurrent role: %s\nTarget role: TRAINER\nDepartment: %s\nQualification: %s\nExperience years: %s\nSkills: %s\nInterests: %s\nApplication reason: %s\nCreate questions that evaluate subject knowledge, instructional ability, judgement, communication and role readiness.")
                .formatted(u.getRole(),v(u.getDepartment()),v(u.getQualifications()),u.getExperienceYears()==null?"Not provided":u.getExperienceYears(),v(u.getSkills()),v(u.getInterests()),v(app.getReason()));
        String raw=ai.generateAssessmentQuestions("Trainer role competency assessment",ctx,questionCount,difficulty==null?"MEDIUM":difficulty);
        try{
            Map<String,Object> parsed=objectMapper.readValue(raw,new TypeReference<>(){});
            List<Map<String,Object>> qs=objectMapper.convertValue(parsed.get("questions"),new TypeReference<List<Map<String,Object>>>(){});
            if(qs==null||qs.size()<5) throw new IllegalStateException("AI did not generate enough assessment questions");
            for(int i=0;i<qs.size();i++) qs.get(i).put("id",String.valueOf(i+1));
            app.setAssessmentJson(objectMapper.writeValueAsString(qs)); app.setAssessmentScore(null); app.setAssessmentPassed(false); app.setAssessmentAssignedAt(LocalDateTime.now()); app.setAssessmentCompletedAt(null); app.setStatus(TrainerApplication.Status.ASSESSMENT_REQUIRED);
            return toResponse(repository.save(app));
        }catch(Exception e){throw new IllegalStateException("Unable to store AI trainer assessment",e);}
    }


    public List<TrainerApplicationResponse> getAllApplications() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<TrainerApplicationResponse> getPendingApplications() {
        return repository
                .findByStatus(TrainerApplication.Status.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TrainerApplicationResponse getApplication(Long id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Trainer application not found: " + id));
    }

    public TrainerApplicationResponse reviewApplication(Long id, TrainerApplicationReviewRequest request){
        TrainerApplication app=repository.findById(id).orElseThrow(()->new IllegalArgumentException("Trainer application not found: "+id));
        if(request.getStatus()==TrainerApplication.Status.REJECTED){app.setStatus(TrainerApplication.Status.REJECTED);app.setAdminComment(request.getAdminComment());app.setReviewedAt(LocalDateTime.now());return toResponse(repository.save(app));}
        if(request.getStatus()!=TrainerApplication.Status.APPROVED) throw new IllegalArgumentException("Review status must be APPROVED or REJECTED");
        if(app.getStatus()!=TrainerApplication.Status.ASSESSMENT_SUBMITTED || !app.isAssessmentPassed()) throw new IllegalStateException("Trainer must pass the assessment before approval");
        app.setStatus(TrainerApplication.Status.APPROVED);app.setAdminComment(request.getAdminComment());app.setReviewedAt(LocalDateTime.now());return toResponse(repository.save(app));
    }

    private String v(String x){return x==null||x.isBlank()?"Not provided":x;}
    private TrainerApplicationResponse toResponse(TrainerApplication a){return TrainerApplicationResponse.builder().id(a.getId()).userId(a.getUserId()).reason(a.getReason()).supportingDocumentUrl(a.getSupportingDocumentUrl()).supportingDocumentKey(a.getSupportingDocumentKey()).status(a.getStatus()).adminComment(a.getAdminComment()).submittedAt(a.getSubmittedAt()).reviewedAt(a.getReviewedAt()).assessmentScore(a.getAssessmentScore()).assessmentPassed(a.isAssessmentPassed()).assessmentAssignedAt(a.getAssessmentAssignedAt()).assessmentCompletedAt(a.getAssessmentCompletedAt()).build();}
}
