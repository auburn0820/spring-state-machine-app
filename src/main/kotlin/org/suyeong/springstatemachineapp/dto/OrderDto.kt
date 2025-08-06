package org.suyeong.springstatemachineapp.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.suyeong.springstatemachineapp.entity.Order
import org.suyeong.springstatemachineapp.entity.OrderItem
import org.suyeong.springstatemachineapp.statemachine.OrderStates
import java.math.BigDecimal
import java.time.LocalDateTime

data class CreateOrderRequest(
  @field:NotNull(message = "Customer ID is required")
  @field:Positive(message = "Customer ID must be positive")
  val customerId: Long,

  @field:NotEmpty(message = "Order items are required")
  @field:Valid
  val orderItems: List<CreateOrderItemRequest>,

  @field:Size(max = 500, message = "Notes must not exceed 500 characters")
  val notes: String? = null
)

data class CreateOrderItemRequest(
  @field:NotBlank(message = "Product name is required")
  @field:Size(max = 200, message = "Product name must not exceed 200 characters")
  val productName: String,

  @field:NotNull(message = "Quantity is required")
  @field:Min(value = 1, message = "Quantity must be at least 1")
  val quantity: Int,

  @field:NotNull(message = "Unit price is required")
  @field:DecimalMin(value = "0.00", inclusive = false, message = "Unit price must be greater than 0")
  val unitPrice: BigDecimal
)

data class OrderResponse(
  val id: Long,
  val customerId: Long,
  val customerName: String,
  val totalAmount: BigDecimal,
  val state: OrderStates,
  val notes: String?,
  val createdAt: LocalDateTime,
  val updatedAt: LocalDateTime?,
  val orderItems: List<OrderItemResponse> = emptyList()
) {
  companion object {
    fun from(order: Order, includeItems: Boolean = false): OrderResponse {
      return OrderResponse(
        id = order.id!!,
        customerId = order.customer?.id ?: 0L,
        customerName = order.customer?.name ?: "",
        totalAmount = order.totalAmount,
        state = order.state,
        notes = order.notes,
        createdAt = order.createdAt,
        updatedAt = order.updatedAt,
        orderItems = if (includeItems) order.orderItems.map { OrderItemResponse.from(it) } else emptyList()
      )
    }
  }
}

data class OrderSummaryResponse(
  val id: Long,
  val customerId: Long,
  val customerName: String,
  val totalAmount: BigDecimal,
  val state: OrderStates,
  val createdAt: LocalDateTime
) {
  companion object {
    fun from(order: Order): OrderSummaryResponse {
      return OrderSummaryResponse(
        id = order.id!!,
        customerId = order.customer?.id ?: 0L,
        customerName = order.customer?.name ?: "",
        totalAmount = order.totalAmount,
        state = order.state,
        createdAt = order.createdAt
      )
    }
  }
}

data class OrderItemResponse(
  val id: Long,
  val productName: String,
  val quantity: Int,
  val unitPrice: BigDecimal,
  val totalPrice: BigDecimal
) {
  companion object {
    fun from(orderItem: OrderItem): OrderItemResponse {
      return OrderItemResponse(
        id = orderItem.id!!,
        productName = orderItem.productName,
        quantity = orderItem.quantity,
        unitPrice = orderItem.unitPrice,
        totalPrice = orderItem.totalPrice
      )
    }
  }
}