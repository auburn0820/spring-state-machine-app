package org.suyeong.springstatemachineapp.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import org.suyeong.springstatemachineapp.entity.Payment
import org.suyeong.springstatemachineapp.entity.PaymentMethod
import org.suyeong.springstatemachineapp.entity.PaymentStatus
import java.math.BigDecimal
import java.time.LocalDateTime

data class CreatePaymentRequest(
    @field:NotNull(message = "Order ID is required")
    @field:Positive(message = "Order ID must be positive")
    val orderId: Long,
    
    @field:NotNull(message = "Payment method is required")
    val paymentMethod: PaymentMethod
)

data class PaymentResponse(
    val id: Long,
    val orderId: Long,
    val amount: BigDecimal,
    val paymentMethod: PaymentMethod,
    val paymentStatus: PaymentStatus,
    val transactionId: String?,
    val createdAt: LocalDateTime,
    val processedAt: LocalDateTime?
) {
    companion object {
        fun from(payment: Payment): PaymentResponse {
            return PaymentResponse(
                id = payment.id!!,
                orderId = payment.order?.id ?: 0L,
                amount = payment.amount,
                paymentMethod = payment.paymentMethod,
                paymentStatus = payment.paymentStatus,
                transactionId = payment.transactionId,
                createdAt = payment.createdAt,
                processedAt = payment.processedAt
            )
        }
    }
}

data class PaymentSummaryResponse(
    val id: Long,
    val orderId: Long,
    val amount: BigDecimal,
    val paymentMethod: PaymentMethod,
    val paymentStatus: PaymentStatus,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(payment: Payment): PaymentSummaryResponse {
            return PaymentSummaryResponse(
                id = payment.id!!,
                orderId = payment.order?.id ?: 0L,
                amount = payment.amount,
                paymentMethod = payment.paymentMethod,
                paymentStatus = payment.paymentStatus,
                createdAt = payment.createdAt
            )
        }
    }
}