package example.example.grading_engine.controller;

import example.example.grading_engine.dto.AddAcademicSessionRequest;
import example.example.grading_engine.dto.AddAcademicSessionResponse;
import example.example.grading_engine.service.impl.AcademicService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hod/academic")
public class AcademicController {

    private final AcademicService academicService;

    public AcademicController(AcademicService academicService) {
        this.academicService = academicService;
    }

    // CREATE
    @PostMapping("/addAcademicSession")
    public ResponseEntity<AddAcademicSessionResponse> addAcademicSession(
            @Valid @RequestBody AddAcademicSessionRequest request
    ) {

        AddAcademicSessionResponse response =
                academicService.addAcademicSession(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // GET ALL
    @GetMapping("/getAcademicSessions")
    public ResponseEntity<List<AddAcademicSessionResponse>>
    getAcademicSessions() {

        return ResponseEntity.ok(
                academicService.getAllAcademicSessions()
        );
    }

    // GET BY ID
    @GetMapping("/getAcademicSession/{id}")
    public ResponseEntity<AddAcademicSessionResponse>
    getAcademicSession(
            @PathVariable UUID id
    ) {

        return ResponseEntity.ok(
                academicService.getAcademicSession(id)
        );
    }

    // UPDATE
    @PutMapping("/updateAcademicSession/{id}")
    public ResponseEntity<AddAcademicSessionResponse>
    updateAcademicSession(
            @PathVariable UUID id,
            @Valid @RequestBody AddAcademicSessionRequest request
    ) {

        return ResponseEntity.ok(
                academicService.updateAcademicSession(
                        id,
                        request
                )
        );
    }

    // DELETE
    @DeleteMapping("/deleteAcademicSession/{id}")
    public ResponseEntity<Void> deleteAcademicSession(
            @PathVariable UUID id
    ) {

        academicService.deleteAcademicSession(id);

        return ResponseEntity.noContent().build();
    }
}