package arc.backend.lwjgl3.vulkan.queue

import org.lwjgl.system.MemoryStack

class ComputeQueue(stack: MemoryStack, familyIndex: Int) : VulkanQueue(stack, familyIndex)