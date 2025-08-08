package org.suyeong.springstatemachineapp.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.statemachine.persist.DefaultStateMachinePersister
import org.springframework.statemachine.persist.StateMachinePersister
import org.suyeong.springstatemachineapp.statemachine.OrderEvents
import org.suyeong.springstatemachineapp.statemachine.OrderStateMachinePersist
import org.suyeong.springstatemachineapp.statemachine.OrderStates

/**
 * Configuration for StateMachinePersister that handles automatic persistence
 * of state machine contexts using the OrderStateMachinePersist implementation.
 */
@Configuration
class StateMachinePersisterConfiguration {

    /**
     * Creates a StateMachinePersister bean for Order state machines.
     * This persister will automatically save/restore state machine contexts
     * using the provided StateMachinePersist implementation.
     */
    @Bean
    fun orderStateMachinePersister(
        stateMachinePersist: OrderStateMachinePersist
    ): StateMachinePersister<OrderStates, OrderEvents, Long> {
        return DefaultStateMachinePersister(stateMachinePersist)
    }
}