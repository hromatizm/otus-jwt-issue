package ru.hromatizm.jwt.issue

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class JwtIssueApplication

fun main(args: Array<String>) {
	runApplication<JwtIssueApplication>(*args)
}
