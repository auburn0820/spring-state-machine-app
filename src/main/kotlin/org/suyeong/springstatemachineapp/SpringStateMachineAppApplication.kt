package org.suyeong.springstatemachineapp

import kotlinx.coroutines.runBlocking
import mu.KLogging
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.suyeong.springstatemachineapp.entity.Customer
import org.suyeong.springstatemachineapp.entity.Order
import org.suyeong.springstatemachineapp.entity.OrderItem
import org.suyeong.springstatemachineapp.repository.CustomerRepository
import org.suyeong.springstatemachineapp.repository.OrderRepository
import org.suyeong.springstatemachineapp.statemachine.OrderEvents
import org.suyeong.springstatemachineapp.statemachine.OrderStateMachineService
import org.suyeong.springstatemachineapp.statemachine.OrderStates
import java.math.BigDecimal
import java.time.LocalDateTime

@SpringBootApplication
class SpringStateMachineAppApplication

fun main(args: Array<String>) {
    runApplication<SpringStateMachineAppApplication>(*args)
}
