package arc.backend.lwjgl3.vulkan.queue

import org.lwjgl.system.MemoryStack

class PresentQueue(stack: MemoryStack, familyIndex: Int) : VulkanQueue(stack, familyIndex, false)