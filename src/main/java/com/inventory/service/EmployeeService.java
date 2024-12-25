package com.inventory.service;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.EmployeeDto;
import com.inventory.entity.Employee;
import com.inventory.exception.ValidationException;
import com.inventory.repository.EmployeeRepository;
import com.inventory.dao.EmployeeDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final EmployeeDao employeeDao;
    private final UtilityService utilityService;

    @Transactional
    public ApiResponse<?> create(EmployeeDto dto) {
        try {
            validateEmployee(dto);
            
            Optional<Employee> existingEmployee = employeeRepository.findByMobileNumber(dto.getMobileNumber().trim());
            if (existingEmployee.isPresent()) {
                throw new ValidationException("Employee with this mobile number already exists");
            }

            Employee employee = new Employee();
            mapDtoToEntity(dto, employee);
            employee.setCreatedBy(utilityService.getCurrentLoggedInUser());
            
            employee = employeeRepository.save(employee);
            return ApiResponse.success("Employee created successfully", mapEntityToDto(employee));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to create employee: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<?> update(Long id, EmployeeDto dto) {
        try {
            validateEmployee(dto);
            
            Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Employee not found"));

            Optional<Employee> existingEmployee = employeeRepository.findByMobileNumberAndIdNot(
                dto.getMobileNumber().trim(), employee.getId());
            if (existingEmployee.isPresent()) {
                throw new ValidationException("Employee with this mobile number already exists");
            }

            mapDtoToEntity(dto, employee);
            employee.setUpdatedAt(OffsetDateTime.now());
            
            employee = employeeRepository.save(employee);
            return ApiResponse.success("Employee updated successfully", mapEntityToDto(employee));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to update employee: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<?> delete(Long id) {
        try {
            Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Employee not found"));
            
            employeeRepository.delete(employee);
            return ApiResponse.success("Employee deleted successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to delete employee: " + e.getMessage());
        }
    }

    public ApiResponse<Map<String, Object>> searchEmployees(EmployeeDto dto) {
        try {
            Map<String, Object> result = employeeDao.searchEmployees(dto);
            return ApiResponse.success("Employees retrieved successfully", result);
        } catch (Exception e) {
            throw new ValidationException("Failed to search employees: " + e.getMessage());
        }
    }

    private void validateEmployee(EmployeeDto dto) {
        if (!StringUtils.hasText(dto.getName())) {
            throw new ValidationException("Name is required");
        }
//        if (!StringUtils.hasText(dto.getMobileNumber())) {
//            throw new ValidationException("Mobile number is required");
//        }
//        if (dto.getMobileNumber().length() < 10 || dto.getMobileNumber().length() > 15) {
//            throw new ValidationException("Invalid mobile number");
//        }
    }

    private void mapDtoToEntity(EmployeeDto dto, Employee employee) {
        employee.setName(dto.getName().trim());
        employee.setMobileNumber(dto.getMobileNumber().trim());
        employee.setEmail(dto.getEmail());
        employee.setAddress(dto.getAddress());
        employee.setDesignation(dto.getDesignation());
        employee.setDepartment(dto.getDepartment());
        employee.setJoiningDate(dto.getJoiningDate());
        employee.setStatus(dto.getStatus() != null ? dto.getStatus() : "A");
    }

    private EmployeeDto mapEntityToDto(Employee employee) {
        EmployeeDto dto = new EmployeeDto();
        dto.setId(employee.getId());
        dto.setName(employee.getName());
        dto.setMobileNumber(employee.getMobileNumber());
        dto.setEmail(employee.getEmail());
        dto.setAddress(employee.getAddress());
        dto.setDesignation(employee.getDesignation());
        dto.setDepartment(employee.getDepartment());
        dto.setJoiningDate(employee.getJoiningDate());
        dto.setStatus(employee.getStatus());
        return dto;
    }
} 