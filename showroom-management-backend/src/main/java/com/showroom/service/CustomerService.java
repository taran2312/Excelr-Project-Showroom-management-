package com.showroom.service;

import com.showroom.dto.CustomerDto;
import com.showroom.entity.Customer;
import com.showroom.exception.BadRequestException;
import com.showroom.exception.ConflictException;
import com.showroom.exception.ResourceNotFoundException;
import com.showroom.mapper.EntityMapper;
import com.showroom.repository.CustomerRepository;
import com.showroom.repository.SaleRepository;
import com.showroom.repository.TestDriveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final SaleRepository saleRepository;
    private final TestDriveRepository testDriveRepository;

    public List<CustomerDto> search(String q) {
        return customerRepository.search(EntityMapper.clean(q)).stream().map(EntityMapper::toDto).toList();
    }

    public CustomerDto findById(Long id) {
        return EntityMapper.toDto(getEntity(id));
    }

    public Customer getEntity(Long id) {
        return customerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }

    @Transactional
    public CustomerDto create(CustomerDto dto) {
        String phone = dto.getPhone().trim();
        if (customerRepository.existsByPhone(phone)) {
            throw new BadRequestException("A customer with phone " + phone + " already exists");
        }
        Customer c = new Customer();
        apply(c, dto, phone);
        return EntityMapper.toDto(customerRepository.save(c));
    }

    @Transactional
    public CustomerDto update(Long id, CustomerDto dto) {
        Customer c = getEntity(id);
        String phone = dto.getPhone().trim();
        if (customerRepository.existsByPhoneAndIdNot(phone, id)) {
            throw new BadRequestException("A customer with phone " + phone + " already exists");
        }
        apply(c, dto, phone);
        return EntityMapper.toDto(c);
    }

    @Transactional
    public void delete(Long id) {
        Customer c = getEntity(id);
        if (saleRepository.existsByCustomerId(id) || testDriveRepository.existsByCustomerId(id)) {
            throw new ConflictException("Customer has sales or test drives on record and cannot be deleted");
        }
        customerRepository.delete(c);
    }

    private void apply(Customer c, CustomerDto dto, String phone) {
        c.setFullName(dto.getFullName().trim());
        c.setEmail(EntityMapper.clean(dto.getEmail()));
        c.setPhone(phone);
        c.setAddress(EntityMapper.clean(dto.getAddress()));
        c.setCity(EntityMapper.clean(dto.getCity()));
        c.setNotes(EntityMapper.clean(dto.getNotes()));
    }
}
