package org.suyeong.springstatemachineapp.statemachine

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.assertj.core.api.Assertions.assertThat
import org.suyeong.springstatemachineapp.entity.Customer
import org.suyeong.springstatemachineapp.entity.OrderItem
import org.suyeong.springstatemachineapp.repository.CustomerRepository
import org.suyeong.springstatemachineapp.service.OrderService
import java.math.BigDecimal

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderStateMachineTest {

    @Autowired
    private lateinit var orderStateMachineService: OrderStateMachineService

    @Autowired
    private lateinit var orderService: OrderService

    @Autowired
    private lateinit var customerRepository: CustomerRepository

    private lateinit var testCustomer: Customer
    private var testOrderId: Long = 0L

    @BeforeEach
    fun setUp() {
        // Create test customer and order for state machine tests
        testCustomer = customerRepository.save(Customer(
            name = "State Machine 테스트 고객",
            email = "statemachine@example.com"
        ))
        
        val testOrder = orderService.createOrder(
            customerId = testCustomer.id!!,
            orderItems = listOf(
                OrderItem(
                    productName = "테스트 상품",
                    quantity = 1,
                    unitPrice = BigDecimal("100000.00")
                )
            )
        )
        testOrderId = testOrder.id!!
    }

    @Test
    fun `initial state should be CREATED`() {
        // Verify initial state from database
        val order = orderService.findById(testOrderId)!!
        assertThat(order.state).isEqualTo(OrderStates.CREATED)
    }

    @Test
    fun `should transition from CREATED to PAID when PAY event is triggered`() {
        // Trigger PAY event
        val result = orderStateMachineService.triggerEvent(OrderEvents.PAY, testOrderId)
        
        // Verify event was accepted and state changed
        assertThat(result).isTrue
        
        // Verify database state changed
        val updatedOrder = orderService.findById(testOrderId)!!
        assertThat(updatedOrder.state).isEqualTo(OrderStates.PAID)
        assertThat(updatedOrder.updatedAt).isNotNull
    }




    @Test
    fun `invalid event should not change state`() {
        // Try to trigger DELIVER event from CREATED state (invalid transition)
        val result = orderStateMachineService.triggerEvent(OrderEvents.DELIVER, testOrderId)
        
        // Verify event was not accepted and state did not change
        assertThat(result).isFalse
        
        // Verify database state unchanged
        val unchangedOrder = orderService.findById(testOrderId)!!
        assertThat(unchangedOrder.state).isEqualTo(OrderStates.CREATED)
    }

    @Test
    fun `should handle state transitions correctly`() {
        // Test that state machine service correctly validates and updates database on state changes
        
        // 1. Start in CREATED state
        val order = orderService.findById(testOrderId)!!
        assertThat(order.state).isEqualTo(OrderStates.CREATED)
        
        // 2. Advance through valid states and verify database is updated each time
        val transitions = listOf(
            OrderEvents.PAY to OrderStates.PAID,
            OrderEvents.START_PREPARATION to OrderStates.IN_PREPARATION,
            OrderEvents.READY_FOR_DELIVERY to OrderStates.READY_FOR_DELIVERY,
            OrderEvents.START_DELIVERY to OrderStates.IN_DELIVERY,
            OrderEvents.DELIVER to OrderStates.DELIVERED
        )
        
        transitions.forEach { (event, expectedState) ->
            val result = orderStateMachineService.triggerEvent(event, testOrderId)
            assertThat(result).`as`("Event $event should be accepted").isTrue
            
            // Verify database state matches
            val dbOrder = orderService.findById(testOrderId)!!
            assertThat(dbOrder.state).`as`("Database state should match expected state").isEqualTo(expectedState)
            assertThat(dbOrder.updatedAt).`as`("updatedAt should be set when state changes").isNotNull
        }
    }

    @Test
    fun `should prevent invalid transitions`() {
        // Test various invalid transitions
        val invalidTransitions = listOf(
            OrderStates.CREATED to OrderEvents.START_PREPARATION,
            OrderStates.CREATED to OrderEvents.READY_FOR_DELIVERY,
            OrderStates.CREATED to OrderEvents.START_DELIVERY,
            OrderStates.CREATED to OrderEvents.DELIVER,
            OrderStates.PAID to OrderEvents.READY_FOR_DELIVERY,
            OrderStates.PAID to OrderEvents.START_DELIVERY,
            OrderStates.PAID to OrderEvents.DELIVER
        )
        
        invalidTransitions.forEach { (initialState, event) ->
            // Create a new order for each test
            val newOrder = orderService.createOrder(
                customerId = testCustomer.id!!,
                orderItems = listOf(
                    OrderItem(
                        productName = "테스트 상품 $event",
                        quantity = 1,
                        unitPrice = BigDecimal("100000.00")
                    )
                )
            )
            
            // Move to the initial state if needed
            if (initialState == OrderStates.PAID) {
                orderStateMachineService.triggerEvent(OrderEvents.PAY, newOrder.id!!)
            }
            
            // Try the invalid transition
            val result = orderStateMachineService.triggerEvent(event, newOrder.id!!)
            
            // Should be rejected
            assertThat(result).`as`("Transition from $initialState with event $event should be rejected").isFalse
            
            // State should remain unchanged
            val unchangedOrder = orderService.findById(newOrder.id!!)!!
            assertThat(unchangedOrder.state).`as`("State should remain $initialState after invalid event $event").isEqualTo(initialState)
        }
    }
}