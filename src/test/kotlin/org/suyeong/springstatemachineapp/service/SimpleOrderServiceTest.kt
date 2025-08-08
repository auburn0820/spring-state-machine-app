package org.suyeong.springstatemachineapp.service

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.suyeong.springstatemachineapp.entity.Customer
import org.suyeong.springstatemachineapp.entity.OrderItem
import org.suyeong.springstatemachineapp.repository.CustomerRepository
import org.suyeong.springstatemachineapp.statemachine.OrderStates
import java.math.BigDecimal

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SimpleOrderServiceTest {

    @Autowired
    private lateinit var orderService: OrderService

    @Autowired
    private lateinit var customerRepository: CustomerRepository

    private lateinit var testCustomer: Customer

    @BeforeEach
    fun setUp() {
        testCustomer = customerRepository.save(
            Customer(
                name = "테스트 고객",
                email = "test@example.com"
            )
        )
    }

    @Test
    fun `should create order with items successfully`() {
        // Given
        val orderItems = listOf(
            OrderItem(
                productName = "테스트 상품1",
                quantity = 2,
                unitPrice = BigDecimal("50000.00")
            ),
            OrderItem(
                productName = "테스트 상품2",
                quantity = 1,
                unitPrice = BigDecimal("30000.00")
            )
        )

        // When
        val createdOrder = orderService.createOrder(
            customerId = testCustomer.id!!,
            orderItems = orderItems,
            notes = "테스트 주문"
        )

        // Then
        assertThat(createdOrder.id).isNotNull
        assertThat(createdOrder.customer?.id).isEqualTo(testCustomer.id)
        assertThat(createdOrder.totalAmount).isEqualTo(BigDecimal("130000.00"))
        assertThat(createdOrder.state).isEqualTo(OrderStates.CREATED)
        assertThat(createdOrder.notes).isEqualTo("테스트 주문")
        assertThat(createdOrder.orderItems).hasSize(2)
    }

    @Test
    fun `should throw exception when creating order without items`() {
        // When & Then
        assertThatThrownBy {
            orderService.createOrder(
                customerId = testCustomer.id!!,
                orderItems = emptyList()
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `should find orders by customer`() {
        // Given
        val customer2 = customerRepository.save(
            Customer(
                name = "고객2",
                email = "customer2@example.com"
            )
        )

        orderService.createOrder(
            customerId = testCustomer.id!!,
            orderItems = listOf(
                OrderItem(productName = "상품1", quantity = 1, unitPrice = BigDecimal("10000"))
            )
        )
        orderService.createOrder(
            customerId = testCustomer.id!!,
            orderItems = listOf(
                OrderItem(productName = "상품2", quantity = 1, unitPrice = BigDecimal("20000"))
            )
        )
        orderService.createOrder(
            customerId = customer2.id!!,
            orderItems = listOf(
                OrderItem(productName = "상품3", quantity = 1, unitPrice = BigDecimal("30000"))
            )
        )

        // When
        val customer1Orders = orderService.findByCustomerId(testCustomer.id!!)
        val customer2Orders = orderService.findByCustomerId(customer2.id!!)

        // Then
        assertThat(customer1Orders).hasSize(2)
        assertThat(customer2Orders).hasSize(1)
    }

    @Test
    fun `should find orders by state`() {
        // Given
        val initialCreatedCount = orderService.findByState(OrderStates.CREATED).size
        
        val order1 = orderService.createOrder(
            customerId = testCustomer.id!!,
            orderItems = listOf(
                OrderItem(productName = "상품1", quantity = 1, unitPrice = BigDecimal("10000"))
            )
        )
        val order2 = orderService.createOrder(
            customerId = testCustomer.id!!,
            orderItems = listOf(
                OrderItem(productName = "상품2", quantity = 1, unitPrice = BigDecimal("20000"))
            )
        )

        // When
        val createdOrders = orderService.findByState(OrderStates.CREATED)

        // Then
        assertThat(createdOrders).hasSizeGreaterThanOrEqualTo(initialCreatedCount + 2)
        assertThat(createdOrders).anyMatch { it.id == order1.id }
        assertThat(createdOrders).anyMatch { it.id == order2.id }
    }

    @Test
    fun `should find order by id with items`() {
        // Given
        val order = orderService.createOrder(
            customerId = testCustomer.id!!,
            orderItems = listOf(
                OrderItem(productName = "상품1", quantity = 2, unitPrice = BigDecimal("25000"))
            )
        )

        // When
        val foundOrder = orderService.findByIdWithItems(order.id!!)

        // Then
        assertThat(foundOrder).isNotNull
        assertThat(foundOrder!!.orderItems).hasSize(1)
        assertThat(foundOrder.orderItems[0].productName).isEqualTo("상품1")
    }

    @Test
    fun `should find order by id with customer`() {
        // Given
        val order = orderService.createOrder(
            customerId = testCustomer.id!!,
            orderItems = listOf(
                OrderItem(productName = "상품1", quantity = 1, unitPrice = BigDecimal("10000"))
            )
        )

        // When
        val foundOrder = orderService.findByIdWithCustomer(order.id!!)

        // Then
        assertThat(foundOrder).isNotNull
        assertThat(foundOrder!!.customer?.name).isEqualTo(testCustomer.name)
        assertThat(foundOrder.customer?.email).isEqualTo(testCustomer.email)
    }

    @Test
    fun `should order be PAID after payment`() {
        // Given
        val order = orderService.createOrder(
            customerId = testCustomer.id!!,
            orderItems = listOf(
                OrderItem(productName = "상품1", quantity = 1, unitPrice = BigDecimal("10000"))
            )
        )

        // When
        val paidOrder = runBlocking { orderService.processPayment(order.id!!) }

        // Then
        assertThat(paidOrder.state).isEqualTo(OrderStates.PAID)
        assertThat(paidOrder.updatedAt).isNotNull
    }

    @Test
    fun `should throw exception when processing payment for non-CREATED order`() {
        // Given
        val order = orderService.createOrder(
            customerId = testCustomer.id!!,
            orderItems = listOf(
                OrderItem(productName = "상품1", quantity = 1, unitPrice = BigDecimal("10000"))
            )
        )

        runBlocking { orderService.processPayment(order.id!!) }

        // When & Then
        assertThatThrownBy {
            runBlocking { orderService.processPayment(order.id!!) }
        }.isInstanceOf(IllegalStateException::class.java)
    }
}