package org.suyeong.springstatemachineapp.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.suyeong.springstatemachineapp.entity.Customer
import org.suyeong.springstatemachineapp.repository.CustomerRepository
import java.util.*

@Service
@Transactional(readOnly = true)
class CustomerService(
    private val customerRepository: CustomerRepository
) {
    
    fun findById(id: Long): Optional<Customer> {
        return customerRepository.findById(id)
    }
    
    fun findByEmail(email: String): Optional<Customer> {
        return customerRepository.findByEmail(email)
    }
    
    fun findAll(): List<Customer> {
        return customerRepository.findAll()
    }
    
    fun searchByName(name: String): List<Customer> {
        return customerRepository.findByNameContaining(name)
    }
    
    fun findByIdWithOrders(customerId: Long): Optional<Customer> {
        return customerRepository.findByIdWithOrders(customerId)
    }
    
    @Transactional
    fun createCustomer(customer: Customer): Customer {
        if (customerRepository.existsByEmail(customer.email)) {
            throw IllegalArgumentException("Customer with email ${customer.email} already exists")
        }
        return customerRepository.save(customer)
    }
    
    @Transactional
    fun updateCustomer(id: Long, updatedCustomer: Customer): Customer {
        val existingCustomer = customerRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Customer with id $id not found") }
        
        if (updatedCustomer.email != existingCustomer.email && 
            customerRepository.existsByEmail(updatedCustomer.email)) {
            throw IllegalArgumentException("Customer with email ${updatedCustomer.email} already exists")
        }
        
        existingCustomer.name = updatedCustomer.name
        existingCustomer.email = updatedCustomer.email
        existingCustomer.phoneNumber = updatedCustomer.phoneNumber
        existingCustomer.address = updatedCustomer.address
        
        return customerRepository.save(existingCustomer)
    }
    
    @Transactional
    fun deleteCustomer(id: Long) {
        if (!customerRepository.existsById(id)) {
            throw IllegalArgumentException("Customer with id $id not found")
        }
        customerRepository.deleteById(id)
    }
}