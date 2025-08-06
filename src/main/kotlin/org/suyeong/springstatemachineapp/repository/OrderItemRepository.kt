package org.suyeong.springstatemachineapp.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.suyeong.springstatemachineapp.entity.OrderItem

@Repository
interface OrderItemRepository : JpaRepository<OrderItem, Long> {
    
    fun findByOrderId(orderId: Long): List<OrderItem>
    
    @Query("SELECT oi FROM OrderItem oi WHERE oi.productName LIKE %:productName%")
    fun findByProductNameContaining(@Param("productName") productName: String): List<OrderItem>
    
    @Query("SELECT SUM(oi.totalPrice) FROM OrderItem oi WHERE oi.order.id = :orderId")
    fun calculateOrderTotal(@Param("orderId") orderId: Long): java.math.BigDecimal?
}