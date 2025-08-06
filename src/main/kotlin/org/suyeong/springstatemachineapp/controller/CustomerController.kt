package org.suyeong.springstatemachineapp.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.suyeong.springstatemachineapp.dto.*
import org.suyeong.springstatemachineapp.service.CustomerService

@RestController
@RequestMapping("/api/customers")
class CustomerController(
    private val customerService: CustomerService
) {
    
    @GetMapping
    fun getAllCustomers(): ResponseEntity<List<CustomerSummaryResponse>> {
        val customers = customerService.findAll()
        val responses = customers.map { CustomerSummaryResponse.from(it) }
        return ResponseEntity.ok(responses)
    }
    
    @GetMapping("/{id}")
    fun getCustomerById(@PathVariable id: Long): ResponseEntity<CustomerResponse> {
        val customer = customerService.findById(id)
            .orElseThrow { IllegalArgumentException("Customer with id $id not found") }
        
        return ResponseEntity.ok(CustomerResponse.from(customer))
    }
    
    @GetMapping("/{id}/with-orders")
    fun getCustomerWithOrders(@PathVariable id: Long): ResponseEntity<CustomerResponse> {
        val customer = customerService.findByIdWithOrders(id)
            .orElseThrow { IllegalArgumentException("Customer with id $id not found") }
        
        return ResponseEntity.ok(CustomerResponse.from(customer))
    }
    
    @GetMapping("/search")
    fun searchCustomersByName(@RequestParam name: String): ResponseEntity<List<CustomerSummaryResponse>> {
        val customers = customerService.searchByName(name)
        val responses = customers.map { CustomerSummaryResponse.from(it) }
        return ResponseEntity.ok(responses)
    }
    
    @GetMapping("/email/{email}")
    fun getCustomerByEmail(@PathVariable email: String): ResponseEntity<CustomerResponse> {
        val customer = customerService.findByEmail(email)
            .orElseThrow { IllegalArgumentException("Customer with email $email not found") }
        
        return ResponseEntity.ok(CustomerResponse.from(customer))
    }
    
    @PostMapping
    fun createCustomer(@Valid @RequestBody request: CreateCustomerRequest): ResponseEntity<CustomerResponse> {
        val customer = customerService.createCustomer(request.toEntity())
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(CustomerResponse.from(customer))
    }
    
    @PutMapping("/{id}")
    fun updateCustomer(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateCustomerRequest
    ): ResponseEntity<CustomerResponse> {
        val updatedCustomer = customerService.updateCustomer(id, request.toEntity())
        return ResponseEntity.ok(CustomerResponse.from(updatedCustomer))
    }
    
    @DeleteMapping("/{id}")
    fun deleteCustomer(@PathVariable id: Long): ResponseEntity<Unit> {
        customerService.deleteCustomer(id)
        return ResponseEntity.noContent().build()
    }
}