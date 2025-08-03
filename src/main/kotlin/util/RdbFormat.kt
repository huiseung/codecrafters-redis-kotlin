package util

import java.io.FileInputStream
import java.lang.IllegalArgumentException

fun parseKey(fis: FileInputStream): String {
    val firstByte = fis.read()
    val top2Bits = (firstByte shr 6) and 0b11
    return when (top2Bits) {
        0b11 -> parsing11(fis, firstByte).toString()
        else -> String(getByteArray(fis, top2Bits, firstByte), Charsets.UTF_8)
    }
}

fun parseStringValue(fis: FileInputStream): String {
    val firstByte = fis.read()
    println("first: $firstByte")
    val top2Bits = (firstByte shr 6) and 0b11
    return if (top2Bits == 0b11) {
        parsing11(fis, firstByte).toString()
    } else {
        String(getByteArray(fis, top2Bits, firstByte), Charsets.UTF_8)
    }
}

fun parseExpiryTime(fis: FileInputStream): Long {
    val timeType = fis.read()
    return when (timeType) {
        0xFC -> readLongByLittle(fis)
        0xFD -> readIntByLittle(fis).toLong() * 1000
        else -> throw IllegalArgumentException()
    }
}


private fun getByteArray(fis: FileInputStream, top2Bits: Int, firstByte: Int): ByteArray {
    return when (top2Bits) {
        0b00 -> parsing00(fis, firstByte)
        0b01 -> parsing01(fis, firstByte)
        0b10 -> parsing10(fis, firstByte)
        else -> throw IllegalArgumentException()
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
        0xC0 -> fis.read()
        0xC1 -> readShortByteLittle(fis)
        0xC2 -> readIntByLittle(fis)
        else -> throw IllegalArgumentException()
    }
}

private fun readLongByLittle(fis: FileInputStream): Long {
    var timeMs = 0L
    for (i in 1..8) {
        timeMs = timeMs or (fis.read().toLong() and 0xFF shl (8 * (i - 1)))
    }
    return timeMs
}

private fun readIntByLittle(fis: FileInputStream): Int {
    val b1 = fis.read()
    val b2 = fis.read()
    val b3 = fis.read()
    val b4 = fis.read()
    return ((b4 and 0xFF) shl 24) or
            ((b3 and 0xFF) shl 16) or
            ((b2 and 0xFF) shl 8) or
            (b1 and 0xFF)
}

private fun readShortByteLittle(fis: FileInputStream): Int {
    val b1 = fis.read()
    val b2 = fis.read()
    return ((b2 and 0xFF) shl 8) or (b1 and 0xFF)
}
