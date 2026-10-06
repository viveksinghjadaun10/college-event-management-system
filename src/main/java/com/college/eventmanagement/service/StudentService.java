package com.college.eventmanagement.service;

import com.college.eventmanagement.exception.BadRequestException;
import com.college.eventmanagement.exception.ResourceNotFoundException;
import com.college.eventmanagement.model.Student;
import com.college.eventmanagement.repository.RegistrationRepository;
import com.college.eventmanagement.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final RegistrationRepository registrationRepository;

    public StudentService(StudentRepository studentRepository, RegistrationRepository registrationRepository) {
        this.studentRepository = studentRepository;
        this.registrationRepository = registrationRepository;
    }

    public Student createStudent(Student student) {
        if (studentRepository.existsByEmail(student.getEmail())) {
            throw new BadRequestException("A student with this email already exists");
        }
        if (student.getMobileNumber() != null
                && studentRepository.existsByMobileNumber(student.getMobileNumber())) {
            throw new BadRequestException("A student with this mobile number already exists");
        }
        student.setId(null);
        return studentRepository.save(student);
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public Student getStudentById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id " + id));
    }

    public Student updateStudent(Long id, Student updated) {
        Student existing = getStudentById(id);
        if (!existing.getEmail().equalsIgnoreCase(updated.getEmail())
                && studentRepository.existsByEmail(updated.getEmail())) {
            throw new BadRequestException("A student with this email already exists");
        }
        String newMobile = updated.getMobileNumber();
        if (newMobile != null
                && !newMobile.equals(existing.getMobileNumber())
                && studentRepository.existsByMobileNumber(newMobile)) {
            throw new BadRequestException("A student with this mobile number already exists");
        }
        existing.setName(updated.getName());
        existing.setEmail(updated.getEmail());
        existing.setDepartment(updated.getDepartment());
        if (newMobile != null) {
            existing.setMobileNumber(newMobile);
        }
        return studentRepository.save(existing);
    }

    public void deleteStudent(Long id) {
        Student student = getStudentById(id);
        if (!registrationRepository.findByStudentId(id).isEmpty()) {
            throw new BadRequestException("Cannot delete a student who has event registrations");
        }
        studentRepository.delete(student);
    }
}