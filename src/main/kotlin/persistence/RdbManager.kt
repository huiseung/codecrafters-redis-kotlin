package persistence

import config.RedisConfig
import storage.StorageService
import util.parseExpiryTime
import util.parseKey
import util.parseStringValue
import java.io.File
import java.io.FileInputStream

class RdbManager(
    private val redisConfig: RedisConfig,
    private val storageService: StorageService,
) {
    fun loadFromDisk() {
        val dir = redisConfig.get("dir") ?: return
        val dbfilename = redisConfig.get("dbfilename") ?: return
        val file = File(dir, dbfilename)
        if (!file.exists()) return

        FileInputStream(file).use { fis ->
            readHeaderSection(fis)
            readMetadataSection(fis)
            readDataBaseSection(fis)
            readEndOfFile(fis)
        }
    }

    private fun readHeaderSection(fis: FileInputStream) {
        val header = fis.readNBytes(9)
        while (true) {
            val byte = fis.read()
            if (byte == -1) {
                break
            }
            if (byte == 0xFA) {
                break
            }
        }
    }

    private fun readMetadataSection(fis: FileInputStream) {
        while (true) {
            val byte = fis.read()
            if (byte == -1) {
                break
            }
            if (byte == 0xFE) {
                break
            }
        }
    }

    private fun readDataBaseSection(fis: FileInputStream) {
        val dbIndex = fis.read()

        val tableSizeFlag = fis.read()
        val numOfKey = fis.read()
        val numOfExpiryKey = fis.read()

        repeat(numOfKey-numOfExpiryKey) {
            val valueEncodeType = fis.read()
            var key: String = parseKey(fis)
            var value = parseStringValue(fis)
            storageService.setString(key, value, null)
        }

        repeat(numOfExpiryKey)
        {
            var timeMs = parseExpiryTime(fis)
            val valueEncodeType = fis.read()
            var key: String = parseKey(fis)
            val value = parseStringValue(fis)
            storageService.setString(key, value, timeMs)
        }

        while (true) {
            val byte = fis.read()
            if (byte == -1) {
                break
            }
            if (byte == 0xFF) {
                break
            }
        }
    }

    private fun readEndOfFile(fis: FileInputStream) {
        val checkSum = fis.readNBytes(8)
        while (true) {
            val byte = fis.read()
            if (byte == -1) {
                break
            }
        }
    }
}
