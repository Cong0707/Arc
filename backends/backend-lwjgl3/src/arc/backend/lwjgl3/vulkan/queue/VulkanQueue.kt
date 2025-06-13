package arc.backend.lwjgl3.vulkan.queue


import arc.backend.lwjgl3.vulkan.Vulkan
import arc.backend.lwjgl3.vulkan.device.DeviceManager
import arc.util.Log
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR
import java.util.stream.IntStream

abstract class VulkanQueue @JvmOverloads internal constructor(
    stack: MemoryStack,
    familyIndex: Int,
    initCommandPool: Boolean = true
) {
    private val queue: VkQueue

    private var commandPool: CommandPool? = null

    @Synchronized
    fun beginCommands(): CommandPool.CommandBuffer {
        MemoryStack.stackPush().use { stack ->
            val commandBuffer: CommandPool.CommandBuffer = commandPool!!.getCommandBuffer(stack)
            commandBuffer.begin(stack)
            return commandBuffer
        }
    }

    init {
        val pQueue = stack.mallocPointer(1)
        VK10.vkGetDeviceQueue(DeviceManager.vkDevice, familyIndex, 0, pQueue)
        this.queue = VkQueue(pQueue[0], DeviceManager.vkDevice)

        if (initCommandPool) this.commandPool = CommandPool(familyIndex)
    }

    @Synchronized
    fun submitCommands(commandBuffer: CommandPool.CommandBuffer): Long {
        MemoryStack.stackPush().use { stack ->
            return commandBuffer.submitCommands(stack, queue, false)
        }
    }

    fun queue(): VkQueue {
        return this.queue
    }

    fun cleanUp() {
        if (commandPool != null) commandPool!!.cleanUp()
    }

    fun waitIdle() {
        VK10.vkQueueWaitIdle(queue)
    }

    fun getCommandPool(): CommandPool? {
        return commandPool
    }

    enum class Family {
        Graphics,
        Transfer,
        Compute
    }

    class QueueFamilyIndices {
        var graphicsFamily: Int = VK10.VK_QUEUE_FAMILY_IGNORED
        var presentFamily: Int = VK10.VK_QUEUE_FAMILY_IGNORED
        var transferFamily: Int = VK10.VK_QUEUE_FAMILY_IGNORED
        var computeFamily: Int = VK10.VK_QUEUE_FAMILY_IGNORED

        val isComplete: Boolean
            get() = graphicsFamily != -1 && presentFamily != -1 && transferFamily != -1 && computeFamily != -1

        val isSuitable: Boolean
            get() = graphicsFamily != -1 && presentFamily != -1

        fun unique(): IntArray {
            return IntStream.of(graphicsFamily, presentFamily, transferFamily, computeFamily).distinct().toArray()
        }

        fun array(): IntArray {
            return intArrayOf(graphicsFamily, presentFamily)
        }
    }

    companion object {
        private var device: VkDevice? = null
        private var queueFamilyIndices: QueueFamilyIndices? = null

        val queueFamilies: QueueFamilyIndices
            get() {
                if (device == null) device = Vulkan.getVkDevice()

                if (queueFamilyIndices == null) {
                    queueFamilyIndices = findQueueFamilies(device!!.physicalDevice)
                }
                return queueFamilyIndices!!
            }

        fun findQueueFamilies(device: VkPhysicalDevice?): QueueFamilyIndices {
            val indices = QueueFamilyIndices()

            MemoryStack.stackPush().use { stack ->
                val queueFamilyCount = stack.ints(0)
                VK10.vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, null)

                val queueFamilies = VkQueueFamilyProperties.mallocStack(queueFamilyCount[0], stack)

                VK10.vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, queueFamilies)

                val presentSupport = stack.ints(VK10.VK_FALSE)

                for (i in 0..<queueFamilies.capacity()) {
                    val queueFlags = queueFamilies[i].queueFlags()

                    if ((queueFlags and VK10.VK_QUEUE_GRAPHICS_BIT) != 0) {
                        indices.graphicsFamily = i

                        vkGetPhysicalDeviceSurfaceSupportKHR(device, i, Vulkan.getSurface(), presentSupport)

                        if (presentSupport[0] == VK10.VK_TRUE) {
                            indices.presentFamily = i
                        }
                    } else if ((queueFlags and (VK10.VK_QUEUE_GRAPHICS_BIT)) == 0
                        && (queueFlags and VK10.VK_QUEUE_COMPUTE_BIT) != 0
                    ) {
                        indices.computeFamily = i
                    } else if ((queueFlags and (VK10.VK_QUEUE_COMPUTE_BIT or VK10.VK_QUEUE_GRAPHICS_BIT)) == 0
                        && (queueFlags and VK10.VK_QUEUE_TRANSFER_BIT) != 0
                    ) {
                        indices.transferFamily = i
                    }

                    if (indices.presentFamily == -1) {
                        vkGetPhysicalDeviceSurfaceSupportKHR(device, i, Vulkan.getSurface(), presentSupport)

                        if (presentSupport[0] == VK10.VK_TRUE) {
                            indices.presentFamily = i
                        }
                    }

                    if (indices.isComplete) break
                }

                if (indices.presentFamily == -1) {
                    // Some drivers will not show present support even if some queue supports it
                    // Use compute queue as fallback

                    indices.presentFamily = indices.computeFamily
                    Log.warn("Using compute queue as present fallback")
                }

                // In case there's no dedicated transfer queue, we need choose another one
                // preferably a different one from the already selected queues
                if (indices.transferFamily == -1) {
                    var transferIndex = -1
                    for (i in 0..<queueFamilies.capacity()) {
                        val queueFlags = queueFamilies[i].queueFlags()

                        if ((queueFlags and VK10.VK_QUEUE_TRANSFER_BIT) != 0) {
                            if (transferIndex == -1) transferIndex = i

                            if ((queueFlags and (VK10.VK_QUEUE_GRAPHICS_BIT)) == 0) {
                                indices.transferFamily = i

                                if (i != indices.computeFamily) break

                                transferIndex = i
                            }
                        }
                    }

                    if (transferIndex == -1) throw RuntimeException("Failed to find queue family with transfer support")

                    indices.transferFamily = transferIndex
                }

                if (indices.computeFamily == -1) {
                    for (i in 0..<queueFamilies.capacity()) {
                        val queueFlags = queueFamilies[i].queueFlags()

                        if ((queueFlags and VK10.VK_QUEUE_COMPUTE_BIT) != 0) {
                            indices.computeFamily = i
                            break
                        }
                    }
                }

                if (indices.graphicsFamily == VK10.VK_QUEUE_FAMILY_IGNORED) throw RuntimeException("Unable to find queue family with graphics support.")
                if (indices.presentFamily == VK10.VK_QUEUE_FAMILY_IGNORED) throw RuntimeException("Unable to find queue family with present support.")
                if (indices.computeFamily == VK10.VK_QUEUE_FAMILY_IGNORED) throw RuntimeException("Unable to find queue family with compute support.")
                return indices
            }
        }
    }
}