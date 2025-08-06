package org.suyeong.springstatemachineapp.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "payments")
class Payment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    var order: Order? = null,
    
    @Column(nullable = false, precision = 10, scale = 2)
    var amount: BigDecimal = BigDecimal.ZERO,
    
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var paymentMethod: PaymentMethod = PaymentMethod.CREDIT_CARD,
    
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    
    @Column(length = 100)
    var transactionId: String? = null,
    
    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column
    var processedAt: LocalDateTime? = null
) {
    fun markAsCompleted(transactionId: String) {
        this.paymentStatus = PaymentStatus.COMPLETED
        this.transactionId = transactionId
        this.processedAt = LocalDateTime.now()
    }
    
    fun markAsFailed() {
        this.paymentStatus = PaymentStatus.FAILED
        this.processedAt = LocalDateTime.now()
    }
}

enum class PaymentMethod {
    CREDIT_CARD,
    DEBIT_CARD,
    BANK_TRANSFER,
    PAYPAL,
    CASH
}

enum class PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED,
    REFUNDED
}