package org.suyeong.springstatemachineapp.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "order_items")
class OrderItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    var order: Order? = null,
    
    @Column(nullable = false, length = 200)
    var productName: String = "",
    
    @Column(nullable = false)
    var quantity: Int = 0,
    
    @Column(nullable = false, precision = 10, scale = 2)
    var unitPrice: BigDecimal = BigDecimal.ZERO,
    
    @Column(nullable = false, precision = 10, scale = 2)
    var totalPrice: BigDecimal = unitPrice * quantity.toBigDecimal()
) {
    fun calculateTotalPrice() {
        totalPrice = unitPrice * quantity.toBigDecimal()
    }
    
    override fun toString(): String {
        return "OrderItem(id=$id, productName='$productName', quantity=$quantity, unitPrice=$unitPrice, totalPrice=$totalPrice)"
    }
}