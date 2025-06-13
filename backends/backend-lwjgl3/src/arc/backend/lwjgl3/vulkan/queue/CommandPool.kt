package arc.backend.lwjgl3.vulkan.queue


import arc.backend.lwjgl3.vulkan.Vulkan
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import java.util.*

class CommandPool internal constructor(queueFamilyIndex: Int) {
    var id: Long = 0

    private val commandBuffers: MutableList<CommandBuffer> = mutableListOf()
    private val availableCmdBuffers: Queue<CommandBuffer> = ArrayDeque()

    init {
        this.createCommandPool(queueFamilyIndex)
    }

    fun createCommandPool(queueFamily: Int) {
        MemoryStack.stackPush().use { stack ->
            val poolInfo = VkCommandPoolCreateInfo.calloc(stack)
            poolInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
            poolInfo.queueFamilyIndex(queueFamily)
            poolInfo.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)

            val pCommandPool = stack.mallocLong(1)

            if (vkCreateCommandPool(Vulkan.getVkDevice(), poolInfo, null, pCommandPool) != VK_SUCCESS) {
                throw RuntimeException("Failed to create command pool")
            }
            this.id = pCommandPool[0]
        }
    }

    fun getCommandBuffer(stack: MemoryStack): CommandBuffer {
        if (availableCmdBuffers.isEmpty()) {
            allocateCommandBuffers(stack)
        }

        val commandBuffer = availableCmdBuffers.poll()
        return commandBuffer
    }

    private fun allocateCommandBuffers(stack: MemoryStack) {
        val size = 10

        val allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
        allocInfo.`sType$Default`()
        allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
        allocInfo.commandPool(id)
        allocInfo.commandBufferCount(size)

        val pCommandBuffer = stack.mallocPointer(size)
        vkAllocateCommandBuffers(Vulkan.getVkDevice(), allocInfo, pCommandBuffer)

        val fenceInfo = VkFenceCreateInfo.calloc(stack)
        fenceInfo.`sType$Default`()
        fenceInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT)

        val semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc(stack)
        semaphoreCreateInfo.`sType$Default`()

        for (i in 0..<size) {
            val pFence = stack.mallocLong(1)
            vkCreateFence(Vulkan.getVkDevice(), fenceInfo, null, pFence)

            val pSemaphore = stack.mallocLong(1)
            vkCreateSemaphore(Vulkan.getVkDevice(), semaphoreCreateInfo, null, pSemaphore)

            val vkCommandBuffer = VkCommandBuffer(pCommandBuffer[i], Vulkan.getVkDevice())
            val commandBuffer = CommandBuffer(this, vkCommandBuffer, pFence[0], pSemaphore[0])
            commandBuffers.add(commandBuffer)
            availableCmdBuffers.add(commandBuffer)
        }
    }

    fun addToAvailable(commandBuffer: CommandBuffer) {
        availableCmdBuffers.add(commandBuffer)
    }

    fun cleanUp() {
        for (commandBuffer in commandBuffers) {
            vkDestroyFence(Vulkan.getVkDevice(), commandBuffer.fence, null)
            vkDestroySemaphore(Vulkan.getVkDevice(), commandBuffer.semaphore, null)
        }
        vkResetCommandPool(Vulkan.getVkDevice(), id, VK_COMMAND_POOL_RESET_RELEASE_RESOURCES_BIT)
        vkDestroyCommandPool(Vulkan.getVkDevice(), id, null)
    }

    class CommandBuffer(
        val commandPool: CommandPool,
        val handle: VkCommandBuffer,
        val fence: Long,
        val semaphore: Long
    ) {
        var isSubmitted: Boolean = false
        var isRecording: Boolean = false

        fun begin(stack: MemoryStack?) {
            val beginInfo = VkCommandBufferBeginInfo.calloc(stack)
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
            beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)

            vkBeginCommandBuffer(this.handle, beginInfo)

            this.isRecording = true
        }

        fun submitCommands(stack: MemoryStack, queue: VkQueue?, useSemaphore: Boolean): Long {
            val fence = this.fence

            vkEndCommandBuffer(this.handle)

            vkResetFences(Vulkan.getVkDevice(), this.fence)

            val submitInfo = VkSubmitInfo.calloc(stack)
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
            submitInfo.pCommandBuffers(stack.pointers(this.handle))

            if (useSemaphore) {
                submitInfo.pSignalSemaphores(stack.longs(this.semaphore))
            }

            vkQueueSubmit(queue, submitInfo, fence)

            this.isRecording = false
            this.isSubmitted = true
            return fence
        }

        fun reset() {
            this.isSubmitted = false
            this.isRecording = false
            commandPool.addToAvailable(this)
        }
    }
}