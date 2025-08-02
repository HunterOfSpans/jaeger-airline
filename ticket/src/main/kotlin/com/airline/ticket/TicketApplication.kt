package com.airline.ticket

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TicketApplication

fun main(args: Array<String>) {
	runApplication<TicketApplication>(*args)
}
