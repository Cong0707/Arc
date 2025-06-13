package arc.backend.lwjgl3.vulkan.memory


import arc.backend.lwjgl3.vulkan.Vulkan
import arc.backend.lwjgl3.vulkan.device.DeviceManager
import arc.backend.lwjgl3.vulkan.device.DeviceManager.transferQueue
import arc.backend.lwjgl3.vulkan.memory.buffer.Buffer
import arc.backend.lwjgl3.vulkan.memory.buffer.StagingBuffer
import org.lwjgl.system.Checks.CHECKS
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkMemoryHeap
import org.lwjgl.vulkan.VkMemoryType
import java.nio.ByteBuffer


object MemoryTypes {
    var GPU_MEM: MemoryType? = null
    var HOST_MEM: MemoryType? = null

    fun createMemoryTypes() {
        for (i in 0..<DeviceManager.memoryProperties!!.memoryTypeCount()) {
            val memoryType = DeviceManager.memoryProperties!!.memoryTypes(i)
            val heap = DeviceManager.memoryProperties!!.memoryHeaps(memoryType.heapIndex())
            val propertyFlags = memoryType.propertyFlags()

            if (propertyFlags == VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT) {
                GPU_MEM = DeviceMappableMemory(memoryType, heap)
            }

            if (propertyFlags == (VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)) {
                HOST_MEM = HostCoherentMemory(memoryType, heap)
            }
        }

        if (GPU_MEM != null && HOST_MEM != null) return

        // Could not find 1 or more MemoryTypes, need to use fallback
        for (i in 0..<DeviceManager.memoryProperties!!.memoryTypeCount()) {
            val memoryType = DeviceManager.memoryProperties!!.memoryTypes(i)
            val heap = DeviceManager.memoryProperties!!.memoryHeaps(memoryType.heapIndex())

            // GPU mappable memory
            if ((memoryType.propertyFlags() and (VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT or VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT)) == (VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT or VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT)) {
                GPU_MEM = DeviceMappableMemory(memoryType, heap)
            }

            if ((memoryType.propertyFlags() and (VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)) == (VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)) {
                HOST_MEM = HostLocalFallbackMemory(memoryType, heap)
            }

            if (GPU_MEM != null && HOST_MEM != null) return
        }

        // Could not find device memory, fallback to host memory
        GPU_MEM = HOST_MEM
    }

    private fun memcpy(src: Buffer, dst: ByteBuffer, size: Long) {
        if (CHECKS) {
            require(size <= dst.remaining()) { "Upload size is greater than available dst buffer size" }
        }

        val srcPtr: Long = src.dataPtr
        val dstPtr = MemoryUtil.memAddress(dst)

        MemoryUtil.memCopy(srcPtr, dstPtr, size)
    }

    private fun memcpy(src: ByteBuffer?, dst: Buffer, size: Long, srcOffset: Long, dstOffset: Long) {
        if (CHECKS) {
            require(!(size > dst.bufferSize - dstOffset)) { "Upload size is greater than available dst buffer size" }
        }

        val dstPtr: Long = dst.dataPtr + dstOffset
        val srcPtr = MemoryUtil.memAddress(src) + srcOffset
        MemoryUtil.memCopy(srcPtr, dstPtr, size)
    }

    /*class DeviceLocalMemory internal constructor(vkMemoryType: VkMemoryType, vkMemoryHeap: VkMemoryHeap) :
        MemoryType(Type.DEVICE_LOCAL, vkMemoryType, vkMemoryHeap) {
        override fun createBuffer(buffer: Buffer, size: Long) {
            MemoryManager.instance!!.createBuffer(
                buffer, size,
                VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT or VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT or buffer.usage,
                VK10.VK_MEMORY_HEAP_DEVICE_LOCAL_BIT
            )
        }

        override fun copyToBuffer(buffer: Buffer, src: ByteBuffer?, size: Long, srcOffset: Long, dstOffset: Long) {
            val stagingBuffer: StagingBuffer = Vulkan.getStagingBuffer()
            stagingBuffer.copyBuffer(size.toInt(), src)

            transferQueue!!.copyBufferCmd(
                stagingBuffer.id,
                stagingBuffer.offset,
                buffer.id,
                dstOffset,
                size
            )
        }

        override fun copyFromBuffer(buffer: Buffer, bufferSize: Long, byteBuffer: ByteBuffer?) {
            // TODO
        }

        fun copyBuffer(src: Buffer, dst: Buffer): Long {
            require(!(dst.bufferSize < src.bufferSize)) { "dst size is less than src size." }

            return transferQueue!!.copyBufferCmd(src.id, 0, dst.id, 0, src.bufferSize)
        }

        override fun mappable(): Boolean {
            return false
        }
    }*/

    internal abstract class MappableMemory(type: Type, vkMemoryType: VkMemoryType, vkMemoryHeap: VkMemoryHeap) :
        MemoryType(type, vkMemoryType, vkMemoryHeap) {
        override fun copyToBuffer(buffer: Buffer, src: ByteBuffer?, size: Long, srcOffset: Long, dstOffset: Long) {
            memcpy(src, buffer, size, srcOffset, dstOffset)
        }

        override fun copyFromBuffer(buffer: Buffer, size: Long, byteBuffer: ByteBuffer?) {
            MemoryUtil.memCopy(buffer.dataPtr, MemoryUtil.memAddress(byteBuffer), size)
            memcpy(buffer, byteBuffer!!, size)
        }

        override fun mappable(): Boolean {
            return true
        }
    }

    internal class HostCoherentMemory(vkMemoryType: VkMemoryType, vkMemoryHeap: VkMemoryHeap) :
        MappableMemory(Type.HOST_LOCAL, vkMemoryType, vkMemoryHeap) {
        override fun copyToBuffer(buffer: Buffer, src: ByteBuffer?, size: Long, srcOffset: Long, dstOffset: Long) {
            //TODO
        }

        override fun createBuffer(buffer: Buffer, size: Long) {
            MemoryManager.instance!!.createBuffer(
                buffer, size,
                VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT or VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT or buffer.usage,
                VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
            )
        }
    }

    internal class HostLocalFallbackMemory(vkMemoryType: VkMemoryType, vkMemoryHeap: VkMemoryHeap) :
        MappableMemory(Type.HOST_LOCAL, vkMemoryType, vkMemoryHeap) {
        override fun copyToBuffer(buffer: Buffer, src: ByteBuffer?, size: Long, srcOffset: Long, dstOffset: Long) {
            //TODO
        }

        override fun createBuffer(buffer: Buffer, size: Long) {
            MemoryManager.instance!!.createBuffer(
                buffer, size,
                VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT or VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT or buffer.usage,
                VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
            )
        }
    }

    internal class DeviceMappableMemory(vkMemoryType: VkMemoryType, vkMemoryHeap: VkMemoryHeap) :
        MappableMemory(Type.DEVICE_LOCAL, vkMemoryType, vkMemoryHeap) {
        override fun copyToBuffer(buffer: Buffer, src: ByteBuffer?, size: Long, srcOffset: Long, dstOffset: Long) {
            //TODO
        }

        override fun createBuffer(buffer: Buffer, size: Long) {
            MemoryManager.instance!!.createBuffer(
                buffer, size,
                VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT or VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT or buffer.usage,
                VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT or VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
            )
        }
    }
}