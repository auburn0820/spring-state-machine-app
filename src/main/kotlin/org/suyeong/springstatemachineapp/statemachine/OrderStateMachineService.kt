package org.suyeong.springstatemachineapp.statemachine

import kotlinx.coroutines.reactive.awaitFirstOrNull
import mu.KLogging
import org.springframework.messaging.support.MessageBuilder
import org.springframework.data.repository.findByIdOrNull
import org.springframework.statemachine.StateMachine
import org.springframework.statemachine.StateMachineEventResult
import org.springframework.statemachine.config.StateMachineFactory
import org.springframework.statemachine.persist.StateMachinePersister
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.suyeong.springstatemachineapp.entity.Order
import org.suyeong.springstatemachineapp.repository.OrderRepository
import org.suyeong.springstatemachineapp.common.Result
import reactor.core.publisher.Mono

/**
 * Service that utilizes Spring State Machine with automatic state persistence.
 * - Uses StateMachinePersister for automatic persistence of state machine contexts
 * - Synchronizes state machines with Order state efficiently
 * - Each operation uses isolated state machine instances
 */
@Service
class OrderStateMachineService(
    private val stateMachineFactory: StateMachineFactory<OrderStates, OrderEvents>,
    private val orderRepository: OrderRepository,
    private val stateMachinePersister: StateMachinePersister<OrderStates, OrderEvents, Long>
) {
    companion object : KLogging()

    /**
     * Creates or restores a state machine for the given order using StateMachinePersister.
     * Automatically restores from database or creates new machine.
     */
    private suspend fun getStateMachineForOrder(order: Order): StateMachine<OrderStates, OrderEvents> {
        val orderId = order.id!!
        
        try {
            // Try to restore state machine from persister
            val stateMachine = stateMachinePersister.restore(stateMachineFactory.getStateMachine(), orderId)
            logger.debug { "Successfully restored state machine for order $orderId in state ${stateMachine.state?.id}" }
            return stateMachine
        } catch (e: Exception) {
            logger.warn(e) { "Failed to restore state machine for order $orderId, creating new one" }
            
            // Create new state machine and sync to order state
            val stateMachine = stateMachineFactory.getStateMachine()
            stateMachine.startReactively().awaitFirstOrNull()
            
            // Sync to current order state if needed
            if (order.state != OrderStates.CREATED) {
                syncStateMachineToOrderState(stateMachine, order.state)
            }
            
            logger.debug { "Created new state machine for order $orderId in state ${stateMachine.state?.id}" }
            return stateMachine
        }
    }

    /**
     * Synchronizes a state machine to match the order's current state.
     */
    private suspend fun syncStateMachineToOrderState(
        stateMachine: StateMachine<OrderStates, OrderEvents>, 
        targetState: OrderStates
    ) {
        val transitionPath = getTransitionPathToState(targetState)
        
        for (event in transitionPath) {
            val message = MessageBuilder.withPayload(event).build()
            val results = stateMachine.sendEvent(Mono.just(message))
            
            val eventAccepted = results.collectList().awaitFirstOrNull()?.any { 
                it.resultType == StateMachineEventResult.ResultType.ACCEPTED 
            } ?: false
            
            if (!eventAccepted) {
                throw IllegalStateException("Failed to sync state machine: event $event not accepted")
            }
        }
        
        logger.debug { "Successfully synchronized state machine to state $targetState" }
    }


    /**
     * Gets the sequence of events needed to reach a specific state from CREATED.
     */
    private fun getTransitionPathToState(targetState: OrderStates): List<OrderEvents> {
        return when (targetState) {
            OrderStates.CREATED -> emptyList()
            OrderStates.PAID -> listOf(OrderEvents.PAY)
            OrderStates.IN_PREPARATION -> listOf(OrderEvents.PAY, OrderEvents.START_PREPARATION)
            OrderStates.READY_FOR_DELIVERY -> listOf(
                OrderEvents.PAY, 
                OrderEvents.START_PREPARATION, 
                OrderEvents.READY_FOR_DELIVERY
            )
            OrderStates.IN_DELIVERY -> listOf(
                OrderEvents.PAY, 
                OrderEvents.START_PREPARATION, 
                OrderEvents.READY_FOR_DELIVERY,
                OrderEvents.START_DELIVERY
            )
            OrderStates.DELIVERED -> listOf(
                OrderEvents.PAY, 
                OrderEvents.START_PREPARATION, 
                OrderEvents.READY_FOR_DELIVERY,
                OrderEvents.START_DELIVERY,
                OrderEvents.DELIVER
            )
            OrderStates.CANCELLED -> listOf(OrderEvents.CANCEL)
        }
    }

    /**
     * Triggers an event on the state machine using Result pattern.
     */
    @Transactional
    suspend fun triggerEventWithResult(event: OrderEvents, orderId: Long): Result<Order> {
        logger.info { "Triggering event: $event for order: $orderId" }

        val order = orderRepository.findByIdOrNull(orderId)
            ?: return Result.Error(IllegalArgumentException("Order with id $orderId not found"))
        
        val originalState = order.state
        logger.debug { "Order $orderId current state: $originalState" }
        
        // Get state machine (restored or newly created)
        val stateMachine = getStateMachineForOrder(order)
        
        return try {
            // Create message with order ID for the interceptor
            val message = MessageBuilder.withPayload(event)
                .setHeader("orderId", orderId)
                .build()

            // Send event to state machine
            val results = stateMachine.sendEvent(Mono.just(message))
            
            val eventAccepted = results.collectList().awaitFirstOrNull()?.any { 
                it.resultType == StateMachineEventResult.ResultType.ACCEPTED 
            } ?: false
            
            if (!eventAccepted) {
                return Result.Error(IllegalStateException("Event $event was not accepted in state $originalState"))
            }
            
            // Persist the updated state machine context (this also updates the Order entity)
            stateMachinePersister.persist(stateMachine, orderId)
            
            // Get the updated order from database
            val updatedOrder = orderRepository.findByIdOrNull(orderId)
                ?: return Result.Error(IllegalStateException("Order $orderId disappeared during state transition"))
            
            logger.info { "Order $orderId transitioned from $originalState to ${updatedOrder.state}" }
            Result.Success(updatedOrder)
        } catch (e: Exception) {
            logger.error(e) { "Error triggering event $event for order $orderId" }
            Result.Error(e)
        } finally {
            // Clean up the isolated state machine instance
            stateMachine.stopReactively().awaitFirstOrNull()
        }
    }

    /**
     * Triggers an event on the state machine with boolean return.
     */
    @Transactional
    suspend fun triggerEvent(event: OrderEvents, orderId: Long): Boolean {
        val result = triggerEventWithResult(event, orderId)
        return result is Result.Success
    }
}