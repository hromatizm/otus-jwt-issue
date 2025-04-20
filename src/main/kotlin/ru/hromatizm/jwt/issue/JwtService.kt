package ru.hromatizm.jwt.issue

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.stereotype.Service
import java.util.*

@Service
class JwtService(
    private val keyProvider: KeyProvider,
    private val gamesContainer: GamesContainer
) {

    private val base64Encoder = Base64.getEncoder()

    val publicKey: String = run {
        val keyBytes = keyProvider.publicKey.encoded
        val keyBase64 = base64Encoder.encodeToString(keyBytes)
        "-----BEGIN PUBLIC KEY-----\n$keyBase64\n-----END PUBLIC KEY-----"
    }

    fun initGame(users: UserListDto): GameId {
        val gameId = UUID.randomUUID().toString()
        gamesContainer.put(gameId = gameId, users = users.users)
        return GameId(gameId)
    }

    fun issueJwt(issueJwtDto: IssueJwtDto): String {
        val userList = gamesContainer.getUserList(gameId = issueJwtDto.gameId)
        val user = userList.findUser(issueJwtDto)
        return buildJwt(gameId = issueJwtDto.gameId, user = user)
    }

    private fun List<UserDto>.findUser(issueJwtDto: IssueJwtDto): UserDto {
        val user = this.firstOrNull { it == issueJwtDto.user }
            ?: throw RuntimeException("User is not authorized to participate in the game ${issueJwtDto.gameId}")
        return user
    }

    private fun buildJwt(user: UserDto, gameId: String): String {
        val now = Date()
        val expiry = Date(now.time + 3600_000)
        return Jwts.builder()
            .setSubject(user.nic)
            .claim("gameId", gameId)
            .setIssuedAt(now)
            .setExpiration(expiry)
            .signWith(keyProvider.privateKey, SignatureAlgorithm.RS256)
            .compact()
    }

}
