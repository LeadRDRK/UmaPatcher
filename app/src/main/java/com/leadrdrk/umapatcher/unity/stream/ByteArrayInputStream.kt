package com.leadrdrk.umapatcher.unity.stream

import java.io.IOException
import java.io.InputStream

/**
 * A specialized [InputStream] for reading the contents of a byte array.
 */
class ByteArrayInputStream : InputStream {
    /**
     * The `byte` array containing the bytes to stream over.
     */
    private var buf: ByteArray

    /**
     * The current position within the byte array.
     */
    var pos = 0

    /**
     * The current mark position. Initially set to 0 or the `offset`
     * parameter within the constructor.
     */
    private var mark: Int

    /**
     * The total number of bytes initially available in the byte array
     * `buf`.
     */
    var count: Int private set

    /**
     * Constructs a new `ByteArrayInputStream` on the byte array
     * `buf`.
     *
     * @param buf
     * the byte array to stream over.
     */
    constructor(buf: ByteArray) {
        mark = 0
        this.buf = buf
        count = buf.size
    }

    /**
     * Constructs a new `ByteArrayInputStream` on the byte array
     * `buf` with the initial position set to `offset` and the
     * number of bytes available set to `offset` + `length`.
     *
     * @param buf
     * the byte array to stream over.
     * @param offset
     * the initial position in `buf` to start streaming from.
     * @param length
     * the number of bytes available for streaming.
     */
    @Suppress("unused")
    constructor(buf: ByteArray, offset: Int, length: Int) {
        // BEGIN android-note
        // changed array notation to be consistent with the rest of harmony
        // END android-note
        this.buf = buf
        pos = offset
        mark = offset
        count = if (offset + length > buf.size) buf.size else offset + length
    }

    /**
     * Returns the number of remaining bytes.
     *
     * @return `count - pos`
     */
    @Synchronized
    override fun available(): Int {
        return count - pos
    }

    /**
     * Closes this stream and frees resources associated with this stream.
     *
     * @throws IOException
     * if an I/O error occurs while closing this stream.
     */
    @Throws(IOException::class)
    override fun close() {
        // Do nothing on close, this matches JDK behavior.
    }

    /**
     * Sets a mark position in this ByteArrayInputStream. The parameter
     * `readlimit` is ignored. Sending `reset()` will reposition the
     * stream back to the marked position.
     *
     * @param readlimit
     * ignored.
     * @see .markSupported
     * @see .reset
     */
    @Synchronized
    override fun mark(readlimit: Int) {
        mark = pos
    }

    /**
     * Indicates whether this stream supports the `mark()` and
     * `reset()` methods. Returns `true` since this class supports
     * these methods.
     *
     * @return always `true`.
     * @see .mark
     * @see .reset
     */
    override fun markSupported(): Boolean {
        return true
    }

    /**
     * Reads a single byte from the source byte array and returns it as an
     * integer in the range from 0 to 255. Returns -1 if the end of the source
     * array has been reached.
     *
     * @return the byte read or -1 if the end of this stream has been reached.
     */
    @Synchronized
    override fun read(): Int {
        return if (pos < count) buf[pos++].toInt() and 0xFF else -1
    }

    private fun checkOffsetAndCount(arrayLength: Int, offset: Int, count: Int) {
        if (offset or count < 0 || offset > arrayLength || arrayLength - offset < count) {
            throw ArrayIndexOutOfBoundsException()
        }
    }

    /**
     * Reads at most `len` bytes from this stream and stores
     * them in byte array `b` starting at `offset`. This
     * implementation reads bytes from the source byte array.
     *
     * @param buffer
     * the byte array in which to store the bytes read.
     * @param offset
     * the initial position in `b` to store the bytes read from
     * this stream.
     * @param length
     * the maximum number of bytes to store in `b`.
     * @return the number of bytes actually read or -1 if no bytes were read and
     * the end of the stream was encountered.
     * @throws IndexOutOfBoundsException
     * if `offset < 0` or `length < 0`, or if
     * `offset + length` is greater than the size of
     * `b`.
     * @throws NullPointerException
     * if `b` is `null`.
     */
    @Synchronized
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        checkOffsetAndCount(buffer.size, offset, length)
        // Are there any bytes available?
        if (pos >= count) {
            return -1
        }
        if (length == 0) {
            return 0
        }
        val copylen = if (count - pos < length) count - pos else length
        System.arraycopy(buf, pos, buffer, offset, copylen)
        pos += copylen
        return copylen
    }

    /**
     * Resets this stream to the last marked location. This implementation
     * resets the position to either the marked position, the start position
     * supplied in the constructor or 0 if neither has been provided.
     *
     * @see .mark
     */
    @Synchronized
    override fun reset() {
        pos = mark
    }

    /**
     * Skips `byteCount` bytes in this InputStream. Subsequent
     * calls to `read` will not return these bytes unless `reset` is
     * used. This implementation skips `byteCount` number of bytes in the
     * target stream. It does nothing and returns 0 if `byteCount` is negative.
     *
     * @return the number of bytes actually skipped.
     */
    @Synchronized
    override fun skip(byteCount: Long): Long {
        if (byteCount <= 0) {
            return 0
        }
        val temp = pos
        pos = if (count - pos < byteCount) count else (pos + byteCount).toInt()
        return (pos - temp).toLong()
    }
}