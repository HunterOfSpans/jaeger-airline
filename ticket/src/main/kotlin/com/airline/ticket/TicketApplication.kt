package com.airline.ticket

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication
@EnableFeignClients
class TicketApplication

fun main(args: Array<String>) {
	runApplication<TicketApplication>(*args)
}
