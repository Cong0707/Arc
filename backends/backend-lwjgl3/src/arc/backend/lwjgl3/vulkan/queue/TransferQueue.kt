package arc.backend.lwjgl3.vulkan.queue

import arc.backend.lwjgl3.vulkan.Synchronization
import arc.backend.lwjgl3.vulkan.Vulkan
import arc.backend.lwjgl3.vulkan.queue.CommandPool.CommandBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK10.vkWaitForFences
import org.lwjgl.vulkan.VkBufferCopy
import org.lwjgl.vulkan.VkCommandBuffer

class TransferQueue(stack: MemoryStack, familyIndex: Int) : VulkanQueue(stack, familyIndex) {
    fun copyBufferCmd(srcBuffer: Long, srcOffset: Long, dstBuffer: Long, dstOffset: Long, size: Long): Long {
        MemoryStack.stackPush().use { stack ->
            val commandBuffer: CommandBuffer = beginCommands()
            val copyRegion = VkBufferCopy.calloc(1, stack)
            copyRegion.size(size)
            copyRegion.srcOffset(srcOffset)
            copyRegion.dstOffset(dstOffset)

            VK10.vkCmdCopyBuffer(commandBuffer.handle, srcBuffer, dstBuffer, copyRegion)

            this.submitCommands(commandBuffer)
            Synchronization.INSTANCE.addCommandBuffer(commandBuffer)
            return commandBuffer.fence
        }
    }

    fun uploadBufferImmediate(srcBuffer: Long, srcOffset: Long, dstBuffer: Long, dstOffset: Long, size: Long) {
        MemoryStack.stackPush().use { stack ->
            val commandBuffer: CommandBuffer = this.beginCommands()
            val copyRegion = VkBufferCopy.calloc(1, stack)
            copyRegion.size(size)
            copyRegion.srcOffset(srcOffset)
            copyRegion.dstOffset(dstOffset)

            VK10.vkCmdCopyBuffer(commandBuffer.handle, srcBuffer, dstBuffer, copyRegion)

            this.submitCommands(commandBuffer)
            vkWaitForFences(DEVICE, commandBuffer.fence, true, Long.MAX_VALUE)
            commandBuffer.reset()
        }
    }

    companion object {
        private val DEVICE = Vulkan.getVkDevice()

        fun uploadBufferCmd(
            commandBuffer: VkCommandBuffer,
            srcBuffer: Long,
            srcOffset: Long,
            dstBuffer: Long,
            dstOffset: Long,
            size: Long
        ) {
            MemoryStack.stackPush().use { stack ->
                val copyRegion = VkBufferCopy.calloc(1, stack)
                copyRegion.size(size)
                copyRegion.srcOffset(srcOffset)
                copyRegion.dstOffset(dstOffset)
                VK10.vkCmdCopyBuffer(commandBuffer, srcBuffer, dstBuffer, copyRegion)
            }
        }
    }
}