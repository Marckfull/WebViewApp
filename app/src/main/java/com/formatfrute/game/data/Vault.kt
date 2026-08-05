package com.formatfrute.game.data

/**
 * Embaralhador simples do save.
 *
 * Não é criptografia e não pretende ser: sem servidor, quem tem o aparelho na
 * mão sempre vence. O objetivo é só tirar do caminho o "abri o XML e troquei
 * 250 por 999999" — que é o caso real, e o único que dá para barrar de graça.
 *
 * Falha de leitura nunca apaga progresso: devolve null e quem chama usa o
 * valor padrão.
 */
object Vault {

    private const val PREFIX = "v1:"
    private val key = "format-frute-2026".toByteArray()

    // Hexadecimal em vez de Base64 de propósito: não depende de nada do
    // Android, então o Vault roda igual no aparelho e no teste de unidade.
    private const val HEX = "0123456789abcdef"

    fun encode(raw: String): String {
        val bytes = raw.toByteArray(Charsets.UTF_8)
        val body = StringBuilder(bytes.size * 2)
        bytes.forEachIndexed { i, byte ->
            val mixed = (byte.toInt() xor key[i % key.size].toInt()) and 0xFF
            body.append(HEX[mixed ushr 4]).append(HEX[mixed and 0x0F])
        }
        return "$PREFIX${checksum(raw)}:$body"
    }

    fun decode(stored: String?): String? {
        if (stored.isNullOrEmpty()) return null
        // Save antigo, gravado em texto puro: aceita e segue a vida.
        if (!stored.startsWith(PREFIX)) return stored

        val parts = stored.removePrefix(PREFIX).split(':', limit = 2)
        if (parts.size != 2) return null

        val raw = runCatching {
            val body = parts[1]
            if (body.length % 2 != 0) return null
            val bytes = ByteArray(body.length / 2) { i ->
                val high = HEX.indexOf(body[i * 2])
                val low = HEX.indexOf(body[i * 2 + 1])
                if (high < 0 || low < 0) return null
                (((high shl 4) or low) xor key[i % key.size].toInt()).toByte()
            }
            String(bytes, Charsets.UTF_8)
        }.getOrNull() ?: return null

        return if (checksum(raw) == parts[0]) raw else null
    }

    private fun checksum(raw: String): String {
        var hash = 0x811C9DC5.toInt()
        raw.forEach { char ->
            hash = (hash xor char.code) * 0x01000193
        }
        return Integer.toHexString(hash)
    }
}
