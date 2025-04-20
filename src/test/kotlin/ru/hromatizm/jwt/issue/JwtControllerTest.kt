package ru.hromatizm.jwt.issue

import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Jwts
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.*
import kotlin.test.Test

private const val BEGIN_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----"
private const val END_PUBLIC_KEY = "-----END PUBLIC KEY-----"

@SpringBootTest
@AutoConfigureMockMvc
class JwtControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var keyProvider: KeyProvider

    @MockitoSpyBean
    lateinit var gamesContainer: GamesContainer

    @MockitoSpyBean
    lateinit var jwtService: JwtService

    @Nested
    inner class GetPublicKeyTest {

        private val uri = "/api/public-key"

        @Test
        fun `the answer is OK`() {
            // Arrange
            val request = get(uri)

            // Act
            val result = mockMvc.perform(request)

            // Assert
            result.andExpect(status().isOk)
        }

        @Test
        fun `response contains target key`() {
            // Arrange
            val targetKey =
                "$BEGIN_PUBLIC_KEY\n" +
                        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwYIh0t3ych3yQHVnU52H60WRUuzKZWiv80ocVkBZi/wL8C+aGoyTpZQ46Kx3gmjx9igNuNPMf8ffQjwsfHH2lN4dDM4/oS54wDbWu/6ZdqDWZvIp2kxs4qq3tGiYWwRoB/Q/xc7j2f2pcM0J2Nm9NTKIlVcnYDA4hm/iwE/rawnKEa8dgS99GQy+FJ75Qj0vnzM/xojc72BqJ7HSWmWBN2o3cu8riz4AQD6nhWi9hAzqKMfp+dtR0gixA6IFx2dEf6sjcg6rfYXNj1tClJAFd5DVFDmmtVxrrAQRgsZtUWR1sdgdNC0NfevUV7f2DDhAdlbxb+tGCn5wsM56d3o5bwIDAQAB\n" +
                        "$END_PUBLIC_KEY"

            val request = get(uri)

            // Act
            val responseContent = mockMvc
                .perform(request)
                .andReturn().response.contentAsString

            // Assert
            assertThat(responseContent).isEqualTo(targetKey)
        }

        @Test
        fun `received key is RSAPublicKey`() {
            // Arrange
            val request = get(uri)

            // Act
            val responseContent = mockMvc
                .perform(request)
                .andReturn().response.contentAsString

            //
            val base64Key = responseContent
                .replace(BEGIN_PUBLIC_KEY, "")
                .replace(END_PUBLIC_KEY, "")
                .replace("\\s+".toRegex(), "")
            val decodedKey = Base64.getDecoder().decode(base64Key)
            val keySpec = X509EncodedKeySpec(decodedKey)
            val publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec)
            assertThat(publicKey).isInstanceOf(RSAPublicKey::class.java)
        }
    }

    @Nested
    inner class InitGameTest {

        private val uri = "/api/init-game"
        private val userList = UserListDto(
            users = listOf(
                UserDto("user_id_1", "user_nic_1"),
                UserDto("user_id_2", "user_nic_2"),
            )
        )

        @Test
        fun `the answer is OK`() {
            // Arrange
            val request = createInitGameRequest(userList)

            // Act
            val resultActions: ResultActions = mockMvc.perform(request)

            // Assert
            resultActions.andExpect(status().isOk)
        }

        @Test
        fun `jwt service is invoked`() {
            // Arrange
            val request = createInitGameRequest(userList)

            // Act
            mockMvc.perform(request)

            // Assert
            verify(jwtService).initGame(userList)
        }

        @Test
        fun `game uuid is returned`() {
            // Arrange
            val request = createInitGameRequest(userList)

            // Act
            val responseContent = mockMvc
                .perform(request)
                .andReturn().response.contentAsString

            // Assert
            val gameUuid = UUID.fromString(responseContent)
            assertThat(gameUuid).isNotNull()
        }

        @Test
        fun `games container contains users for the game`() {
            // Arrange
            val request = createInitGameRequest(userList)

            // Act
            val gameUuid = mockMvc
                .perform(request)
                .andReturn().response.contentAsString

            // Assert
            assertThat(gamesContainer.getUserList(gameUuid))
                .containsExactlyInAnyOrderElementsOf(userList.users)
        }

        private fun createInitGameRequest(userList: UserListDto): MockHttpServletRequestBuilder {
            val messageJson = objectMapper.writeValueAsString(userList)
            return post(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(messageJson)
        }
    }

    @Nested
    inner class IssueJwtTest {

        private val uri = "/api/issue-jwt"

        @Test
        fun `the answer is OK`() {
            // Arrange
            val gameId = UUID.randomUUID().toString()
            val user = UserDto("user_id_1", "user_nic_1")
            val issueJwtDto = IssueJwtDto(
                gameId = gameId,
                user = user
            )
            doReturn(listOf(user)).`when`(gamesContainer).getUserList(gameId)
            val request = createIssueJwtRequest(issueJwtDto)

            // Act
            val resultActions: ResultActions = mockMvc.perform(request)

            // Assert
            resultActions.andExpect(status().isOk)
        }

        @Test
        fun `jwt service is invoked`() {
            // Arrange
            val gameId = UUID.randomUUID().toString()
            val user = UserDto("user_id_1", "user_nic_1")
            val issueJwtDto = IssueJwtDto(
                gameId = gameId,
                user = user
            )
            doReturn(listOf(user)).`when`(gamesContainer).getUserList(gameId)
            val request = createIssueJwtRequest(issueJwtDto)

            // Act
            mockMvc.perform(request)

            // Assert
            verify(jwtService).issueJwt(issueJwtDto)
        }

        @Test
        fun `jwt is returned`() {
            // Arrange
            val gameId = UUID.randomUUID().toString()
            val user = UserDto("user_id_1", "user_nic_1")
            val issueJwtDto = IssueJwtDto(
                gameId = gameId,
                user = user
            )
            doReturn(listOf(user)).`when`(gamesContainer).getUserList(gameId)
            val request = createIssueJwtRequest(issueJwtDto)

            // Act
            val jwt = mockMvc
                .perform(request)
                .andReturn().response.contentAsString

            // Assert
            assertThat(jwt).isNotBlank
            assertThat(jwt.split(".")).hasSize(3)

            val claims = Jwts.parserBuilder()
                .setSigningKey(keyProvider.publicKey)
                .build()
                .parseClaimsJws(jwt)
                .body
            assertThat(claims["gameId"]).isEqualTo(gameId)
            assertThat(claims.subject).isEqualTo(user.nic)
        }

        private fun createIssueJwtRequest(dto: IssueJwtDto): MockHttpServletRequestBuilder {
            val messageJson = objectMapper.writeValueAsString(dto)
            return post(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(messageJson)
        }
    }
}
