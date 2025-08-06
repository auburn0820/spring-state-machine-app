package org.suyeong.springstatemachineapp.exception

class OrderNotFoundException(message: String) : RuntimeException(message) {
    constructor(orderId: Long) : this("Order with id $orderId not found")
}

class PaymentNotFoundException(message: String) : RuntimeException(message) {
    constructor(paymentId: Long) : this("Payment with id $paymentId not found")
}

class PaymentAlreadyExistsException(message: String) : IllegalStateException(message) {
    constructor(orderId: Long) : this("Payment already exists for order $orderId")
}

class InvalidPaymentStatusException(message: String) : IllegalStateException(message)

class PaymentProcessingFailedException(message: String, cause: Throwable? = null) : RuntimeException("Payment processing failed: $message", cause)

class RefundProcessingFailedException(message: String, cause: Throwable? = null) : RuntimeException("Refund processing failed: $message", cause)