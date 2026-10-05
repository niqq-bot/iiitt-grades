package example.example.grading_engine.service.impl;

import example.example.grading_engine.dto.AddAcademicSessionRequest;
import example.example.grading_engine.dto.AddAcademicSessionResponse;
import example.example.grading_engine.model.entity.AcademicSession;
import example.example.grading_engine.repository.AcademicRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class AcademicService {

    private final AcademicRepository academicRepository;

    public AcademicService(AcademicRepository academicRepository) {
        this.academicRepository = academicRepository;
    }

    // CREATE
    public AddAcademicSessionResponse addAcademicSession(
            AddAcademicSessionRequest request) {

        if (academicRepository.existsByAcademicYearAndSemester(
                request.academicYear(),
                request.semester())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Academic session already exists"
            );
        }

        AcademicSession academicSession = AcademicSession.builder()
                .id(UUID.randomUUID())
                .academicYear(request.academicYear())
                .semester(request.semester())
                .regulationVersion(request.regulationVersion())
                .gradingPolicyVer(request.gradingPolicyVersion())
                .build();

        AcademicSession saved =
                academicRepository.save(academicSession);

        return toResponse(saved);
    }

    // GET ALL
    public List<AddAcademicSessionResponse> getAllAcademicSessions() {

        return academicRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // GET BY ID
    public AddAcademicSessionResponse getAcademicSession(UUID id) {

        AcademicSession academicSession =
                academicRepository.findById(id)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Academic session not found"
                                )
                        );

        return toResponse(academicSession);
    }

    // UPDATE
    public AddAcademicSessionResponse updateAcademicSession(
            UUID id,
            AddAcademicSessionRequest request) {

        AcademicSession academicSession =
                academicRepository.findById(id)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Academic session not found"
                                )
                        );

        if (academicRepository.existsByAcademicYearAndSemesterAndIdNot(
                request.academicYear(),
                request.semester(),
                id)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Academic session already exists"
            );
        }

        academicSession.setAcademicYear(
                request.academicYear()
        );

        academicSession.setSemester(
                request.semester()
        );

        academicSession.setRegulationVersion(
                request.regulationVersion()
        );

        academicSession.setGradingPolicyVer(
                request.gradingPolicyVersion()
        );

        AcademicSession updated =
                academicRepository.save(academicSession);

        return toResponse(updated);
    }

    // DELETE
    public void deleteAcademicSession(UUID id) {

        if (!academicRepository.existsById(id)) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Academic session not found"
            );
        }

        academicRepository.deleteById(id);
    }

    // ENTITY -> RESPONSE DTO
    private AddAcademicSessionResponse toResponse(
            AcademicSession academicSession) {

        return new AddAcademicSessionResponse(
                academicSession.getId(),
                academicSession.getAcademicYear(),
                academicSession.getSemester(),
                academicSession.getRegulationVersion(),
                academicSession.getGradingPolicyVer()
        );
    }
}