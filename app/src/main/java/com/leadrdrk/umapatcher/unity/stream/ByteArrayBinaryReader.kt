package com.leadrdrk.umapatcher.unity.stream

class ByteArrayBinaryReader(
    private val _stream: ByteArrayInputStream
): SeekableBinaryReader(_stream) {
    override var position: Long
        get() = _stream.pos.toLong()
        set(v) { _stream.pos = v.toInt() }

    override val size: Long
        get() = _stream.count.toLong()
}