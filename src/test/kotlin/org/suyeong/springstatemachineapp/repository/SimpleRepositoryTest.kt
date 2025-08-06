package org.suyeong.springstatemachineapp.repository

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.annotation.Transactional
import org.suyeong.springstatemachineapp.entity.Customer
import org.suyeong.springstatemachineapp.entity.Order
import org.suyeong.springstatemachineapp.entity.OrderItem
import org.suyeong.springstatemachineapp.statemachine.OrderStates
import java.math.BigDecimal

@DataJpaTest
@ActiveProfiles("test")
@Transactional
class SimpleRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var customerRepository: CustomerRepository

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var orderItemRepository: OrderItemRepository

    private lateinit var testCustomer: Customer

    @BeforeEach
    fun setUp() {
        testCustomer = Customer(
            name = "테스트 고객",
            email = "test@example.com",
            phoneNumber = "010-1234-5678",
            address = "서울시 강남구"
        )
        testCustomer = entityManager.persist(testCustomer)
        entityManager.flush()
    }

    @Test
    fun `should save and find customer`() {
        val foundCustomer = customerRepository.findByIdOrNull(testCustomer.id!!)
        
        assertThat(foundCustomer).isNotNull
        assertThat(foundCustomer!!.name).isEqualTo("테스트 고객")
        assertThat(foundCustomer.email).isEqualTo("test@example.com")
    }

    @Test
    fun `should find customer by email`() {
        val foundCustomer = customerRepository.findByEmail("test@example.com").orElse(null)
        
        assertThat(foundCustomer).isNotNull
        assertThat(foundCustomer!!.id).isEqualTo(testCustomer.id)
    }

    @Test
    fun `should check if email exists`() {
        assertThat(customerRepository.existsByEmail("test@example.com")).isTrue
        assertThat(customerRepository.existsByEmail("nonexistent@example.com")).isFalse
    }

    @Test
    fun `should find customers by name containing`() {
        entityManager.persist(Customer(name = "김철수", email = "kim1@example.com"))
        entityManager.persist(Customer(name = "김영희", email = "kim2@example.com"))
        entityManager.flush()
        
        val customers = customerRepository.findByNameContaining("김")
        assertThat(customers).hasSize(2)
        
        val testCustomers = customerRepository.findByNameContaining("테스트")
        assertThat(testCustomers).hasSize(1)
    }

    @Test
    fun `should save and find order`() {
        val order = Order(
            customer = testCustomer,
            totalAmount = BigDecimal("150000.00"),
            notes = "테스트 주문"
        )
        val savedOrder = orderRepository.save(order)
        
        val foundOrder = orderRepository.findByIdOrNull(savedOrder.id!!)
        assertThat(foundOrder).isNotNull
        assertThat(foundOrder!!.notes).isEqualTo("테스트 주문")
        assertThat(foundOrder.totalAmount).isEqualTo(BigDecimal("150000.00"))
    }

    @Test
    fun `should find orders by state`() {
        val order1 = entityManager.persist(Order(
            customer = testCustomer,
            totalAmount = BigDecimal("100000.00"),
            state = OrderStates.CREATED
        ))
        val order2 = entityManager.persist(Order(
            customer = testCustomer,
            totalAmount = BigDecimal("200000.00"),
            state = OrderStates.PAID
        ))
        val order3 = entityManager.persist(Order(
            customer = testCustomer,
            totalAmount = BigDecimal("300000.00"),
            state = OrderStates.CREATED
        ))
        entityManager.flush()
        
        val createdOrders = orderRepository.findByState(OrderStates.CREATED)
        val paidOrders = orderRepository.findByState(OrderStates.PAID)
        
        assertThat(createdOrders).hasSize(2)
        assertThat(paidOrders).hasSize(1)
    }

    @Test
    fun `should find orders by customer`() {
        val customer2 = entityManager.persist(Customer(
            name = "고객2",
            email = "customer2@example.com"
        ))
        
        entityManager.persist(Order(customer = testCustomer, totalAmount = BigDecimal("100000.00")))
        entityManager.persist(Order(customer = testCustomer, totalAmount = BigDecimal("200000.00")))
        entityManager.persist(Order(customer = customer2, totalAmount = BigDecimal("300000.00")))
        entityManager.flush()
        
        val customer1Orders = orderRepository.findByCustomerId(testCustomer.id!!)
        val customer2Orders = orderRepository.findByCustomerId(customer2.id!!)
        
        assertThat(customer1Orders).hasSize(2)
        assertThat(customer2Orders).hasSize(1)
    }

    @Test
    fun `should save and find order item`() {
        val order = entityManager.persist(Order(
            customer = testCustomer,
            totalAmount = BigDecimal("100000.00")
        ))
        
        val orderItem = OrderItem(
            order = order,
            productName = "테스트 상품",
            quantity = 2,
            unitPrice = BigDecimal("50000.00")
        )
        orderItem.calculateTotalPrice()
        
        val savedOrderItem = orderItemRepository.save(orderItem)
        
        assertThat(savedOrderItem.id).isNotNull
        assertThat(savedOrderItem.productName).isEqualTo("테스트 상품")
        assertThat(savedOrderItem.quantity).isEqualTo(2)
        assertThat(savedOrderItem.unitPrice).isEqualTo(BigDecimal("50000.00"))
        assertThat(savedOrderItem.totalPrice).isEqualTo(BigDecimal("100000.00"))
    }

    @Test
    fun `should find order items by order`() {
        val order = entityManager.persist(Order(
            customer = testCustomer,
            totalAmount = BigDecimal("200000.00")
        ))
        
        val item1 = OrderItem(
            order = order,
            productName = "상품1",
            quantity = 1,
            unitPrice = BigDecimal("100000.00")
        )
        val item2 = OrderItem(
            order = order,
            productName = "상품2",
            quantity = 2,
            unitPrice = BigDecimal("50000.00")
        )
        
        item1.calculateTotalPrice()
        item2.calculateTotalPrice()
        
        orderItemRepository.save(item1)
        orderItemRepository.save(item2)
        
        val orderItems = orderItemRepository.findByOrderId(order.id!!)
        assertThat(orderItems).hasSize(2)
    }
}