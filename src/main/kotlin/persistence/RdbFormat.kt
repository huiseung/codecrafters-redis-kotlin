package persistence

import java.io.InputStream
import java.lang.IllegalArgumentException

fun parseKey(fis: InputStream): String {
    val firstByte = fis.read()
    val top2Bits = (firstByte shr 6) and 0b11
    return when (top2Bits) {
        0b11 -> parsing11(fis, firstByte).toString()
        else -> String(getByteArray(fis, top2Bits, firstByte), Charsets.UTF_8)
    }
}

fun parseStringValue(fis: InputStream): String {
    val firstByte = fis.read()
    val top2Bits = (firstByte shr 6) and 0b11
    return if (top2Bits == 0b11) {
        parsing11(fis, firstByte).toString()
    } else {
        String(getByteArray(fis, top2Bits, firstByte), Charsets.UTF_8)
    }
}


private fun getByteArray(fis: InputStream, top2Bits: Int, firstByte: Int): ByteArray {
    return when (top2Bits) {
        0b00 -> parsing00(fis, firstByte)
        0b01 -> parsing01(fis, firstByte)
        0b10 -> parsing10(fis, firstByte)
        else -> throw IllegalArgumentException()
    }
}

private fun parsing00(fis: InputStream, firstByte: Int): ByteArray {
    val length = firstByte and 0b00111111
    return fis.readNBytes(length)
}

private fun parsing01(fis: InputStream, firstByte: Int): ByteArray {
    val high6 = firstByte and 0b00111111
    val low8 = fis.read() and 0xFF
    val length = (high6 shl 8) or low8
    return fis.readNBytes(length)
}

private fun parsing10(fis: InputStream, firstByte: Int): ByteArray {
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

private fun parsing11(fis: InputStream, firstByte: Int): Int {
    return when (firstByte) {
        0xC0 -> fis.read()
        0xC1 -> readShortByteLittle(fis)
        0xC2 -> readIntByLittle(fis)
        else -> throw IllegalArgumentException()
    }
}

fun readLongByLittle(fis: InputStream): Long {
    var timeMs = 0L
    for (i in 1..8) {
        timeMs = timeMs or (fis.read().toLong() and 0xFF shl (8 * (i - 1)))
    }
    return timeMs
}

fun readIntByLittle(fis: InputStream): Int {
    val b1 = fis.read()
    val b2 = fis.read()
    val b3 = fis.read()
    val b4 = fis.read()
    return ((b4 and 0xFF) shl 24) or
            ((b3 and 0xFF) shl 16) or
            ((b2 and 0xFF) shl 8) or
            (b1 and 0xFF)
}

private fun readShortByteLittle(fis: InputStream): Int {
    val b1 = fis.read()
    val b2 = fis.read()
    return ((b2 and 0xFF) shl 8) or (b1 and 0xFF)
}

fun parseLengthEncodedInt(fis: InputStream): Int {
    val first = fis.read()
    if (first == -1) throw IllegalArgumentException("Unexpected EOF in length")
    val top2 = (first shr 6) and 0b11
    return when (top2) {
        0b00 -> first and 0b0011_1111
        0b01 -> {
            val b2 = fis.read() and 0xFF
            ((first and 0b0011_1111) shl 8) or b2
        }

        0b10 -> {
            val b1 = fis.read() and 0xFF
            val b2 = fis.read() and 0xFF
            val b3 = fis.read() and 0xFF
            val b4 = fis.read() and 0xFF
            (b1 shl 24) or (b2 shl 16) or (b3 shl 8) or b4
        }

        else -> {
            throw IllegalArgumentException("Special-encoded length unsupported here")
        }
    }
}
