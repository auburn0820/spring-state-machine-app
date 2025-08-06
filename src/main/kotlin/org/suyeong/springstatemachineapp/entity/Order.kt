package org.suyeong.springstatemachineapp.entity

import jakarta.persistence.*
import org.suyeong.springstatemachineapp.statemachine.OrderStates
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Entity representing an order in the database.
 * The state field represents the current state of the order in the state machine.
 */
@Entity
@Table(name = "orders")
class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    var customer: Customer? = null,
    
    @Column(nullable = false, precision = 10, scale = 2)
    var totalAmount: BigDecimal = BigDecimal.ZERO,
    
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var state: OrderStates = OrderStates.CREATED,
    
    @Column(length = 500)
    var notes: String? = null,
    
    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column
    var updatedAt: LocalDateTime? = null,
    
    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var orderItems: MutableList<OrderItem> = mutableListOf(),
    
    @OneToOne(mappedBy = "order", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var payment: Payment? = null
) {
    /**
     * Updates the state of the order.
     * Also updates the updatedAt timestamp.
     */
    fun updateState(newState: OrderStates) {
        this.state = newState
        this.updatedAt = LocalDateTime.now()
    }
    
    fun addOrderItem(orderItem: OrderItem) {
        orderItems.add(orderItem)
        orderItem.order = this
    }
    
    fun calculateTotalAmount() {
        totalAmount = orderItems.sumOf { it.totalPrice }
    }
    
    override fun toString(): String {
        return "Order(id=$id, customer=${customer?.name}, totalAmount=$totalAmount, state=$state, createdAt=$createdAt, updatedAt=$updatedAt)"
    }
}