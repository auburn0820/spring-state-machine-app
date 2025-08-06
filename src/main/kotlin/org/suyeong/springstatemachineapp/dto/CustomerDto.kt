package org.suyeong.springstatemachineapp.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.suyeong.springstatemachineapp.entity.Customer
import java.time.LocalDateTime

data class CreateCustomerRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 100, message = "Name must not exceed 100 characters")
    val name: String,
    
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    @field:Size(max = 100, message = "Email must not exceed 100 characters")
    val email: String,
    
    @field:Size(max = 20, message = "Phone number must not exceed 20 characters")
    val phoneNumber: String? = null,
    
    @field:Size(max = 500, message = "Address must not exceed 500 characters")
    val address: String? = null
) {
    fun toEntity(): Customer {
        return Customer(
            name = name,
            email = email,
            phoneNumber = phoneNumber,
            address = address
        )
    }
}

data class UpdateCustomerRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 100, message = "Name must not exceed 100 characters")
    val name: String,
    
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    @field:Size(max = 100, message = "Email must not exceed 100 characters")
    val email: String,
    
    @field:Size(max = 20, message = "Phone number must not exceed 20 characters")
    val phoneNumber: String? = null,
    
    @field:Size(max = 500, message = "Address must not exceed 500 characters")
    val address: String? = null
) {
    fun toEntity(): Customer {
        return Customer(
            name = name,
            email = email,
            phoneNumber = phoneNumber,
            address = address
        )
    }
}

data class CustomerResponse(
    val id: Long,
    val name: String,
    val email: String,
    val phoneNumber: String?,
    val address: String?,
    val createdAt: LocalDateTime,
    val orderCount: Int = 0
) {
    companion object {
        fun from(customer: Customer): CustomerResponse {
            return CustomerResponse(
                id = customer.id!!,
                name = customer.name,
                email = customer.email,
                phoneNumber = customer.phoneNumber,
                address = customer.address,
                createdAt = customer.createdAt,
                orderCount = customer.orders.size
            )
        }
    }
}

data class CustomerSummaryResponse(
    val id: Long,
    val name: String,
    val email: String,
    val orderCount: Int,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(customer: Customer): CustomerSummaryResponse {
            return CustomerSummaryResponse(
                id = customer.id!!,
                name = customer.name,
                email = customer.email,
                orderCount = customer.orders.size,
                createdAt = customer.createdAt
            )
        }
    }
}