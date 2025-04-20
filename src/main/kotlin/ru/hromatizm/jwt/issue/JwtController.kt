package ru.hromatizm.jwt.issue

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class JwtController(
    private val jwtService: JwtService
) {

    @GetMapping("/public-key")
    fun getPublicKey(): ResponseEntity<String> {
        val key = jwtService.publicKey
        return ResponseEntity.ok().body(key)
    }

    @PostMapping("/init-game")
    fun initGame(@RequestBody users: UserListDto): ResponseEntity<String> {
        val gameId = jwtService.initGame(users)
        return ResponseEntity.ok().body(gameId.id)
    }

    @PostMapping("/issue-jwt")
    fun issueJwt(@RequestBody issueJwtDto: IssueJwtDto): ResponseEntity<String> {
        val jwt = jwtService.issueJwt(issueJwtDto)
        return ResponseEntity.ok().body(jwt)
    }
}
