package com.mozip.mozip.domain.applicant.service;

import com.mozip.mozip.domain.paperAnswer.dto.PaperAnswerForApplicantDto;
import com.mozip.mozip.domain.paperAnswer.dto.PaperAnswersForApplicantResDto;
import com.mozip.mozip.domain.paperAnswer.entity.PaperAnswer;
import com.mozip.mozip.domain.paperAnswer.repository.PaperAnswerRepository;
import com.mozip.mozip.domain.applicant.dto.*;
import com.mozip.mozip.domain.applicant.entity.Applicant;
import com.mozip.mozip.domain.applicant.exception.ApplicantNotFoundException;
import com.mozip.mozip.domain.applicant.repository.ApplicantRepository;
import com.mozip.mozip.domain.mozip.entity.Mozip;
import com.mozip.mozip.domain.club.exception.MozipNotFoundException;
import com.mozip.mozip.domain.mozip.repository.MozipRepository;
import com.mozip.mozip.domain.evaluation.entity.Evaluation;
import com.mozip.mozip.domain.evaluation.entity.PaperEvaluation;
import com.mozip.mozip.domain.evaluation.exception.EvaluationNotFoundException;
import com.mozip.mozip.domain.evaluation.exception.PaperEvaluationNotFoundException;
import com.mozip.mozip.domain.evaluation.repository.EvaluationRepository;
import com.mozip.mozip.domain.evaluation.repository.PaperEvaluationRepository;
import com.mozip.mozip.domain.paperQuestion.dto.PaperQuestionWithAnswersDto;
import com.mozip.mozip.domain.paperQuestion.entity.PaperQuestion;
import com.mozip.mozip.domain.paperQuestion.repository.PaperQuestionRepository;
import com.mozip.mozip.domain.user.entity.User;
import com.mozip.mozip.domain.user.exception.UserNotFoundException;
import com.mozip.mozip.domain.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicantService {
    private final UserRepository userRepository;
    private final ApplicantRepository applicantRepository;
    private final MozipRepository mozipRepository;
    private final PaperQuestionRepository paperQuestionRepository;
    private final PaperAnswerRepository paperAnswerRepository;
    private final EvaluationRepository evaluationRepository;
    private final PaperEvaluationRepository paperEvaluationRepository;

    public Applicant getApplicantById(String applicantId) {
        return applicantRepository.findById(applicantId)
                .orElseThrow(() -> new EntityNotFoundException("Applicant가 없습니다 : " + applicantId));
    }

    public ApplicantListResponse getApplicantListByMozipId(String mozipId, String sortBy, String order) {
        Mozip mozip = mozipRepository.findById(mozipId)
                .orElseThrow(() -> new MozipNotFoundException(mozipId));

        List<Applicant> applicants = applicantRepository.findApplicantsByMozip(mozip);

        List<ApplicationDto> applicationDtos = applicants.stream()
                .map(applicant -> {
                    List<Evaluation> evaluations = evaluationRepository.findByApplicant(applicant);
                    int totalPaperScore = evaluations.stream()
                            .mapToInt(Evaluation::getPaperScore)
                            .sum();
                    return ApplicationDto.from(applicant, totalPaperScore);
                })
                .toList();

        return ApplicantListResponse.from(applicationDtos, mozip);
    }

    public PaperAnswersForApplicantResDto getPaperAnswersByMozipId(String userId, String mozipId, String applicantId, String questionId) {
        User evaluator = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        List<PaperQuestion> paperQuestions = paperQuestionRepository.findByMozipId(mozipId).stream()
                .filter(question -> questionId == null || Arrays.asList(questionId.split(",")).contains(question.getId()))
                .toList();

        List<PaperQuestionWithAnswersDto> questionWithAnswersDtos = paperQuestions.stream()
                .map(question -> {
                    List<PaperAnswer> paperAnswers = paperAnswerRepository.findByPaperQuestionId(question.getId())
                            .stream()
                            .filter(answer -> applicantId == null || Arrays.asList(applicantId.split(",")).contains(answer.getApplicant().getId()))
                            .toList();

                    List<PaperAnswerForApplicantDto> paperAnswerForApplicantDtos = paperAnswers.stream()
                            .map(answer -> {
                                Evaluation evaluation = evaluationRepository.findByEvaluatorAndApplicant(
                                        evaluator,
                                        answer.getApplicant()
                                ).orElseThrow(() -> new EvaluationNotFoundException(evaluator.getId(), answer.getApplicant().getId()));

                                PaperEvaluation paperEvaluation = paperEvaluationRepository.findByPaperAnswerAndEvaluation(
                                        answer,
                                        evaluation
                                ).orElseThrow(() -> new PaperEvaluationNotFoundException(evaluation.getId(), answer.getId()));

                                return PaperAnswerForApplicantDto.from(answer, paperEvaluation.getScore());
                            })
                            .toList();

                    return PaperQuestionWithAnswersDto.from(question, paperAnswerForApplicantDtos);
                })
                .toList();

        return PaperAnswersForApplicantResDto.from(questionWithAnswersDtos);
    }

    public UpdateApplicantStatusResponse updateApplicantPaperStatuses(String mozipId, UpdateApplicantStatusRequest request) {
        Mozip mozip = mozipRepository.findById(mozipId)
                .orElseThrow(() -> new MozipNotFoundException(mozipId));

        request.getApplicants().forEach(each -> {
            Applicant applicant = applicantRepository.findByIdAndMozip(each.getApplicantId(), mozip)
                    .orElseThrow(() -> new ApplicantNotFoundException(each.getApplicantId()));

            applicant.setPaperStatus(each.getStatus());
            applicantRepository.save(applicant);
        });

        return UpdateApplicantStatusResponse.builder()
                .timestamp(Instant.now().toString())
                .build();
    }
}