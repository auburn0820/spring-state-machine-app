package org.suyeong.springstatemachineapp.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.suyeong.springstatemachineapp.dto.CreatePaymentRequest
import org.suyeong.springstatemachineapp.dto.PaymentResponse
import org.suyeong.springstatemachineapp.dto.PaymentSummaryResponse
import org.suyeong.springstatemachineapp.entity.PaymentMethod
import org.suyeong.springstatemachineapp.entity.PaymentStatus
import org.suyeong.springstatemachineapp.service.PaymentService

@RestController
@RequestMapping("/api/payments")
class PaymentController(
    private val paymentService: PaymentService
) {

    @GetMapping("/{id}")
    fun getPaymentById(@PathVariable id: Long): ResponseEntity<PaymentResponse> {
        val payment = paymentService.findById(id)
            .orElseThrow { IllegalArgumentException("Payment with id $id not found") }

        return ResponseEntity.ok(PaymentResponse.from(payment))
    }

    @GetMapping("/order/{orderId}")
    fun getPaymentByOrderId(@PathVariable orderId: Long): ResponseEntity<PaymentResponse> {
        val payment =
            paymentService.findByOrderId(orderId)
                ?: throw IllegalArgumentException("Payment for order $orderId not found")

        return ResponseEntity.ok(PaymentResponse.from(payment))
    }

    @GetMapping("/transaction/{transactionId}")
    fun getPaymentByTransactionId(@PathVariable transactionId: String): ResponseEntity<PaymentResponse> {
        val payment = paymentService.findByTransactionId(transactionId)
            ?: throw IllegalArgumentException("Payment with transaction ID $transactionId not found")

        return ResponseEntity.ok(PaymentResponse.from(payment))
    }

    @GetMapping("/by-status/{status}")
    fun getPaymentsByStatus(@PathVariable status: PaymentStatus): ResponseEntity<List<PaymentSummaryResponse>> {
        val payments = paymentService.findByStatus(status)
        val responses = payments.map { PaymentSummaryResponse.from(it) }
        return ResponseEntity.ok(responses)
    }

    @GetMapping("/by-method/{method}")
    fun getPaymentsByMethod(@PathVariable method: PaymentMethod): ResponseEntity<List<PaymentSummaryResponse>> {
        val payments = paymentService.findByMethod(method)
        val responses = payments.map { PaymentSummaryResponse.from(it) }
        return ResponseEntity.ok(responses)
    }

    @GetMapping("/by-customer/{customerId}")
    fun getPaymentsByCustomer(@PathVariable customerId: Long): ResponseEntity<List<PaymentSummaryResponse>> {
        val payments = paymentService.findByCustomerId(customerId)
        val responses = payments.map { PaymentSummaryResponse.from(it) }
        return ResponseEntity.ok(responses)
    }

    @PostMapping
    fun createPayment(@Valid @RequestBody request: CreatePaymentRequest): ResponseEntity<PaymentResponse> {
        val payment = paymentService.createPayment(request.orderId, request.paymentMethod)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(PaymentResponse.from(payment))
    }

    @PostMapping("/{id}/process")
    fun processPayment(@PathVariable id: Long): ResponseEntity<PaymentResponse> {
        val payment = paymentService.processPayment(id)
        return ResponseEntity.ok(PaymentResponse.from(payment))
    }

    @PostMapping("/{id}/refund")
    fun refundPayment(@PathVariable id: Long): ResponseEntity<PaymentResponse> {
        val payment = paymentService.refundPayment(id)
        return ResponseEntity.ok(PaymentResponse.from(payment))
    }
}