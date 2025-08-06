package org.suyeong.springstatemachineapp.statemachine

import mu.KLogging
import org.springframework.messaging.support.MessageBuilder
import org.springframework.data.repository.findByIdOrNull
import org.springframework.statemachine.StateMachine
import org.springframework.statemachine.StateMachineEventResult
import org.springframework.statemachine.config.StateMachineFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.suyeong.springstatemachineapp.entity.Order
import org.suyeong.springstatemachineapp.repository.OrderRepository
import org.suyeong.springstatemachineapp.common.Result
import reactor.core.publisher.Mono

/**
 * Service that properly utilizes Spring State Machine.
 * - Events are sent to the state machine
 * - State machine automatically handles state transitions
 * - Interceptor automatically saves state changes to database
 * - Each operation uses an isolated state machine instance to avoid concurrency issues
 */
@Service
class OrderStateMachineService(
    private val stateMachineFactory: StateMachineFactory<OrderStates, OrderEvents>,
    private val orderRepository: OrderRepository,
    private val orderStateChangeInterceptor: OrderStateChangeInterceptor
) {
    companion object : KLogging() {
        private val STATE_TRANSITIONS: Map<OrderStates, Map<OrderEvents, OrderStates>> = mapOf(
            OrderStates.CREATED to mapOf(
                OrderEvents.PAY to OrderStates.PAID,
                OrderEvents.CANCEL to OrderStates.CANCELLED
            ),
            OrderStates.PAID to mapOf(
                OrderEvents.START_PREPARATION to OrderStates.IN_PREPARATION,
                OrderEvents.CANCEL to OrderStates.CANCELLED
            ),
            OrderStates.IN_PREPARATION to mapOf(
                OrderEvents.READY_FOR_DELIVERY to OrderStates.READY_FOR_DELIVERY,
                OrderEvents.CANCEL to OrderStates.CANCELLED
            ),
            OrderStates.READY_FOR_DELIVERY to mapOf(
                OrderEvents.START_DELIVERY to OrderStates.IN_DELIVERY,
                OrderEvents.CANCEL to OrderStates.CANCELLED
            ),
            OrderStates.IN_DELIVERY to mapOf(
                OrderEvents.DELIVER to OrderStates.DELIVERED,
                OrderEvents.CANCEL to OrderStates.CANCELLED
            ),
            OrderStates.DELIVERED to emptyMap(),
            OrderStates.CANCELLED to emptyMap()
        )
    }

    /**
     * Creates an isolated state machine instance and synchronizes it to the order's current state.
     * This is the key to properly using Spring State Machine with existing data.
     */
    private fun createStateMachineForOrder(order: Order): StateMachine<OrderStates, OrderEvents> {
        val stateMachine = stateMachineFactory.getStateMachine()
        
        // Start the state machine (starts in CREATED state)
        stateMachine.startReactively().block()
        
        // If the order is not in CREATED state, we need to "replay" events to get to the current state
        // This is a practical approach that works with Spring State Machine's design
        if (order.state != OrderStates.CREATED) {
            logger.debug { "Synchronizing state machine to order ${order.id} state: ${order.state}" }
            
            // Replay the sequence of events needed to reach the current state
            // We don't attach the interceptor yet, so no database saves occur during synchronization
            val eventsToReplay = getEventsToReachState(order.state)
            for (event in eventsToReplay) {
                val message = MessageBuilder.withPayload(event).build()
                stateMachine.sendEvent(Mono.just(message)).collectList().block()
            }
        }
        
        // Now register interceptor for automatic persistence of future state changes
        stateMachine.stateMachineAccessor.doWithAllRegions { access ->
            access.addStateMachineInterceptor(orderStateChangeInterceptor)
        }
        
        logger.debug { "State machine synchronized for order ${order.id} in state ${stateMachine.state.id}" }
        return stateMachine
    }

    /**
     * Gets the sequence of events needed to reach a specific state from CREATED using BFS.
     * This replaces the hardcoded event sequences with a graph-based approach.
     */
    private fun getEventsToReachState(targetState: OrderStates): List<OrderEvents> {
        if (targetState == OrderStates.CREATED) {
            return emptyList()
        }
        
        val queue = mutableListOf<Pair<OrderStates, List<OrderEvents>>>()
        val visited = mutableSetOf<OrderStates>()
        
        queue.add(OrderStates.CREATED to emptyList())
        visited.add(OrderStates.CREATED)
        
        while (queue.isNotEmpty()) {
            val (currentState, eventPath) = queue.removeAt(0)
            
            if (currentState == targetState) {
                return eventPath
            }
            
            STATE_TRANSITIONS[currentState]?.forEach { (event, nextState) ->
                if (nextState !in visited) {
                    visited.add(nextState)
                    queue.add(nextState to eventPath + event)
                }
            }
        }
        
        throw IllegalArgumentException("No path found from CREATED to $targetState")
    }

    /**
     * Triggers an event on the state machine using Result pattern.
     * The state machine handles the transition and the interceptor handles persistence automatically.
     */
    @Transactional
    fun triggerEventWithResult(event: OrderEvents, orderId: Long): Result<Order> {
        logger.info { "Triggering event: $event for order: $orderId" }

        val order = orderRepository.findByIdOrNull(orderId)
            ?: return Result.Error(IllegalArgumentException("Order with id $orderId not found"))
        
        val originalState = order.state
        logger.debug { "Order $orderId current state: $originalState" }
        
        // Create state machine instance configured for this order's current state
        val stateMachine = createStateMachineForOrder(order)
        
        return try {
            // Create message with order ID for the interceptor
            val message = MessageBuilder.withPayload(event)
                .setHeader("orderId", orderId)
                .build()

            // Send event to state machine - IT WILL HANDLE THE TRANSITION AUTOMATICALLY
            val results = stateMachine.sendEvent(Mono.just(message))
            
            val eventAccepted = results.collectList().block()?.any { 
                it.resultType == StateMachineEventResult.ResultType.ACCEPTED 
            } ?: false
            
            if (!eventAccepted) {
                return Result.Error(IllegalStateException("Event $event was not accepted in state $originalState"))
            }
            
            // The interceptor has ALREADY updated the order in the database
            // Just retrieve the updated order
            val updatedOrder = orderRepository.findByIdOrNull(orderId)
                ?: return Result.Error(IllegalStateException("Order $orderId disappeared during state transition"))
            
            logger.info { "Order $orderId transitioned from $originalState to ${updatedOrder.state}" }
            Result.Success(updatedOrder)
        } catch (e: Exception) {
            Result.Error(e)
        } finally {
            // Clean up the isolated state machine instance
            stateMachine.stopReactively().block()
        }
    }

    /**
     * Triggers an event on the state machine.
     * The state machine handles the transition and the interceptor handles persistence automatically.
     */
    @Transactional
    fun triggerEvent(event: OrderEvents, orderId: Long): Boolean {
        logger.info { "Triggering event: $event for order: $orderId" }

        val order = orderRepository.findByIdOrNull(orderId)
        if (order == null) {
            logger.error { "Order not found: $orderId" }
            return false
        }
        
        logger.debug { "Order $orderId current state: ${order.state}" }
        
        // Create state machine instance configured for this order's current state
        val stateMachine = createStateMachineForOrder(order)
        
        return try {
            // Create message with order ID for the interceptor
            val message = MessageBuilder.withPayload(event)
                .setHeader("orderId", orderId)
                .build()

            // Send event to state machine - IT WILL HANDLE THE TRANSITION AUTOMATICALLY
            val results = stateMachine.sendEvent(Mono.just(message))
            
            val eventAccepted = results.collectList().block()?.any { 
                it.resultType == StateMachineEventResult.ResultType.ACCEPTED 
            } ?: false
            
            if (eventAccepted) {
                logger.info { "Event $event accepted for order $orderId" }
                // The interceptor has already updated the database!
            } else {
                logger.warn { "Event $event was not accepted for order $orderId in state ${order.state}" }
            }
            
            eventAccepted
        } catch (e: Exception) {
            logger.error(e) { "Error triggering event $event for order $orderId" }
            false
        } finally {
            // Clean up the isolated state machine instance
            stateMachine.stopReactively().block()
        }
    }

    
}