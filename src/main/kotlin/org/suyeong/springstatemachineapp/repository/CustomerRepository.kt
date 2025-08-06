package org.suyeong.springstatemachineapp.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.suyeong.springstatemachineapp.entity.Customer
import java.util.*

@Repository
interface CustomerRepository : JpaRepository<Customer, Long> {
    
    fun findByEmail(email: String): Optional<Customer>
    
    fun existsByEmail(email: String): Boolean
    
    @Query("SELECT c FROM Customer c WHERE c.name LIKE %:name%")
    fun findByNameContaining(@Param("name") name: String): List<Customer>
    
    @Query("SELECT c FROM Customer c JOIN FETCH c.orders WHERE c.id = :customerId")
    fun findByIdWithOrders(@Param("customerId") customerId: Long): Optional<Customer>
}