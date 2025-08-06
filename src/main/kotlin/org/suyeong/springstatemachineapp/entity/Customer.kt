package org.suyeong.springstatemachineapp.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "customers")
class Customer(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(nullable = false, length = 100)
    var name: String = "",
    
    @Column(nullable = false, unique = true, length = 100)
    var email: String = "",
    
    @Column(length = 20)
    var phoneNumber: String? = null,
    
    @Column(length = 500)
    var address: String? = null,
    
    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    
    @OneToMany(mappedBy = "customer", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var orders: MutableList<Order> = mutableListOf()
) {
    fun addOrder(order: Order) {
        orders.add(order)
        order.customer = this
    }
}