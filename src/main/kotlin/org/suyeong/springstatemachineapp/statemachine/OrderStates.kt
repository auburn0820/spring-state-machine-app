package org.suyeong.springstatemachineapp.statemachine

/**
 * Enum representing the possible states of an order in the state machine.
 */
enum class OrderStates {
    // Initial state when an order is created
    CREATED,
    
    // Order has been paid
    PAID,
    
    // Order is being prepared
    IN_PREPARATION,
    
    // Order is ready for delivery
    READY_FOR_DELIVERY,
    
    // Order is being delivered
    IN_DELIVERY,
    
    // Order has been delivered successfully
    DELIVERED,
    
    // Order has been cancelled
    CANCELLED
}