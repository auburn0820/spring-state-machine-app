package org.suyeong.springstatemachineapp.repository

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.suyeong.springstatemachineapp.entity.Customer
import org.suyeong.springstatemachineapp.entity.Order
import org.suyeong.springstatemachineapp.entity.OrderItem
import org.suyeong.springstatemachineapp.entity.Payment
import org.suyeong.springstatemachineapp.entity.PaymentMethod
import org.suyeong.springstatemachineapp.entity.PaymentStatus
import org.suyeong.springstatemachineapp.statemachine.OrderStates
import java.math.BigDecimal

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DataAwareRepositoryTest {

    @Autowired
    private lateinit var customerRepository: CustomerRepository

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var orderItemRepository: OrderItemRepository

    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    @Test
    fun `should work with database state`() {
        // 데이터베이스가 작동하는지 확인
        val initialCustomerCount = customerRepository.count()
        
        // 새 데이터 추가 가능한지 확인
        val customer = customerRepository.save(
            Customer(
                name = "테스트용 고객",
                email = "db-test@example.com"
            )
        )
        
        assertThat(customerRepository.count()).isEqualTo(initialCustomerCount + 1)
        assertThat(customer.id).isNotNull
    }

    @Test
    fun `should create and find customer`() {
        val customer = customerRepository.save(
            Customer(
                name = "테스트 고객",
                email = "test@example.com",
                phoneNumber = "010-1234-5678",
                address = "서울시 강남구"
            )
        )

        val foundCustomer = customerRepository.findByIdOrNull(customer.id!!)
        assertThat(foundCustomer).isNotNull
        assertThat(foundCustomer!!.name).isEqualTo("테스트 고객")
    }

    @Test
    fun `should create order with customer and items`() {
        // Create customer first
        val customer = customerRepository.save(
            Customer(
                name = "주문 테스트 고객",
                email = "order-test@example.com"
            )
        )

        // Create order
        val order = Order(
            customer = customer,
            totalAmount = BigDecimal("100000.00"),
            notes = "테스트 주문"
        )
        val savedOrder = orderRepository.save(order)

        // Create order items
        val orderItem1 = OrderItem(
            order = savedOrder,
            productName = "테스트 상품 1",
            quantity = 2,
            unitPrice = BigDecimal("30000.00")
        )
        orderItem1.calculateTotalPrice()

        val orderItem2 = OrderItem(
            order = savedOrder,
            productName = "테스트 상품 2",
            quantity = 1,
            unitPrice = BigDecimal("40000.00")
        )
        orderItem2.calculateTotalPrice()

        val savedItem1 = orderItemRepository.save(orderItem1)
        val savedItem2 = orderItemRepository.save(orderItem2)

        // Verify items were saved correctly
        assertThat(savedItem1.id).isNotNull
        assertThat(savedItem2.id).isNotNull

        // Verify we can find items by order ID
        val orderItems = orderItemRepository.findByOrderId(savedOrder.id!!)
        assertThat(orderItems).hasSize(2)
    }

    @Test
    fun `should find orders by state`() {
        val customer = customerRepository.save(
            Customer(
                name = "상태 테스트 고객",
                email = "state-test@example.com"
            )
        )

        // Create orders with different states
        val createdOrder = orderRepository.save(
            Order(
                customer = customer,
                totalAmount = BigDecimal("100000.00"),
                state = OrderStates.CREATED
            )
        )

        val paidOrder = orderRepository.save(
            Order(
                customer = customer,
                totalAmount = BigDecimal("200000.00"),
                state = OrderStates.PAID
            )
        )

        // Find by state
        val createdOrders = orderRepository.findByState(OrderStates.CREATED)
        val paidOrders = orderRepository.findByState(OrderStates.PAID)

        // Should find our created orders
        assertThat(createdOrders).anyMatch { it.id == createdOrder.id }
        assertThat(paidOrders).anyMatch { it.id == paidOrder.id }
    }

    @Test
    fun `should find orders by customer`() {
        val customer1 = customerRepository.save(
            Customer(
                name = "고객 1",
                email = "customer1@example.com"
            )
        )

        val customer2 = customerRepository.save(
            Customer(
                name = "고객 2",
                email = "customer2@example.com"
            )
        )

        // Create orders for different customers
        orderRepository.save(
            Order(
                customer = customer1,
                totalAmount = BigDecimal("100000.00")
            )
        )

        orderRepository.save(
            Order(
                customer = customer1,
                totalAmount = BigDecimal("150000.00")
            )
        )

        orderRepository.save(
            Order(
                customer = customer2,
                totalAmount = BigDecimal("200000.00")
            )
        )

        // Find orders by customer
        val customer1Orders = orderRepository.findByCustomerId(customer1.id!!)
        val customer2Orders = orderRepository.findByCustomerId(customer2.id!!)

        assertThat(customer1Orders).hasSize(2)
        assertThat(customer2Orders).hasSize(1)
    }

    @Test
    fun `should create and process payment`() {
        val customer = customerRepository.save(
            Customer(
                name = "결제 테스트 고객",
                email = "payment-test@example.com"
            )
        )

        val order = orderRepository.save(
            Order(
                customer = customer,
                totalAmount = BigDecimal("150000.00")
            )
        )

        val payment = Payment(
            order = order,
            amount = BigDecimal("150000.00"),
            paymentMethod = PaymentMethod.CREDIT_CARD,
            paymentStatus = PaymentStatus.PENDING
        )

        val savedPayment = paymentRepository.save(payment)

        // Find payment by order
        val foundPayment = paymentRepository.findByOrderId(order.id!!)
        assertThat(foundPayment).isNotNull
        assertThat(foundPayment!!.paymentMethod).isEqualTo(PaymentMethod.CREDIT_CARD)
        assertThat(foundPayment.paymentStatus).isEqualTo(PaymentStatus.PENDING)

        // Update payment status
        savedPayment.markAsCompleted("TXN_TEST_001")
        paymentRepository.save(savedPayment)

        val updatedPayment = paymentRepository.findByIdOrNull(savedPayment.id!!)
        assertThat(updatedPayment).isNotNull
        assertThat(updatedPayment!!.paymentStatus).isEqualTo(PaymentStatus.COMPLETED)
        assertThat(updatedPayment.transactionId).isEqualTo("TXN_TEST_001")
        assertThat(updatedPayment.processedAt).isNotNull
    }

    @Test
    fun `should find payments by status`() {
        val customer = customerRepository.save(
            Customer(
                name = "결제 상태 테스트 고객",
                email = "payment-status-test@example.com"
            )
        )

        val order1 = orderRepository.save(
            Order(
                customer = customer,
                totalAmount = BigDecimal("100000.00")
            )
        )

        val order2 = orderRepository.save(
            Order(
                customer = customer,
                totalAmount = BigDecimal("200000.00")
            )
        )

        // Create payments with different statuses
        paymentRepository.save(
            Payment(
                order = order1,
                amount = BigDecimal("100000.00"),
                paymentMethod = PaymentMethod.CREDIT_CARD,
                paymentStatus = PaymentStatus.COMPLETED
            )
        )

        paymentRepository.save(
            Payment(
                order = order2,
                amount = BigDecimal("200000.00"),
                paymentMethod = PaymentMethod.BANK_TRANSFER,
                paymentStatus = PaymentStatus.PENDING
            )
        )

        // Find by status
        val completedPayments = paymentRepository.findByPaymentStatus(PaymentStatus.COMPLETED)
        val pendingPayments = paymentRepository.findByPaymentStatus(PaymentStatus.PENDING)

        // Should find our payments
        assertThat(completedPayments).anyMatch { it.order?.id == order1.id }
        assertThat(pendingPayments).anyMatch { it.order?.id == order2.id }
    }

    @Test
    fun `should handle customer email uniqueness`() {
        customerRepository.save(
            Customer(
                name = "첫 번째 고객",
                email = "unique@example.com"
            )
        )

        // Try to create another customer with same email
        assertThatThrownBy {
            customerRepository.save(
                Customer(
                    name = "두 번째 고객",
                    email = "unique@example.com" // Same email
                )
            )
            customerRepository.flush() // Force the constraint check
        }.isInstanceOf(Exception::class.java)
    }
}