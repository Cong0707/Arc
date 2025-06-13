package arc.backend.lwjgl3.vulkan.queue

import arc.backend.lwjgl3.vulkan.Synchronization
import arc.backend.lwjgl3.vulkan.queue.CommandPool.CommandBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10

class GraphicsQueue(stack: MemoryStack, familyIndex: Int) : VulkanQueue(stack, familyIndex) {
    fun startRecording() {
        currentCmdBuffer = beginCommands()
    }

    fun endRecordingAndSubmit() {
        val fence: Long = submitCommands(currentCmdBuffer!!)
        Synchronization.INSTANCE.addCommandBuffer(currentCmdBuffer!!)

        currentCmdBuffer = null
    }

    val commandBuffer: CommandBuffer
        get() {
            return if (currentCmdBuffer != null) {
                currentCmdBuffer!!
            } else {
                beginCommands()
            }
        }

    fun endIfNeeded(commandBuffer: CommandBuffer): Long {
        return if (currentCmdBuffer != null) {
            VK10.VK_NULL_HANDLE
        } else {
            submitCommands(commandBuffer)
        }
    }

    companion object {
        var INSTANCE: GraphicsQueue? = null

        private var currentCmdBuffer: CommandBuffer? = null
    }
}