package example.example.grading_engine.controller;

import example.example.grading_engine.enums.marksvalidation.MarkComponentType;
import example.example.grading_engine.model.entity.*;
import example.example.grading_engine.repository.*;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/faculty/marks")
public class MarksUploadController {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final AcademicRepository academicRepository;
    private final FacultyAssignmentRepository facultyAssignmentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final MarksRepository marksRepository;

    public MarksUploadController(
            UserRepository userRepository,
            StudentRepository studentRepository,
            SubjectRepository subjectRepository,
            AcademicRepository academicRepository,
            FacultyAssignmentRepository facultyAssignmentRepository,
            StudentEnrollmentRepository enrollmentRepository,
            MarksRepository marksRepository
    ) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.academicRepository = academicRepository;
        this.facultyAssignmentRepository = facultyAssignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.marksRepository = marksRepository;
    }

    @PreAuthorize("hasAnyRole('HOD','FACULTY')")
    @PostMapping("/upload")
    public ResponseEntity<?> uploadExcel(
            @RequestParam("file") MultipartFile file
    ) throws Exception {

        Workbook xl = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = xl.getSheetAt(0);

        String email = getString(sheet,1,1);
        String subjectName = getString(sheet,2,1);
        String subjectCode = getString(sheet,3,1);
        int sem = (int) sheet.getRow(4).getCell(1).getNumericCellValue();
        String academicYear = getString(sheet,5,1);
        String classcode = getString(sheet,6,1);

        User faculty = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        Subject subject = subjectRepository
                .findBySubjectNameAndSubjectCode(subjectName,subjectCode)
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        AcademicSession session = academicRepository
                .findByAcademicYearAndSemester(academicYear,sem)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        boolean assigned = facultyAssignmentRepository
                .existsByFacultyIdAndSubjectIdAndAcademicYearAndSemesterAndClassCodeAndIsActiveTrue(
                        faculty.getId(),
                        subject.getId(),
                        academicYear,
                        sem,
                        classcode
                );

        if(!assigned)
            return ResponseEntity.badRequest().body("Faculty not assigned");

        Row header = sheet.getRow(8);
        boolean hasCT = header.getCell(1).toString().contains("CT1");

        for(int i = 9; i <= sheet.getLastRowNum(); i++){

            Row row = sheet.getRow(i);
            if(row == null) continue;

            String rollNo = getRoll(row);

            // skip empty rows
            if(rollNo == null) continue;

            Student student = studentRepository.findByRollNumber(rollNo)
                    .orElseThrow(() -> new RuntimeException(
                            "Student not found " + rollNo
                    ));

            StudentEnrollment enrollment = enrollmentRepository
                    .findByStudentIdAndSubjectIdAndSessionId(
                            student.getId(),
                            subject.getId(),
                            session.getId()
                    ).orElseThrow(() -> new RuntimeException(
                            "Not enrolled " + rollNo
                    ));

            int ct1 = getNum(row,1);
            int ct2 = getNum(row,2);
            int ct3 = getNum(row,3);
            int internal = getNum(row,4);
            int external = getNum(row,5);

            if(ct1<-1 || ct2<-1 || ct3<-1 || internal<0 || external<0)
                throw new RuntimeException("Negative marks "+rollNo);

            if(ct1>20 || ct2>20 || ct3>20)
                throw new RuntimeException("CT max 20 "+rollNo);

            int count = 0;
            if(ct1!=-1) count++;
            if(ct2!=-1) count++;
            if(ct3!=-1) count++;

            if(count>2)
                throw new RuntimeException("Only two CT allowed "+rollNo);

            int ctTotal = 0;
            if(ct1!=-1) ctTotal+=ct1;
            if(ct2!=-1) ctTotal+=ct2;
            if(ct3!=-1) ctTotal+=ct3;

            if(hasCT && internal>10)
                throw new RuntimeException("Internal max 10 "+rollNo);

            if(!hasCT && internal>50)
                throw new RuntimeException("Internal max 50 "+rollNo);

            if(external>50)
                throw new RuntimeException("External max 50 "+rollNo);

            int total = ctTotal + internal + external;

            if(total>100)
                throw new RuntimeException("Total > 100 "+rollNo);

            save(enrollment, faculty, MarkComponentType.CT1, ct1);
            save(enrollment, faculty, MarkComponentType.CT2, ct2);
            save(enrollment, faculty, MarkComponentType.CT3, ct3);
            save(enrollment, faculty, MarkComponentType.INTERNAL, internal);
            save(enrollment, faculty, MarkComponentType.EXTERNAL, external);
        }

        return ResponseEntity.ok("Marks uploaded successfully");
    }

    private void save(
            StudentEnrollment enrollment,
            User faculty,
            MarkComponentType type,
            int marks
    ){
        if(marks == -1) return;

        Mark m = marksRepository
                .findByEnrollment(enrollment)
                .stream()
                .filter(x -> x.getMarksType() == type)
                .findFirst()
                .orElse(null);

        if(m == null){
            m = new Mark();
            m.setEnrollment(enrollment);
            m.setEnteredBy(faculty);
            m.setEnteredAt(java.time.LocalDateTime.now());
        }

        // IMPORTANT — force enum object (not string)
        m.setMarksType(type);

        m.setMarks(BigDecimal.valueOf(marks));

        marksRepository.saveAndFlush(m);
    }

    private String getRoll(Row row){

        Cell cell = row.getCell(0);

        if(cell == null) return null;
        if(cell.getCellType() == CellType.BLANK) return null;

        if(cell.getCellType() == CellType.NUMERIC){
            return String.valueOf((long)cell.getNumericCellValue());
        }

        String val = cell.getStringCellValue();
        if(val == null || val.trim().isEmpty()) return null;

        return val.trim();
    }

    private int getNum(Row row,int col){
        if(row.getCell(col)==null) return -1;
        return (int) row.getCell(col).getNumericCellValue();
    }

    private String getString(Sheet sheet,int r,int c){
        return sheet.getRow(r).getCell(c).toString().trim();
    }
}