package org.suyeong.springstatemachineapp.statemachine

import mu.KLogging
import org.springframework.messaging.Message
import org.springframework.statemachine.StateMachine
import org.springframework.statemachine.state.State
import org.springframework.statemachine.support.StateMachineInterceptorAdapter
import org.springframework.statemachine.transition.Transition
import org.springframework.stereotype.Component
import org.springframework.data.repository.findByIdOrNull
import org.suyeong.springstatemachineapp.repository.OrderRepository

/**
 * Interceptor that automatically persists order state changes when state machine transitions occur.
 * This is the key component that makes Spring State Machine automatically save state changes.
 */
@Component
class OrderStateChangeInterceptor(
    private val orderRepository: OrderRepository
) : StateMachineInterceptorAdapter<OrderStates, OrderEvents>() {

    companion object : KLogging()

    /**
     * Called after a state change occurs in the state machine.
     * This is where we automatically update the Order entity in the database.
     */
    override fun postStateChange(
        state: State<OrderStates, OrderEvents>?,
        message: Message<OrderEvents>?,
        transition: Transition<OrderStates, OrderEvents>?,
        stateMachine: StateMachine<OrderStates, OrderEvents>?,
        rootStateMachine: StateMachine<OrderStates, OrderEvents>?
    ) {
        // Only process if we have both state and message
        if (state == null || message == null) {
            return
        }

        // Get the order ID from the message headers
        val orderId = message.headers["orderId"] as? Long
        if (orderId == null) {
            logger.warn { "No orderId found in message headers for state change to ${state.id}" }
            return
        }

        // Get the order from the database
        val order = orderRepository.findByIdOrNull(orderId)
        if (order == null) {
            logger.error { "Order not found: $orderId during state change to ${state.id}" }
            return
        }

        val newState = state.id
        val oldState = order.state

        // Update the order's state if it has changed
        if (oldState != newState) {
            logger.info { "State machine triggered: updating order $orderId from $oldState to $newState" }
            order.updateState(newState)
            orderRepository.save(order)
            logger.debug { "Order $orderId state successfully updated by interceptor" }
        } else {
            logger.debug { "Order $orderId state unchanged: $newState" }
        }
    }

}