package persistence

import config.RedisConfig
import storage.StorageService
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*

private const val DEBUG_RDB = false
private inline fun dbg(msg: () -> String) {
    if (DEBUG_RDB) println("[RDB] ${msg()}")
}

class RdbManager(
    private val redisConfig: RedisConfig,
    private val storageService: StorageService,
) {
    private var metadataTerminator: Int = -1
    fun loadFromDisk() {
        val dir = redisConfig.get("dir") ?: return
        val dbfilename = redisConfig.get("dbfilename") ?: return
        val file = File(dir, dbfilename)
        if (!file.exists()) {
            File(dir).mkdirs()
            initRdbFile(file)
            return
        }

        FileInputStream(file).use { fis ->
            loadFromStream(fis)
        }
    }

    fun loadFromStream(input: InputStream) {
        dbg { "enter loadFromStream()" }

        metadataTerminator = -1
        readHeaderSection(input)
        readMetadataSection(input)
        dbg { "metadata section done; terminator=0x${metadataTerminator.toString(16)}" }
        readDataBaseSection(input)
        dbg { "database section done" }
        readEndOfFile(input)
    }

    private fun initRdbFile(file: File) {
        val base64 =
            "UkVESVMwMDEx+glyZWRpcy12ZXIFNy4yLjD6CnJlZGlzLWJpdHPAQPoFY3RpbWXCbQi8ZfoIdXNlZC1tZW3CsMQQAPoIYW9mLWJhc2XAAP/wbjv+wP9aog=="
        val bytes = Base64.getDecoder().decode(base64)
        FileOutputStream(file).use { it.write(bytes) }
    }

    private fun readHeaderSection(fis: InputStream) {
        val header = fis.readNBytes(9)
        dbg { "header raw: ${header.joinToString(" ") { "%02X".format(it) }}" }
        dbg { "header text: ${String(header)}" }
    }

    private fun readMetadataSection(fis: InputStream) {
        while (true) {
            val byte = fis.read()
            if (byte == -1) {
                return
            }
            when (byte) {
                0xFA -> {
                    val k = parseStringValue(fis).also { dbg { "AUX key='$it'" } }
                    val v = parseStringValue(fis).also { dbg { "AUX val='$it'" } }
                }

                0xFE, 0xFF -> {
                    metadataTerminator = byte
                    dbg { "metadata terminator=0x${byte.toString(16)}" }
                    return
                }
                else -> {
                    dbg { "metadata: unknown byte=0x${byte.toString(16)} (skip?)" }
                }
            }
        }
    }

    private fun readDataBaseSection(fis: InputStream) {
        if (metadataTerminator == 0xFF) return
        val dbIndex = fis.read()
        var pendingExpireAtMs: Long? = null
        while (true) {
            val op = fis.read()
            when (op) {
                -1 -> return
                0xFF -> return
                0xFB -> {
                    parseLengthEncodedInt(fis)
                    parseLengthEncodedInt(fis)
                }

                0xFD -> {
                    val seconds = readIntByLittle(fis)
                    pendingExpireAtMs = seconds.toLong() * 1000L
                }

                0xFC -> {
                    pendingExpireAtMs = readLongByLittle(fis)
                }

                0x00 -> {
                    val key = parseKey(fis)
                    val value = parseStringValue(fis)
                    storageService.setString(key, value, pendingExpireAtMs)
                    pendingExpireAtMs = null
                }

                else -> {
                    throw IllegalArgumentException("Unknown opcode in DB section: 0x${op.toString(16)}")
                }
            }
        }
    }

    private fun readEndOfFile(fis: InputStream) {
        val checkSum = fis.readNBytes(8)
        while (fis.read() != -1) {
        }
    }
}
