package ru.hromatizm.jwt.issue

import org.springframework.stereotype.Component
import java.io.InputStream
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

@Component
class KeyProvider {

    private val keyFactory = KeyFactory.getInstance("RSA")

    val privateKey: PrivateKey = run {
        val pem: ByteArray = getPem("/keys/private.pem")
        val keySpec = PKCS8EncodedKeySpec(pem)
        keyFactory.generatePrivate(keySpec)
    }

    val publicKey: PublicKey = run {
        val pem: ByteArray = getPem("/keys/public.pem")
        val spec = X509EncodedKeySpec(pem)
        keyFactory.generatePublic(spec)
    }

    private fun getPem(path: String): ByteArray {
        val resource: InputStream = javaClass.getResourceAsStream(path)
            ?: throw RuntimeException("Path $path not found")
        val keyBytes: ByteArray = resource.use { it.readAllBytes() }
        return parsePem(keyBytes)
    }

    private fun parsePem(pemBites: ByteArray): ByteArray {
        val pemStr = String(pemBites)
            .replace("-----BEGIN.*?-----".toRegex(), "")
            .replace("-----END.*?-----".toRegex(), "")
            .replace("\\s+".toRegex(), "")
        return Base64.getDecoder().decode(pemStr)
    }
}