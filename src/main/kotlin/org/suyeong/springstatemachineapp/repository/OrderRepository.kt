package org.suyeong.springstatemachineapp.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.suyeong.springstatemachineapp.entity.Order
import org.suyeong.springstatemachineapp.statemachine.OrderStates
import java.time.LocalDateTime
import java.util.*

@Repository
interface OrderRepository : JpaRepository<Order, Long> {
    
    fun findByState(state: OrderStates): List<Order>
    
    fun findByCustomerId(customerId: Long): List<Order>
    
    @Query("SELECT o FROM Order o JOIN FETCH o.orderItems WHERE o.id = :orderId")
    fun findByIdWithItems(@Param("orderId") orderId: Long): Optional<Order>
    
    @Query("SELECT o FROM Order o JOIN FETCH o.customer WHERE o.id = :orderId")
    fun findByIdWithCustomer(@Param("orderId") orderId: Long): Optional<Order>
    
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    fun findByCreatedAtBetween(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Order>
    
    @Query("SELECT o FROM Order o WHERE o.customer.id = :customerId AND o.state = :state")
    fun findByCustomerIdAndState(
        @Param("customerId") customerId: Long,
        @Param("state") state: OrderStates
    ): List<Order>
}