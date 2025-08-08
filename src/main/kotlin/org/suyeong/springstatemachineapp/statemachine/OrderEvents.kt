package org.suyeong.springstatemachineapp.statemachine

/**
 * Enum representing the events that trigger state transitions in the order state machine.
 */
enum class OrderEvents {
    PAY,
    START_PREPARATION,
    READY_FOR_DELIVERY,
    START_DELIVERY,
    DELIVER,
    CANCEL
}