package org.suyeong.springstatemachineapp.statemachine

/**
 * Enum representing the events that trigger state transitions in the order state machine.
 */
enum class OrderEvents {
    // Event to trigger payment for an order
    PAY,
    
    // Event to start preparing the order
    START_PREPARATION,
    
    // Event to mark the order as ready for delivery
    READY_FOR_DELIVERY,
    
    // Event to start the delivery process
    START_DELIVERY,
    
    // Event to mark the order as delivered
    DELIVER,
    
    // Event to cancel the order
    CANCEL
}