package arc.backend.lwjgl3.vulkan.memory.buffer


import arc.backend.lwjgl3.vulkan.Synchronization
import arc.backend.lwjgl3.vulkan.memory.MemoryTypes
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.libc.LibCString
import org.lwjgl.vulkan.VK10
import java.nio.ByteBuffer

class StagingBuffer @JvmOverloads constructor(size: Long = DEFAULT_SIZE) :
    Buffer(VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT, MemoryTypes.HOST_MEM!!) {
    init {
        this.createBuffer(size)
    }

    fun copyBuffer(size: Int, byteBuffer: ByteBuffer?) {
        this.copyBuffer(size, MemoryUtil.memAddress(byteBuffer))
    }

    fun copyBuffer(size: Int, scrPtr: Long) {
        require(size <= this.bufferSize) { "Upload size is greater than staging buffer size." }

        if (size > this.bufferSize - this.usedBytes) {
            submitUploads()
        }

        LibCString.nmemcpy(this.dataPtr + this.usedBytes, scrPtr, size.toLong())

        this.offset = this.usedBytes
        this.usedBytes += size.toLong()
    }

    private fun align(l: Long, alignment: Int): Long {
        if (alignment == 0) return l

        val r = l % alignment
        return if (r != 0L) l + alignment - r else l
    }

    fun align(alignment: Int) {
        var alignedOffset: Long = align(usedBytes, alignment)

        if (alignedOffset > this.bufferSize) {
            submitUploads()
            alignedOffset = 0
        }

        this.usedBytes = alignedOffset
    }

    private fun submitUploads() {
        // Submit and wait all recorded uploads before resetting the buffer
        //UploadManager.INSTANCE.submitUploads()
        //ImageUploadHelper.INSTANCE.submitCommands()
        Synchronization.INSTANCE.waitFences()

        this.reset()
    }

    companion object {
        private const val DEFAULT_SIZE = (64 * 1024 * 1024).toLong()
    }
}