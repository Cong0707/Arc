package arc.backend.lwjgl3.vulkan.memory

import arc.backend.lwjgl3.vulkan.memory.buffer.Buffer
import org.lwjgl.vulkan.VkMemoryHeap
import org.lwjgl.vulkan.VkMemoryType
import java.nio.ByteBuffer

abstract class MemoryType internal constructor(
    val type: Type,
    val vkMemoryType: VkMemoryType,
    val vkMemoryHeap: VkMemoryHeap
) {
    abstract fun createBuffer(buffer: Buffer, size: Long)

    abstract fun copyToBuffer(buffer: Buffer, src: ByteBuffer?, size: Long, srcOffset: Long, dstOffset: Long)

    abstract fun copyFromBuffer(buffer: Buffer, bufferSize: Long, byteBuffer: ByteBuffer?)

    abstract fun mappable(): Boolean

    enum class Type {
        DEVICE_LOCAL,
        HOST_LOCAL
    }
}