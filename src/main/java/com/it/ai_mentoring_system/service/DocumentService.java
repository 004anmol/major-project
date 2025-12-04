package com.it.ai_mentoring_system.service;

import com.it.ai_mentoring_system.model.*;
import com.it.ai_mentoring_system.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    private final Path fileStorageLocation;

    public DocumentService(@org.springframework.beans.factory.annotation.Value("${file.upload-dir}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public Document requestDocument(Teacher teacher, Student student, String requestMessage) {
        Document document = new Document();
        document.setTeacher(teacher);
        document.setStudent(student);
        document.setRequestMessage(requestMessage);
        document.setStatus(Document.DocumentStatus.PENDING);
        
        return documentRepository.save(document);
    }

    public Document uploadDocument(Student student, Teacher teacher, MultipartFile file, 
                                  String requestMessage) throws IOException {
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path targetLocation = this.fileStorageLocation.resolve(fileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        Document document = new Document();
        document.setStudent(student);
        document.setTeacher(teacher);
        document.setFileName(fileName);
        document.setFilePath(targetLocation.toString());
        document.setOriginalFileName(file.getOriginalFilename());
        document.setRequestMessage(requestMessage);
        document.setStatus(Document.DocumentStatus.PENDING);

        return documentRepository.save(document);
    }

    @Transactional
    public Document reviewDocument(Long documentId, String remarks, Document.DocumentStatus status) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        
        document.setRemarks(remarks);
        document.setStatus(status);
        document.setReviewedAt(java.time.LocalDateTime.now());
        
        return documentRepository.save(document);
    }

    public List<Document> getDocumentsForStudent(Student student) {
        return documentRepository.findByStudent(student);
    }

    public List<Document> getDocumentsForTeacher(Teacher teacher) {
        return documentRepository.findByTeacher(teacher);
    }

    public Document getDocumentById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
    }
}


