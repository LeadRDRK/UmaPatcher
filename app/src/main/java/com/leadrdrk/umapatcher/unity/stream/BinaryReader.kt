package com.leadrdrk.umapatcher.unity.stream

import com.leadrdrk.umapatcher.unity.InvalidValueException
import com.leadrdrk.umapatcher.unity.OutOfRangeException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val NULL = 0.toByte()

open class BinaryReader(
    protected val stream: InputStream
): InputStream() {
    private val longArray = ByteArray(8)
    private val longBuffer = ByteBuffer.wrap(longArray)

    var order: ByteOrder
        get() = longBuffer.order()
        set(v) { longBuffer.order(v) }

    override fun close() = stream.close()
    override fun read() = stream.read()
    override fun read(b: ByteArray) = stream.read(b)
    override fun read(b: ByteArray, off: Int, len: Int) =
        stream.read(b, off, len)
    override fun reset() = stream.reset()
    override fun markSupported() = stream.markSupported()
    override fun skip(n: Long) = stream.skip(n)
    override fun available() = stream.available()
    override fun mark(readlimit: Int) = stream.mark(readlimit)

    fun readBytes(b: ByteArray, off: Int, len: Int): Int {
        val res = read(b, off, len)
        if (res != len) throw OutOfRangeException()
        return res
    }

    fun readBytes(len: Int): ByteArray {
        val b = ByteArray(len)
        readBytes(b, 0, len)
        return b
    }

    fun readUInt8(): UByte {
        val v = read()
        if (v == -1) throw OutOfRangeException()
        return v.toUByte()
    }

    fun readInt8(): Byte = readUInt8().toByte()

    fun readBoolean(): Boolean {
        val v = read()
        if (v == -1) throw OutOfRangeException()
        return v != 0
    }

    fun readInt16(): Short {
        if (read(longArray, 0, 2) != 2) throw OutOfRangeException()
        longBuffer.clear()
        return longBuffer.getShort()
    }

    fun readInt32(): Int {
        if (read(longArray, 0, 4) != 4) throw OutOfRangeException()
        longBuffer.clear()
        return longBuffer.getInt()
    }

    fun readInt64(): Long {
        if (read(longArray, 0, 8) != 8) throw OutOfRangeException()
        longBuffer.clear()
        return longBuffer.getLong()
    }

    fun readUInt16(): UShort = readInt16().toUShort()
    fun readUInt32(): UInt   = readInt32().toUInt()
    fun readUInt64(): ULong  = readInt64().toULong()

    fun readFloat(): Float {
        if (read(longArray, 0, 4) != 4) throw OutOfRangeException()
        longBuffer.clear()
        return longBuffer.getFloat()
    }

    fun readDouble(): Double {
        if (read(longArray, 0, 8) != 8) throw OutOfRangeException()
        longBuffer.clear()
        return longBuffer.getDouble()
    }

    fun readNullTerminatedString(maxLength: Int = 32767): String {
        val b = mutableListOf<Byte>()

        while (true) {
            val v = read()
            if (v == -1) throw OutOfRangeException()

            val byte = v.toByte()
            if (byte == NULL) break
            b.add(byte)

            if (b.size > maxLength)
                throw InvalidValueException("unterminated string")
        }

        return String(b.toByteArray(), Charsets.UTF_8)
    }
}