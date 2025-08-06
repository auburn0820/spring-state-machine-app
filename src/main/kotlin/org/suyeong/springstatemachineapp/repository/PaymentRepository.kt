package org.suyeong.springstatemachineapp.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.suyeong.springstatemachineapp.entity.Payment
import org.suyeong.springstatemachineapp.entity.PaymentMethod
import org.suyeong.springstatemachineapp.entity.PaymentStatus

@Repository
interface PaymentRepository : JpaRepository<Payment, Long> {
    
    fun findByOrderId(orderId: Long): Payment?
    
    fun findByTransactionId(transactionId: String): Payment?
    
    fun findByPaymentStatus(status: PaymentStatus): List<Payment>
    
    fun findByPaymentMethod(method: PaymentMethod): List<Payment>
    
    @Query("SELECT p FROM Payment p WHERE p.order.customer.id = :customerId")
    fun findByCustomerId(@Param("customerId") customerId: Long): List<Payment>
    
    @Query("SELECT p FROM Payment p WHERE p.paymentStatus = :status AND p.paymentMethod = :method")
    fun findByStatusAndMethod(
        @Param("status") status: PaymentStatus,
        @Param("method") method: PaymentMethod
    ): List<Payment>
}