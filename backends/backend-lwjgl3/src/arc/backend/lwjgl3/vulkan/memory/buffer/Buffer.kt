package arc.backend.lwjgl3.vulkan.memory.buffer

import arc.backend.lwjgl3.vulkan.memory.MemoryManager
import arc.backend.lwjgl3.vulkan.memory.MemoryType
import java.nio.ByteBuffer

open class Buffer(val usage: Int, type: MemoryType) {
    val type: MemoryType = type

    var id: Long = 0
    var allocation: Long = 0

    var bufferSize: Long = 0
    var usedBytes: Long = 0
        protected set
    var offset: Long = 0
        protected set

    var dataPtr: Long = 0
        protected set

    fun createBuffer(bufferSize: Long) {
        type.createBuffer(this, bufferSize)

        if (type.mappable()) {
            this.dataPtr = MemoryManager.instance!!.Map(this.allocation).get(0)
        }
    }

    fun resizeBuffer(newSize: Long) {
        MemoryManager.instance!!.addToFreeable(this)
        this.createBuffer(newSize)
    }

    fun copyBuffer(byteBuffer: ByteBuffer?, size: Int) {
        if (size > this.bufferSize - this.usedBytes) {
            resizeBuffer((this.bufferSize + size) * 2)
        }

        type.copyToBuffer(this, byteBuffer, size.toLong(), 0, this.usedBytes)
        this.offset = this.usedBytes
        this.usedBytes += size.toLong()
    }

    fun copyBuffer(byteBuffer: ByteBuffer?, size: Int, dstOffset: Int) {
        if (size > this.bufferSize - dstOffset) {
            resizeBuffer((this.bufferSize + size) * 2)
        }

        type.copyToBuffer(this, byteBuffer, size.toLong(), 0, dstOffset.toLong())
        this.offset = dstOffset.toLong()
        this.usedBytes = (dstOffset + size).toLong()
    }

    fun scheduleFree() {
        MemoryManager.instance!!.addToFreeable(this)
    }

    fun reset() {
        usedBytes = 0
    }

    val bufferInfo: BufferInfo
        get() = BufferInfo(
            this.id, this.allocation, this.bufferSize,
            type.type
        )

    class BufferInfo(val id: Long, val allocation: Long, val bufferSize: Long, type: MemoryType.Type) {
        val type: MemoryType.Type = type
    }
}