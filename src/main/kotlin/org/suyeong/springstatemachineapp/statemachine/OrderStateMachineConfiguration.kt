package org.suyeong.springstatemachineapp.statemachine

import org.springframework.context.annotation.Configuration
import org.springframework.statemachine.config.EnableStateMachineFactory
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer

/**
 * Factory configuration for creating isolated state machine instances.
 * Each operation gets its own state machine instance to avoid concurrency issues.
 * The state machine handles transitions automatically when events are sent.
 */
@Configuration
@EnableStateMachineFactory
class OrderStateMachineConfiguration : EnumStateMachineConfigurerAdapter<OrderStates, OrderEvents>() {

    /**
     * Configures the states of the state machine.
     */
    override fun configure(states: StateMachineStateConfigurer<OrderStates, OrderEvents>) {
        states
            .withStates()
            .initial(OrderStates.CREATED)
            .states(OrderStates.entries.toSet())
    }

    /**
     * Configures the transitions between states.
     * These define the valid state transitions and which events trigger them.
     */
    override fun configure(transitions: StateMachineTransitionConfigurer<OrderStates, OrderEvents>) {
        transitions
            .withExternal()
            .source(OrderStates.CREATED)
            .target(OrderStates.PAID)
            .event(OrderEvents.PAY)
            .and()
            .withExternal()
            .source(OrderStates.PAID)
            .target(OrderStates.IN_PREPARATION)
            .event(OrderEvents.START_PREPARATION)
            .and()
            .withExternal()
            .source(OrderStates.IN_PREPARATION)
            .target(OrderStates.READY_FOR_DELIVERY)
            .event(OrderEvents.READY_FOR_DELIVERY)
            .and()
            .withExternal()
            .source(OrderStates.READY_FOR_DELIVERY)
            .target(OrderStates.IN_DELIVERY)
            .event(OrderEvents.START_DELIVERY)
            .and()
            .withExternal()
            .source(OrderStates.IN_DELIVERY)
            .target(OrderStates.DELIVERED)
            .event(OrderEvents.DELIVER)
            .and()
            .withExternal()
            .source(OrderStates.CREATED)
            .target(OrderStates.CANCELLED)
            .event(OrderEvents.CANCEL)
            .and()
            .withExternal()
            .source(OrderStates.PAID)
            .target(OrderStates.CANCELLED)
            .event(OrderEvents.CANCEL)
            .and()
            .withExternal()
            .source(OrderStates.IN_PREPARATION)
            .target(OrderStates.CANCELLED)
            .event(OrderEvents.CANCEL)
    }

    /**
     * Configures the state machine with minimal setup for factory use.
     */
    override fun configure(config: StateMachineConfigurationConfigurer<OrderStates, OrderEvents>) {
        config
            .withConfiguration()
            .autoStartup(false) // We'll start each instance manually
    }

}