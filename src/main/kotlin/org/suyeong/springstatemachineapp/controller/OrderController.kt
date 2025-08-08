package org.suyeong.springstatemachineapp.controller

import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.suyeong.springstatemachineapp.dto.*
import org.suyeong.springstatemachineapp.service.OrderService
import org.suyeong.springstatemachineapp.statemachine.OrderStates
import org.suyeong.springstatemachineapp.common.Result
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val orderService: OrderService
) {
    
    @GetMapping
    fun getAllOrders(): ResponseEntity<List<OrderSummaryResponse>> {
        val orders = orderService.findAll()
        val responses = orders.map { OrderSummaryResponse.from(it) }
        return ResponseEntity.ok(responses)
    }
    
    @GetMapping("/{id}")
    fun getOrderById(@PathVariable id: Long): ResponseEntity<OrderResponse> {
        val order = orderService.findByIdWithCustomer(id)
            ?: throw IllegalArgumentException("Order with id $id not found")
        
        return ResponseEntity.ok(OrderResponse.from(order))
    }
    
    @GetMapping("/{id}/with-items")
    fun getOrderWithItems(@PathVariable id: Long): ResponseEntity<OrderResponse> {
        val order = orderService.findByIdWithItems(id)
            ?: throw IllegalArgumentException("Order with id $id not found")
        
        return ResponseEntity.ok(OrderResponse.from(order, includeItems = true))
    }
    
    @GetMapping("/by-state/{state}")
    fun getOrdersByState(@PathVariable state: OrderStates): ResponseEntity<List<OrderSummaryResponse>> {
        val orders = orderService.findByState(state)
        val responses = orders.map { OrderSummaryResponse.from(it) }
        return ResponseEntity.ok(responses)
    }
    
    @GetMapping("/by-customer/{customerId}")
    fun getOrdersByCustomer(@PathVariable customerId: Long): ResponseEntity<List<OrderSummaryResponse>> {
        val orders = orderService.findByCustomerId(customerId)
        val responses = orders.map { OrderSummaryResponse.from(it) }
        return ResponseEntity.ok(responses)
    }
    
    @GetMapping("/by-customer/{customerId}/state/{state}")
    fun getOrdersByCustomerAndState(
        @PathVariable customerId: Long,
        @PathVariable state: OrderStates
    ): ResponseEntity<List<OrderSummaryResponse>> {
        val orders = orderService.findByCustomerIdAndState(customerId, state)
        val responses = orders.map { OrderSummaryResponse.from(it) }
        return ResponseEntity.ok(responses)
    }
    
    @GetMapping("/by-date-range")
    fun getOrdersByDateRange(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: LocalDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: LocalDateTime
    ): ResponseEntity<List<OrderSummaryResponse>> {
        val orders = orderService.findByDateRange(startDate, endDate)
        val responses = orders.map { OrderSummaryResponse.from(it) }
        return ResponseEntity.ok(responses)
    }
    
    @PostMapping
    fun createOrder(@Valid @RequestBody request: CreateOrderRequest): ResponseEntity<OrderResponse> {
        val order = orderService.createOrder(
            customerId = request.customerId,
            orderItems = orderService.convertToOrderItems(request.orderItems),
            notes = request.notes
        )
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(OrderResponse.from(order))
    }
    
    @PostMapping("/{id}/pay")
    suspend fun processPayment(@PathVariable id: Long): ResponseEntity<OrderResponse> {
        val order = orderService.processPayment(id)
        return ResponseEntity.ok(OrderResponse.from(order))
    }
    
    @PostMapping("/{id}/start-preparation")
    suspend fun startPreparation(@PathVariable id: Long): ResponseEntity<OrderResponse> {
        val order = orderService.startPreparation(id)
        return ResponseEntity.ok(OrderResponse.from(order))
    }
    
    @PostMapping("/{id}/ready-for-delivery")
    suspend fun markReadyForDelivery(@PathVariable id: Long): ResponseEntity<OrderResponse> {
        val order = orderService.markReadyForDelivery(id)
        return ResponseEntity.ok(OrderResponse.from(order))
    }
    
    @PostMapping("/{id}/start-delivery")
    suspend fun startDelivery(@PathVariable id: Long): ResponseEntity<OrderResponse> {
        val order = orderService.startDelivery(id)
        return ResponseEntity.ok(OrderResponse.from(order))
    }
    
    @PostMapping("/{id}/deliver")
    suspend fun completeDelivery(@PathVariable id: Long): ResponseEntity<OrderResponse> {
        val order = orderService.completeDelivery(id)
        return ResponseEntity.ok(OrderResponse.from(order))
    }
    
    @PostMapping("/{id}/cancel")
    suspend fun cancelOrder(@PathVariable id: Long): ResponseEntity<OrderResponse> {
        val order = orderService.cancelOrder(id)
        return ResponseEntity.ok(OrderResponse.from(order))
    }
    
    // ========== Result Pattern Endpoints ==========
    
    /**
     * Process payment using Result pattern - returns detailed error information
     */
    @PostMapping("/{id}/pay-with-result")
    suspend fun processPaymentWithResult(@PathVariable id: Long): ResponseEntity<Any> {
        return orderService.processPaymentWithResult(id)
            .let { result ->
                when (result) {
                    is Result.Success -> ResponseEntity.ok(OrderResponse.from(result.value))
                    is Result.Error -> ResponseEntity.badRequest().body(
                        mapOf(
                            "error" to "PAYMENT_PROCESSING_FAILED",
                            "message" to result.message,
                            "orderId" to id
                        )
                    )
                }
            }
    }
    
    
    /**
     * Cancel order using Result pattern with rich error handling
     */
    @PostMapping("/{id}/cancel-with-result")
    suspend fun cancelOrderWithResult(@PathVariable id: Long): ResponseEntity<Any> {
        return orderService.cancelOrderWithResult(id)
            .let { result ->
                when (result) {
                    is Result.Success -> ResponseEntity.ok(OrderResponse.from(result.value))
                    is Result.Error -> {
                        val errorCode = when {
                            result.message.contains("not found") -> "ORDER_NOT_FOUND"
                            result.message.contains("Cannot cancel") -> "INVALID_STATE_FOR_CANCELLATION"
                            result.message.contains("not accepted") -> "STATE_TRANSITION_REJECTED"
                            else -> "CANCELLATION_FAILED"
                        }
                        ResponseEntity.badRequest().body(
                            mapOf(
                                "error" to errorCode,
                                "message" to result.message,
                                "orderId" to id,
                                "suggestion" to when (errorCode) {
                                    "ORDER_NOT_FOUND" -> "Please verify the order ID exists"
                                    "INVALID_STATE_FOR_CANCELLATION" -> "Order cannot be cancelled in its current state"
                                    "STATE_TRANSITION_REJECTED" -> "State machine rejected the cancellation request"
                                    else -> "Please contact support"
                                }
                            )
                        )
                    }
                }
            }
    }
}