package com.leadrdrk.umapatcher.unity.stream

import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

open class BinaryWriter(
    protected val stream: OutputStream
): OutputStream() {
    private val longArray = ByteArray(8)
    private val longBuffer = ByteBuffer.wrap(longArray)
    var position: Long = 0
        private set

    var order: ByteOrder
        get() = longBuffer.order()
        set(v) { longBuffer.order(v) }

    override fun close() {
        stream.close()
    }

    override fun flush() {
        stream.flush()
    }

    override fun write(b: Int) {
        stream.write(b)
        ++position
    }

    override fun write(b: ByteArray) {
        stream.write(b)
        position += b.size
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        stream.write(b, off, len)
        position += len
    }

    fun writeInt8(b: Byte) = write(b.toInt())
    fun writeUInt8(b: UByte) = write(b.toInt())
    fun writeBoolean(b: Boolean) = write(if (b) 1 else 0)

    fun writeInt16(b: Short) {
        longBuffer.putShort(b)
        write(longArray, 0, 2)
        longBuffer.clear()
    }

    fun writeInt32(b: Int) {
        longBuffer.putInt(b)
        write(longArray, 0, 4)
        longBuffer.clear()
    }

    fun writeInt64(b: Long) {
        longBuffer.putLong(b)
        write(longArray, 0, 8)
        longBuffer.clear()
    }

    fun writeUInt16(b: UShort) = writeInt16(b.toShort())
    fun writeUInt32(b: UInt)   = writeInt32(b.toInt())
    fun writeUInt64(b: ULong)  = writeInt64(b.toLong())

    fun writeFloat(b: Float) {
        longBuffer.putFloat(b)
        write(longArray, 0, 4)
        longBuffer.clear()
    }

    fun writeDouble(b: Double) {
        longBuffer.putDouble(b)
        write(longArray, 0, 8)
        longBuffer.clear()
    }

    fun writeNullTerminatedString(str: String) {
        write(str.toByteArray(Charsets.UTF_8))
        write(0)
    }

    fun align(alignment: Int = 4) {
        val size = (alignment - position % alignment) % alignment
        write(ByteArray(size.toInt()))
    }

    fun writeAlignedString(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        writeInt32(bytes.size)
        write(bytes)
        align()
    }
}