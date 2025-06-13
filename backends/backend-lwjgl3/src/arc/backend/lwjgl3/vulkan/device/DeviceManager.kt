package arc.backend.lwjgl3.vulkan.device


import arc.backend.lwjgl3.vulkan.Vulkan
import arc.backend.lwjgl3.vulkan.VulkanUtil.asPointerBuffer
import arc.backend.lwjgl3.vulkan.VulkanUtil.checkResult
import arc.backend.lwjgl3.vulkan.queue.*
import arc.backend.lwjgl3.vulkan.queue.VulkanQueue.Companion.findQueueFamilies
import arc.util.Log
import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.GLFWVulkan
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import java.nio.IntBuffer
import java.util.stream.Collectors

object DeviceManager {
    var availableDevices: List<Device>? = null
    var suitableDevices: List<Device>? = null

    var physicalDevice: VkPhysicalDevice? = null
    var vkDevice: VkDevice? = null

    var device: Device? = null

    var deviceProperties: VkPhysicalDeviceProperties? = null
    var memoryProperties: VkPhysicalDeviceMemoryProperties? = null

    var surfaceProperties: SurfaceProperties? = null

    var graphicsQueue: GraphicsQueue? = null
    var presentQueue: PresentQueue? = null
    var transferQueue: TransferQueue? = null
    var computeQueue: ComputeQueue? = null

    fun init(instance: VkInstance?) {
        try {
            getSuitableDevices(instance)
            pickPhysicalDevice()
            createLogicalDevice()
        } catch (e: Exception) {
            Log.infoTag("Vulkan", availableDevicesInfo)
            throw RuntimeException(e)
        }
    }

    fun getAvailableDevices(instance: VkInstance?): List<Device> {
        MemoryStack.stackPush().use { stack ->
            val devices: MutableList<Device> = mutableListOf()
            val deviceCount = stack.ints(0)

            VK10.vkEnumeratePhysicalDevices(instance, deviceCount, null)

            if (deviceCount[0] == 0) {
                return listOf()
            }

            val ppPhysicalDevices = stack.mallocPointer(deviceCount[0])
            VK10.vkEnumeratePhysicalDevices(instance, deviceCount, ppPhysicalDevices)

            var currentDevice: VkPhysicalDevice

            for (i in 0..<ppPhysicalDevices.capacity()) {
                currentDevice = VkPhysicalDevice(ppPhysicalDevices[i], instance)

                val device = Device(currentDevice)
                devices.add(device)
            }
            return devices
        }
    }

    fun getSuitableDevices(instance: VkInstance?) {
        availableDevices = getAvailableDevices(instance)

        val devices: MutableList<Device> = mutableListOf()
        for (device in availableDevices!!) {
            if (isDeviceSuitable(device.physicalDevice)) {
                devices.add(device)
            }
        }

        suitableDevices = devices
    }

    fun pickPhysicalDevice() {
        MemoryStack.stackPush().use { stack ->
            device = autoPickDevice()

            physicalDevice = device!!.physicalDevice

            // Get device properties
            deviceProperties = device!!.properties

            memoryProperties = VkPhysicalDeviceMemoryProperties.malloc()
            VK10.vkGetPhysicalDeviceMemoryProperties(physicalDevice, memoryProperties)
            surfaceProperties = querySurfaceProperties(physicalDevice, stack)
        }
    }

    fun autoPickDevice(): Device? {
        val integratedGPUs = ArrayList<Device>()
        val otherDevices = ArrayList<Device>()

        var flag = false

        var currentDevice: Device? = null
        for (device in suitableDevices!!) {
            currentDevice = device

            val deviceType = device.properties.deviceType()
            if (deviceType == VK10.VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU) {
                flag = true
                break
            } else if (deviceType == VK10.VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU) integratedGPUs.add(device)
            else otherDevices.add(device)
        }

        if (!flag) {
            currentDevice = if (!integratedGPUs.isEmpty()) integratedGPUs[0]
            else if (!otherDevices.isEmpty()) otherDevices[0]
            else {
                throw IllegalStateException("Failed to find a suitable GPU")
            }
        }

        return currentDevice
    }

    fun createLogicalDevice() {
        MemoryStack.stackPush().use { stack ->
            val indices: VulkanQueue.QueueFamilyIndices = findQueueFamilies(physicalDevice)
            val uniqueQueueFamilies: IntArray = indices.unique()

            val queueCreateInfos = VkDeviceQueueCreateInfo.calloc(uniqueQueueFamilies.size, stack)

            for (i in uniqueQueueFamilies.indices) {
                val queueCreateInfo = queueCreateInfos[i]
                queueCreateInfo.sType(VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                queueCreateInfo.queueFamilyIndex(uniqueQueueFamilies[i])
                queueCreateInfo.pQueuePriorities(stack.floats(1.0f))
            }

            val deviceVulkan11Features = VkPhysicalDeviceVulkan11Features.calloc(stack)
            deviceVulkan11Features.`sType$Default`()
            deviceVulkan11Features.shaderDrawParameters(device!!.isDrawIndirectSupported)

            val deviceFeatures = VkPhysicalDeviceFeatures2.calloc(stack)
            deviceFeatures.`sType$Default`()
            deviceFeatures.features().samplerAnisotropy(device!!.availableFeatures.features().samplerAnisotropy())
            deviceFeatures.features().logicOp(device!!.availableFeatures.features().logicOp())
            // TODO: Disable indirect draw option if unsupported.
            deviceFeatures.features().multiDrawIndirect(device!!.isDrawIndirectSupported)

            // Must not set line width to anything other than 1.0 if this is not supported
            if (device!!.availableFeatures.features().wideLines()) {
                deviceFeatures.features().wideLines(true)
                Vulkan.canSetLineWidth = true
            }

            val createInfo = VkDeviceCreateInfo.calloc(stack)
            createInfo.`sType$Default`()
            createInfo.sType(VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
            createInfo.pQueueCreateInfos(queueCreateInfos)
            createInfo.pEnabledFeatures(deviceFeatures.features())
            createInfo.pNext(deviceVulkan11Features)

            if (Vulkan.DYNAMIC_RENDERING) {
                val dynamicRenderingFeaturesKHR = VkPhysicalDeviceDynamicRenderingFeaturesKHR.calloc(stack)
                dynamicRenderingFeaturesKHR.`sType$Default`()
                dynamicRenderingFeaturesKHR.dynamicRendering(true)

                deviceVulkan11Features.pNext(dynamicRenderingFeaturesKHR.address())

                //                //Vulkan 1.3 dynamic rendering
//                VkPhysicalDeviceVulkan13Features deviceVulkan13Features = VkPhysicalDeviceVulkan13Features.calloc(stack);
//                deviceVulkan13Features.sType$Default();
//                if(!deviceInfo.availableFeatures13.dynamicRendering())
//                    throw new RuntimeException("Device does not support dynamic rendering feature.");
//
//                deviceVulkan13Features.dynamicRendering(true);
//                createInfo.pNext(deviceVulkan13Features);
//                deviceVulkan13Features.pNext(deviceVulkan11Features.address());
            }

            createInfo.ppEnabledExtensionNames(asPointerBuffer(Vulkan.REQUIRED_EXTENSION))

            //            Configuration.DEBUG_FUNCTIONS.set(true);
            createInfo.ppEnabledLayerNames(if (Vulkan.ENABLE_VALIDATION_LAYERS) asPointerBuffer(Vulkan.VALIDATION_LAYERS!!) else null)

            val pDevice = stack.pointers(VK10.VK_NULL_HANDLE)

            val res = VK10.vkCreateDevice(physicalDevice, createInfo, null, pDevice)
            checkResult(res, "Failed to create logical device")

            vkDevice = VkDevice(pDevice[0], physicalDevice, createInfo, VK12.VK_API_VERSION_1_2)

            graphicsQueue = GraphicsQueue(stack, indices.graphicsFamily)
            transferQueue = TransferQueue(stack, indices.transferFamily)
            presentQueue = PresentQueue(stack, indices.presentFamily)
            computeQueue = ComputeQueue(stack, indices.computeFamily)
        }
    }

    private val requiredExtensions: PointerBuffer
        get() {
            val glfwExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions()

            if (Vulkan.ENABLE_VALIDATION_LAYERS) {
                val stack = MemoryStack.stackGet()

                val extensions = stack.mallocPointer(glfwExtensions.capacity() + 1)

                extensions.put(glfwExtensions)
                extensions.put(stack.UTF8(EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME))

                // Rewind the buffer before returning it to reset its position back to 0
                return extensions.rewind()
            }

            return glfwExtensions
        }

    private fun isDeviceSuitable(device: VkPhysicalDevice): Boolean {
        MemoryStack.stackPush().use { stack ->
            val indices: VulkanQueue.QueueFamilyIndices = findQueueFamilies(device)
            val availableExtensions = getAvailableExtension(stack, device)
            val extensionsSupported = availableExtensions.stream()
                .map<String> { obj: VkExtensionProperties -> obj.extensionNameString() }
                .collect(Collectors.toSet<String>())
                .containsAll(Vulkan.REQUIRED_EXTENSION)

            var swapChainAdequate = false

            if (extensionsSupported) {
                val surfaceProperties = querySurfaceProperties(device, stack)
                swapChainAdequate =
                    surfaceProperties.formats!!.hasRemaining() && surfaceProperties.presentModes!!.hasRemaining()
            }

            val supportedFeatures = VkPhysicalDeviceFeatures.malloc(stack)
            VK10.vkGetPhysicalDeviceFeatures(device, supportedFeatures)
            val anisotropicFilterSupported = supportedFeatures.samplerAnisotropy()
            return indices.isSuitable && extensionsSupported && swapChainAdequate
        }
    }

    private fun getAvailableExtension(stack: MemoryStack, device: VkPhysicalDevice): VkExtensionProperties.Buffer {
        val extensionCount = stack.ints(0)
        VK10.vkEnumerateDeviceExtensionProperties(device, null as String?, extensionCount, null)

        val availableExtensions = VkExtensionProperties.malloc(extensionCount[0], stack)
        VK10.vkEnumerateDeviceExtensionProperties(device, null as String?, extensionCount, availableExtensions)

        return availableExtensions
    }

    // Use the optimal most performant depth format for the specific GPU
    // Nvidia performs best with 24 bit depth, while AMD is most performant with 32-bit float
    fun findDepthFormat(use24BitsDepthFormat: Boolean): Int {
        val formats = if (use24BitsDepthFormat)
            intArrayOf(
                VK10.VK_FORMAT_D24_UNORM_S8_UINT,
                VK10.VK_FORMAT_X8_D24_UNORM_PACK32,
                VK10.VK_FORMAT_D32_SFLOAT,
                VK10.VK_FORMAT_D32_SFLOAT_S8_UINT
            )
        else
            intArrayOf(VK10.VK_FORMAT_D32_SFLOAT, VK10.VK_FORMAT_D32_SFLOAT_S8_UINT)

        return findSupportedFormat(
            VK10.VK_IMAGE_TILING_OPTIMAL,
            VK10.VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT,
            *formats
        )
    }

    private fun findSupportedFormat(tiling: Int, features: Int, vararg formatCandidates: Int): Int {
        MemoryStack.stackPush().use { stack ->
            val props = VkFormatProperties.calloc(stack)
            for (format in formatCandidates) {
                VK10.vkGetPhysicalDeviceFormatProperties(physicalDevice, format, props)

                if (tiling == VK10.VK_IMAGE_TILING_LINEAR && (props.linearTilingFeatures() and features) == features) {
                    return format
                } else if (tiling == VK10.VK_IMAGE_TILING_OPTIMAL && (props.optimalTilingFeatures() and features) == features) {
                    return format
                }
            }
        }
        throw RuntimeException("Failed to find supported format")
    }

    val availableDevicesInfo: String
        get() {
            val stringBuilder = StringBuilder()
            stringBuilder.append("\n")

            if (availableDevices == null) {
                stringBuilder.append("\tDevice Manager not initialized")
                return stringBuilder.toString()
            }

            if (availableDevices!!.isEmpty()) {
                stringBuilder.append("\tNo available device found")
            }

            for (device in availableDevices!!) {
                stringBuilder.append("\tDevice: %s\n".format(device.deviceName))

                stringBuilder.append("\t\tVulkan Version: %s\n".format(device.vkVersion))

                stringBuilder.append("\t\t")
                val unsupportedExtensions = device.getUnsupportedExtensions(Vulkan.REQUIRED_EXTENSION)
                if (unsupportedExtensions.isEmpty()) {
                    stringBuilder.append("All required extensions are supported\n")
                } else {
                    stringBuilder.append("Unsupported extension: %s\n".format(unsupportedExtensions))
                }
            }

            return stringBuilder.toString()
        }

    fun destroy() {
        graphicsQueue?.cleanUp()
        transferQueue?.cleanUp()
        computeQueue?.cleanUp()

        VK10.vkDestroyDevice(vkDevice, null)
    }

    fun querySurfaceProperties(device: VkPhysicalDevice?, stack: MemoryStack): SurfaceProperties {
        val surface: Long = Vulkan.getSurface()
        val details = SurfaceProperties()

        details.capabilities = VkSurfaceCapabilitiesKHR.malloc(stack)
        KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, surface, details.capabilities)

        val count = stack.ints(0)

        KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, count, null)

        if (count[0] != 0) {
            details.formats = VkSurfaceFormatKHR.malloc(count[0], stack)
            KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, count, details.formats)
        }

        KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, count, null)

        if (count[0] != 0) {
            details.presentModes = stack.mallocInt(count[0])
            KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, count, details.presentModes)
        }

        return details
    }

    class SurfaceProperties {
        var capabilities: VkSurfaceCapabilitiesKHR? = null
        var formats: VkSurfaceFormatKHR.Buffer? = null
        var presentModes: IntBuffer? = null
    }
}