package org.suyeong.springstatemachineapp.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.suyeong.springstatemachineapp.entity.Payment
import org.suyeong.springstatemachineapp.entity.PaymentMethod
import org.suyeong.springstatemachineapp.entity.PaymentStatus
import org.suyeong.springstatemachineapp.exception.InvalidPaymentStatusException
import org.suyeong.springstatemachineapp.exception.OrderNotFoundException
import org.suyeong.springstatemachineapp.exception.PaymentAlreadyExistsException
import org.suyeong.springstatemachineapp.exception.PaymentNotFoundException
import org.suyeong.springstatemachineapp.exception.PaymentProcessingFailedException
import org.suyeong.springstatemachineapp.exception.RefundProcessingFailedException
import org.suyeong.springstatemachineapp.repository.OrderRepository
import org.suyeong.springstatemachineapp.repository.PaymentRepository
import java.util.*

@Service
@Transactional(readOnly = true)
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository
) {

    fun findById(id: Long): Optional<Payment> {
        return paymentRepository.findById(id)
    }

    fun findByOrderId(orderId: Long): Payment? {
        return paymentRepository.findByOrderId(orderId)
    }

    fun findByTransactionId(transactionId: String): Payment? {
        return paymentRepository.findByTransactionId(transactionId)
    }

    fun findByStatus(status: PaymentStatus): List<Payment> {
        return paymentRepository.findByPaymentStatus(status)
    }

    fun findByMethod(method: PaymentMethod): List<Payment> {
        return paymentRepository.findByPaymentMethod(method)
    }

    fun findByCustomerId(customerId: Long): List<Payment> {
        return paymentRepository.findByCustomerId(customerId)
    }

    @Transactional
    fun createPayment(orderId: Long, paymentMethod: PaymentMethod): Payment {
        val order = orderRepository.findByIdOrNull(orderId) ?: throw OrderNotFoundException(orderId)

        val existingPayment = paymentRepository.findByOrderId(orderId)
        if (existingPayment != null) {
            throw PaymentAlreadyExistsException(orderId)
        }

        val payment = Payment(
            order = order,
            amount = order.totalAmount,
            paymentMethod = paymentMethod
        )

        return paymentRepository.save(payment)
    }

    @Transactional
    fun processPayment(paymentId: Long): Payment {
        val payment = paymentRepository.findByIdOrNull(paymentId) ?: throw PaymentNotFoundException(paymentId)

        if (payment.paymentStatus != PaymentStatus.PENDING) {
            throw InvalidPaymentStatusException("Payment must be in PENDING status to process")
        }

        val transactionId = generateTransactionId()

        try {
            val success = processExternalPayment(payment, transactionId)

            if (success) {
                payment.markAsCompleted(transactionId)
            } else {
                payment.markAsFailed()
            }
        } catch (e: Exception) {
            payment.markAsFailed()
            throw PaymentProcessingFailedException(e.message ?: "Unknown error", e)
        }

        return paymentRepository.save(payment)
    }

    @Transactional
    fun refundPayment(paymentId: Long): Payment {
        val payment = paymentRepository.findByIdOrNull(paymentId) ?: throw PaymentNotFoundException(paymentId)

        if (payment.paymentStatus != PaymentStatus.COMPLETED) {
            throw InvalidPaymentStatusException("Can only refund completed payments")
        }

        try {
            val success = processExternalRefund(payment)

            if (success) {
                payment.paymentStatus = PaymentStatus.REFUNDED
            } else {
                throw RefundProcessingFailedException("Refund processing failed")
            }
        } catch (e: RefundProcessingFailedException) {
            throw e
        } catch (e: Exception) {
            throw RefundProcessingFailedException(e.message ?: "Unknown error", e)
        }

        return paymentRepository.save(payment)
    }

    private fun generateTransactionId(): String {
        return "TXN_${System.currentTimeMillis()}_${UUID.randomUUID().toString().substring(0, 8)}"
    }

    private fun processExternalPayment(payment: Payment, transactionId: String): Boolean {
        // In a real environment, integrate with an external payment system
        // Here, it's a simulation
        Thread.sleep(100) // Simulate external API call
        return true // Assume success
    }

    private fun processExternalRefund(payment: Payment): Boolean {
        // In a real environment, request a refund from an external payment system
        // Here, it's a simulation
        Thread.sleep(100) // Simulate external API call
        return true // Assume success
    }
}