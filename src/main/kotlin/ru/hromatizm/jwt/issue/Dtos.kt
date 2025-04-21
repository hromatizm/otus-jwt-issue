package ru.hromatizm.jwt.issue

data class UserDto(
    val id: String,
    val nic: String,
)

data class UserListDto(
    val users: List<UserDto>
)

data class IssueJwtDto(
    val gameId: String,
    val user: UserDto,
)