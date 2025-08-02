import java.io.File
import java.io.FileInputStream
import java.lang.IllegalArgumentException

class RdbManager(
    private val redisConfig: RedisConfig,
    private val storageService: StorageService,
) {
    fun loadFromDisk() {
        if (redisConfig.get("dir") == null || redisConfig.get("dbfilename") == null) {
            return
        }
        if (!File(redisConfig.get("dir"), redisConfig.get("dbfilename")).exists()) {
            return
        }
        FileInputStream(redisConfig.get("dir") + "/" + redisConfig.get("dbfilename")).use { fis ->
            readHeaderSection(fis)
            readMetadataSection(fis)
            readDataBaseSection(fis)
            readEndOfFile(fis)
        }
    }

    private fun parsing00(fis: FileInputStream, firstByte: Int): ByteArray {
        val length = firstByte and 0b00111111
        return fis.readNBytes(length)
    }

    private fun parsing01(fis: FileInputStream, firstByte: Int): ByteArray {
        val high6 = firstByte and 0b00111111
        val low8 = fis.read() and 0xFF
        val length = (high6 shl 8) or low8
        return fis.readNBytes(length)
    }

    private fun parsing10(fis: FileInputStream, firstByte: Int): ByteArray {
        val b1 = fis.read()
        val b2 = fis.read()
        val b3 = fis.read()
        val b4 = fis.read()
        val length = ((b1 and 0xFF) shl 24) or
                ((b2 and 0xFF) shl 16) or
                ((b3 and 0xFF) shl 8) or
                (b4 and 0xFF)
        return fis.readNBytes(length)
    }

    private fun parsing11(fis: FileInputStream, firstByte: Int): Int {
        return when (firstByte) {
            0xC0 -> {
                fis.read()
            }

            0xC1 -> {
                val b1 = fis.read()
                val b2 = fis.read()
                ((b2 and 0xFF) shl 8) or (b1 and 0xFF)
            }

            0xC2 -> {
                val b1 = fis.read()
                val b2 = fis.read()
                val b3 = fis.read()
                val b4 = fis.read()
                ((b4 and 0xFF) shl 24) or ((b3 and 0xFF) shl 16) or ((b2 and 0xFF) shl 8) or (b1 and 0xFF)
            }

            else -> throw IllegalArgumentException()
        }
    }

    private fun parseKey(fis: FileInputStream): String {
        var firstByte = fis.read()
        var top2Bits = (firstByte shr 6) and 0b11
        var key = ""
        if (top2Bits == 0b11) {
            val int = parsing11(fis, firstByte)
            key = int.toString()
        } else {
            val bytes = when (top2Bits) {
                0b00 -> {
                    parsing00(fis, firstByte)
                }

                0b01 -> {
                    parsing01(fis, firstByte)
                }

                0b10 -> {
                    parsing10(fis, firstByte)
                }

                else -> throw IllegalArgumentException()
            }
            key = String(bytes, Charsets.UTF_8)
        }
        return key
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
        val numOfNonExpiryKey = fis.read()
        val numOfExpiryKey = fis.read()

        repeat(numOfNonExpiryKey) {
            val valueEncodeType = fis.read()
            var key: String = parseKey(fis)
            //
            var firstByte = fis.read()
            var top2Bits = (firstByte shr 6) and 0b11
            var value = ""
            if (top2Bits == 0b11) {
                val int = parsing11(fis, firstByte)
                if (valueEncodeType == 0) {
                    value = int.toString()
                }
            } else {
                val bytes = when (top2Bits) {
                    0b00 -> {
                        parsing00(fis, firstByte)
                    }

                    0b01 -> {
                        parsing01(fis, firstByte)
                    }

                    0b10 -> {
                        parsing10(fis, firstByte)
                    }

                    else -> throw IllegalArgumentException()
                }
                if (valueEncodeType == 0) {
                    value = String(bytes, Charsets.UTF_8)
                }
            }

            storageService.setString(key, value, null)
        }
        repeat(numOfExpiryKey)
        {

            val timeType = fis.read()
            var timeMs = 0L
            if (timeType == 0xFC) {
                val b1 = fis.read()
                val b2 = fis.read()
                val b3 = fis.read()
                val b4 = fis.read()
                val b5 = fis.read()
                val b6 = fis.read()
                val b7 = fis.read()
                val b8 = fis.read()

                timeMs = ((b8.toLong() and 0xFF) shl 56) or
                        ((b7.toLong() and 0xFF) shl 48) or
                        ((b6.toLong() and 0xFF) shl 40) or
                        ((b5.toLong() and 0xFF) shl 32) or
                        ((b4.toLong() and 0xFF) shl 24) or
                        ((b3.toLong() and 0xFF) shl 16) or
                        ((b2.toLong() and 0xFF) shl 8) or
                        (b1.toLong() and 0xFF)

            } else if (timeType == 0xFD) {
                val b1 = fis.read()
                val b2 = fis.read()
                val b3 = fis.read()
                val b4 = fis.read()

                val timeSec = ((b4 and 0xFF) shl 24) or
                        ((b3 and 0xFF) shl 16) or
                        ((b2 and 0xFF) shl 8) or
                        (b1 and 0xFF)
                timeMs = timeSec.toLong() * 1000
            }

            //
            val valueEncodeType = fis.read()
            var key: String = parseKey(fis)
            //
            var firstByte = fis.read()
            var top2Bits = (firstByte shr 6) and 0b11
            var value = ""
            if (top2Bits == 0b11) {
                val int = parsing11(fis, firstByte)
                if (valueEncodeType == 0) {
                    value = int.toString()
                }
            } else {
                val bytes = when (top2Bits) {
                    0b00 -> {
                        parsing00(fis, firstByte)
                    }

                    0b01 -> {
                        parsing01(fis, firstByte)
                    }

                    0b10 -> {
                        parsing10(fis, firstByte)
                    }

                    else -> throw IllegalArgumentException()
                }
                if (valueEncodeType == 0) {
                    value = String(bytes, Charsets.UTF_8)
                }
            }
            //
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
