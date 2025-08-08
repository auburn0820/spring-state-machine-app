package org.suyeong.springstatemachineapp.statemachine

/**
 * Enum representing the possible states of an order in the state machine.
 */
enum class OrderStates {
    CREATED,
    PAID,
    IN_PREPARATION,
    READY_FOR_DELIVERY,
    IN_DELIVERY,
    DELIVERED,
    CANCELLED
}