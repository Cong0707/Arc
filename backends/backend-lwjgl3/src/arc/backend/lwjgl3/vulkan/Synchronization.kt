package arc.backend.lwjgl3.vulkan


import arc.backend.lwjgl3.vulkan.queue.CommandPool.CommandBuffer
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10
import java.nio.LongBuffer

class Synchronization internal constructor(allocSize: Int) {
    private val fences: LongBuffer = MemoryUtil.memAllocLong(allocSize)
    private var idx = 0

    private val commandBuffers: MutableList<CommandBuffer> = mutableListOf()

    @Synchronized
    fun addCommandBuffer(commandBuffer: CommandBuffer) {
        this.addFence(commandBuffer.fence)
        commandBuffers.add(commandBuffer)
    }

    @Synchronized
    fun addFence(fence: Long) {
        if (idx == ALLOCATION_SIZE) waitFences()

        fences.put(idx, fence)
        idx++
    }

    @Synchronized
    fun waitFences() {
        if (idx == 0) return

        val device = Vulkan.getVkDevice()

        fences.limit(idx)

        VK10.vkWaitForFences(device, fences, true, Long.MAX_VALUE)

        commandBuffers.forEach { it.reset() }
        commandBuffers.clear()

        fences.limit(ALLOCATION_SIZE)
        idx = 0
    }

    companion object {
        private const val ALLOCATION_SIZE = 50

        val INSTANCE: Synchronization = Synchronization(ALLOCATION_SIZE)

        fun waitFence(fence: Long) {
            val device = Vulkan.getVkDevice()

            VK10.vkWaitForFences(device, fence, true, Long.MAX_VALUE)
        }

        fun checkFenceStatus(fence: Long): Boolean {
            val device = Vulkan.getVkDevice()
            return VK10.vkGetFenceStatus(device, fence) == VK10.VK_SUCCESS
        }
    }
}