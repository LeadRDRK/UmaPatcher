package com.leadrdrk.umapatcher.unity.stream

import java.io.FileInputStream
import java.io.IOException

class FileBinaryReader(
    stream: FileInputStream
): SeekableBinaryReader(stream) {
    private val channel = stream.channel
    private var mark: Long = -1

    override var position: Long
        get() = channel.position()
        set(v) { channel.position(v) }

    override val size: Long
        get() = channel.size()

    override fun markSupported() = true

    @Synchronized
    override fun mark(readlimit: Int) {
        mark = try {
            channel.position()
        } catch (_: IOException) {
            -1
        }
    }

    @Synchronized
    @Throws(IOException::class)
    override fun reset() {
        if (mark == -1L) throw IOException("not marked")
        channel.position(mark)
    }
}