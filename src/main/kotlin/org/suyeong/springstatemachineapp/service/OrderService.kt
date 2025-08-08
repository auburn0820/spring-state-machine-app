package org.suyeong.springstatemachineapp.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.suyeong.springstatemachineapp.common.Result
import org.suyeong.springstatemachineapp.entity.Order
import org.suyeong.springstatemachineapp.entity.OrderItem
import org.suyeong.springstatemachineapp.dto.CreateOrderItemRequest
import org.suyeong.springstatemachineapp.repository.CustomerRepository
import org.suyeong.springstatemachineapp.repository.OrderRepository
import org.suyeong.springstatemachineapp.statemachine.OrderEvents
import org.suyeong.springstatemachineapp.statemachine.OrderStateMachineService
import org.suyeong.springstatemachineapp.statemachine.OrderStates
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class OrderService(
    private val orderRepository: OrderRepository,
    private val customerRepository: CustomerRepository,
    private val orderStateMachineService: OrderStateMachineService
) {

    fun findById(id: Long): Order? {
        return orderRepository.findByIdOrNull(id)
    }

    fun findByIdWithItems(id: Long): Order? {
        return orderRepository.findByIdWithItems(id).orElse(null)
    }

    fun findByIdWithCustomer(id: Long): Order? {
        return orderRepository.findByIdWithCustomer(id).orElse(null)
    }

    fun findAll(): List<Order> {
        return orderRepository.findAll()
    }

    fun findByState(state: OrderStates): List<Order> {
        return orderRepository.findByState(state)
    }

    fun findByCustomerId(customerId: Long): List<Order> {
        return orderRepository.findByCustomerId(customerId)
    }

    fun findByCustomerIdAndState(customerId: Long, state: OrderStates): List<Order> {
        return orderRepository.findByCustomerIdAndState(customerId, state)
    }

    fun findByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): List<Order> {
        return orderRepository.findByCreatedAtBetween(startDate, endDate)
    }

    @Transactional
    fun createOrder(customerId: Long, orderItems: List<OrderItem>, notes: String? = null): Order {
        val customer = customerRepository.findByIdOrNull(customerId)
            ?: throw IllegalArgumentException("Customer with id $customerId not found")

        if (orderItems.isEmpty()) {
            throw IllegalArgumentException("Order must have at least one item")
        }

        val order = Order(
            customer = customer,
            totalAmount = java.math.BigDecimal.ZERO,
            notes = notes
        )

        orderItems.forEach { item ->
            item.calculateTotalPrice()
            order.addOrderItem(item)
        }

        order.calculateTotalAmount()

        return orderRepository.save(order)
    }

    fun convertToOrderItems(orderItems: List<CreateOrderItemRequest>): List<OrderItem> {
        return orderItems.map { orderItemRequest ->
            OrderItem(
                productName = orderItemRequest.productName,
                quantity = orderItemRequest.quantity,
                unitPrice = orderItemRequest.unitPrice
            )
        }
    }

    @Transactional
    suspend fun processPayment(orderId: Long): Order {
        val result = orderStateMachineService.triggerEventWithResult(OrderEvents.PAY, orderId)
        return when (result) {
            is Result.Success -> result.value
            is Result.Error -> throw result.exception
        }
    }

    @Transactional
    suspend fun startPreparation(orderId: Long): Order {
        val result = orderStateMachineService.triggerEventWithResult(OrderEvents.START_PREPARATION, orderId)
        return when (result) {
            is Result.Success -> result.value
            is Result.Error -> throw result.exception
        }
    }

    @Transactional
    suspend fun markReadyForDelivery(orderId: Long): Order {
        val result = orderStateMachineService.triggerEventWithResult(OrderEvents.READY_FOR_DELIVERY, orderId)
        return when (result) {
            is Result.Success -> result.value
            is Result.Error -> throw result.exception
        }
    }

    @Transactional
    suspend fun startDelivery(orderId: Long): Order {
        val result = orderStateMachineService.triggerEventWithResult(OrderEvents.START_DELIVERY, orderId)
        return when (result) {
            is Result.Success -> result.value
            is Result.Error -> throw result.exception
        }
    }

    @Transactional
    suspend fun completeDelivery(orderId: Long): Order {
        val result = orderStateMachineService.triggerEventWithResult(OrderEvents.DELIVER, orderId)
        return when (result) {
            is Result.Success -> result.value
            is Result.Error -> throw result.exception
        }
    }

    @Transactional
    suspend fun cancelOrder(orderId: Long): Order {
        val result = orderStateMachineService.triggerEventWithResult(OrderEvents.CANCEL, orderId)
        return when (result) {
            is Result.Success -> result.value
            is Result.Error -> throw result.exception
        }
    }

    // ========== Result Pattern Methods ==========

    /**
     * Process payment using Result pattern - provides rich error information
     */
    @Transactional
    suspend fun processPaymentWithResult(orderId: Long): Result<Order> {
        return orderStateMachineService.triggerEventWithResult(OrderEvents.PAY, orderId)
    }

    /**
     * Cancel order using Result pattern with detailed error handling
     */
    @Transactional
    suspend fun cancelOrderWithResult(orderId: Long): Result<Order> {
        return orderStateMachineService.triggerEventWithResult(OrderEvents.CANCEL, orderId)
    }
}