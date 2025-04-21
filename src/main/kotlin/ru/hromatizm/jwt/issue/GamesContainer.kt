package ru.hromatizm.jwt.issue

import org.springframework.stereotype.Component

/**
 * Для простоты игры и участники хранятся не в БД, а в контейнере в памяти
 */
@Component
class GamesContainer {

    private val gameToUsersMap = mutableMapOf<GameId, List<UserDto>>()

    fun getUserList(gameId: String): List<UserDto> {
        return gameToUsersMap[GameId(gameId)]
            ?: throw RuntimeException("Game with id $gameId not found")
    }

    fun put(gameId: String, users: List<UserDto>) {
        gameToUsersMap[GameId(gameId)] = users
    }
}