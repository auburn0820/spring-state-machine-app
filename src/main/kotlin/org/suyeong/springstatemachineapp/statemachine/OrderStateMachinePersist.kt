package org.suyeong.springstatemachineapp.statemachine

import mu.KLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.statemachine.StateMachineContext
import org.springframework.statemachine.StateMachinePersist
import org.springframework.statemachine.support.DefaultStateMachineContext
import org.springframework.stereotype.Component
import org.suyeong.springstatemachineapp.repository.OrderRepository

/**
 * JPA-based implementation of StateMachinePersist that uses the Order entity directly.
 * This eliminates the need for a separate state machine context table.
 */
@Component
class OrderStateMachinePersist(
    private val orderRepository: OrderRepository
) : StateMachinePersist<OrderStates, OrderEvents, Long> {
    companion object : KLogging()

    /**
     * Saves the state machine context by updating the Order entity's state
     */
    override fun write(context: StateMachineContext<OrderStates, OrderEvents>, orderId: Long) {
        try {
            val order = orderRepository.findByIdOrNull(orderId)
            if (order != null) {
                val newState = context.state ?: OrderStates.CREATED
                if (order.state != newState) {
                    logger.info { "Persisting state change for order $orderId: ${order.state} -> $newState" }
                    order.updateState(newState)
                    orderRepository.save(order)
                }
            } else {
                logger.error { "Cannot persist state: Order $orderId not found" }
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to persist state for order $orderId" }
            throw e
        }
    }

    /**
     * Reads the state machine context from the Order entity
     */
    override fun read(orderId: Long): StateMachineContext<OrderStates, OrderEvents>? {
        return try {
            val order = orderRepository.findByIdOrNull(orderId)
            if (order != null) {
                logger.debug { "Reading state for order $orderId: ${order.state}" }
                // Create a simple context with just the state
                DefaultStateMachineContext(
                    order.state,
                    null,
                    null,
                    null,
                    null
                )
            } else {
                logger.debug { "Order $orderId not found" }
                null
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to read state for order $orderId" }
            null
        }
    }
}