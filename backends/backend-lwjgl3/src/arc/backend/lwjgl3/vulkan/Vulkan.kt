package arc.backend.lwjgl3.vulkan

import arc.backend.lwjgl3.vulkan.VulkanUtil.asPointerBuffer
import arc.backend.lwjgl3.vulkan.VulkanUtil.checkResult
import arc.backend.lwjgl3.vulkan.device.Device
import arc.backend.lwjgl3.vulkan.device.DeviceManager
import arc.backend.lwjgl3.vulkan.device.DeviceManager.findDepthFormat
import arc.backend.lwjgl3.vulkan.memory.MemoryTypes
import arc.backend.lwjgl3.vulkan.queue.VulkanQueue
import arc.util.ArcRuntimeException
import arc.util.Log
import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface
import org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions
import org.lwjgl.system.MemoryStack.stackGet
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.util.vma.Vma.vmaCreateAllocator
import org.lwjgl.util.vma.VmaAllocatorCreateInfo
import org.lwjgl.util.vma.VmaVulkanFunctions
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.EXTDebugUtils.*
import org.lwjgl.vulkan.KHRDynamicRendering.VK_KHR_DYNAMIC_RENDERING_EXTENSION_NAME
import org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VK12.VK_API_VERSION_1_2
import java.nio.LongBuffer
import java.util.stream.Collectors.toSet


class Vulkan(val window: Long) {
    var vulkanInstance: VkInstance

    init {
        if (ENABLE_VALIDATION_LAYERS) {
            VALIDATION_LAYERS = HashSet()
            VALIDATION_LAYERS!!.add("VK_LAYER_KHRONOS_validation")
//            VALIDATION_LAYERS.add("VK_LAYER_KHRONOS_synchronization2");
        } else {
            // We are not going to use it, so we don't create it
            VALIDATION_LAYERS = null
        }
        vulkanInstance = glfwGetRequiredInstanceExtensions()?.let { createInstance() } ?: throw ArcRuntimeException("Failed to find list of required Vulkan extensions")


        createSurface();
        setupDebugMessenger();
        DeviceManager.init(vulkanInstance);

        createVma();
        MemoryTypes.createMemoryTypes();

        createCommandPool();

        setupDepthFormat();
    }

    private fun createInstance(): VkInstance {
        if (ENABLE_VALIDATION_LAYERS && !checkValidationLayerSupport()) {
            throw RuntimeException("Validation requested but not supported");
        }

        stackPush().use { stack ->

            // Use calloc to initialize the structs with 0s. Otherwise, the program can crash due to random values
            val appInfo = VkApplicationInfo.calloc(stack)

            appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
            appInfo.pApplicationName(stack.UTF8Safe("Arc"))
            appInfo.applicationVersion(VK_MAKE_VERSION(1, 0, 0))
            appInfo.pEngineName(stack.UTF8Safe("Arc"))
            appInfo.engineVersion(VK_MAKE_VERSION(1, 0, 0))
            appInfo.apiVersion(VK_API_VERSION_1_0)

            val createInfo = VkInstanceCreateInfo.calloc(stack)

            createInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
            createInfo.pApplicationInfo(appInfo)
            createInfo.ppEnabledExtensionNames(getRequiredInstanceExtensions())

            if (ENABLE_VALIDATION_LAYERS) {
                createInfo.ppEnabledLayerNames(asPointerBuffer(VALIDATION_LAYERS!!))
            }

            // We need to retrieve the pointer of the created instance
            val instancePtr = stack.mallocPointer(1)

            val result = vkCreateInstance(createInfo, null, instancePtr)
            checkResult(result, "Failed to create instance")
            return VkInstance(instancePtr[0], createInfo)
        }
    }

    private fun getRequiredInstanceExtensions(): PointerBuffer {
        val glfwExtensions = glfwGetRequiredInstanceExtensions()

        if (ENABLE_VALIDATION_LAYERS) {
            val stack = stackGet()

            val extensions = stack.mallocPointer(glfwExtensions.capacity() + 1)

            extensions.put(glfwExtensions)
            extensions.put(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME))

            // Rewind the buffer before returning it to reset its position back to 0
            return extensions.rewind()
        }

        return glfwExtensions
    }

    fun checkValidationLayerSupport(): Boolean {
        stackPush().use { stack ->
            val layerCount = stack.ints(0)
            vkEnumerateInstanceLayerProperties(layerCount, null)

            val availableLayers = VkLayerProperties.malloc(layerCount[0], stack)

            vkEnumerateInstanceLayerProperties(layerCount, availableLayers)

            val availableLayerNames = availableLayers.stream()
                .map { obj: VkLayerProperties -> obj.layerNameString() }
                .collect(toSet())
            return availableLayerNames.containsAll(VALIDATION_LAYERS!!)
        }
    }

    private fun populateDebugMessengerCreateInfo(debugCreateInfo: VkDebugUtilsMessengerCreateInfoEXT) {
        debugCreateInfo.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
        //        debugCreateInfo.messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT);
        debugCreateInfo.messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT or VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT)
        debugCreateInfo.messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT or VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT or VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT)
        //        debugCreateInfo.messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT);
        debugCreateInfo.pfnUserCallback(Vulkan::debugCallback)
    }

    private fun setupDebugMessenger() {
        if (!ENABLE_VALIDATION_LAYERS) {
            return
        }

        stackPush().use { stack ->
            val createInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack)
            populateDebugMessengerCreateInfo(createInfo)

            val pDebugMessenger = stack.longs(VK_NULL_HANDLE)

            checkResult(
                createDebugUtilsMessengerEXT(vulkanInstance, createInfo, null, pDebugMessenger),
                "Failed to set up debug messenger"
            )
            debugMessenger = pDebugMessenger[0]
        }
    }

    private fun createDebugUtilsMessengerEXT(
        instance: VkInstance, createInfo: VkDebugUtilsMessengerCreateInfoEXT,
        allocationCallbacks: VkAllocationCallbacks?, pDebugMessenger: LongBuffer
    ): Int {
        if (vkGetInstanceProcAddr(instance, "vkCreateDebugUtilsMessengerEXT") !== NULL) {
            return vkCreateDebugUtilsMessengerEXT(instance, createInfo, allocationCallbacks, pDebugMessenger)
        }

        return VK_ERROR_EXTENSION_NOT_PRESENT
    }

    private fun createSurface() {
        stackPush().use { stack ->
            val pSurface = stack.longs(VK_NULL_HANDLE)
            checkResult(
                glfwCreateWindowSurface(vulkanInstance, window, null, pSurface),
                "Failed to create window surface"
            )
            surface = pSurface[0]
        }
    }

    private fun createVma() {
        stackPush().use { stack ->
            val vulkanFunctions = VmaVulkanFunctions.calloc(stack)
            vulkanFunctions[vulkanInstance] = DeviceManager.vkDevice

            val allocatorCreateInfo = VmaAllocatorCreateInfo.calloc(stack)
            allocatorCreateInfo.physicalDevice(DeviceManager.physicalDevice)
            allocatorCreateInfo.device(DeviceManager.vkDevice)
            allocatorCreateInfo.pVulkanFunctions(vulkanFunctions)
            allocatorCreateInfo.instance(vulkanInstance)
            allocatorCreateInfo.vulkanApiVersion(VK_API_VERSION_1_2)

            val pAllocator = stack.pointers(VK_NULL_HANDLE)

            checkResult(
                vmaCreateAllocator(allocatorCreateInfo, pAllocator),
                "Failed to create Allocator"
            )
            allocator = pAllocator[0]
        }
    }

    private fun createCommandPool() {
        stackPush().use { stack ->
            val queueFamilyIndices: VulkanQueue.QueueFamilyIndices = VulkanQueue.queueFamilies
            val poolInfo = VkCommandPoolCreateInfo.calloc(stack)
            poolInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
            poolInfo.queueFamilyIndex(queueFamilyIndices.graphicsFamily)
            poolInfo.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)

            val pCommandPool = stack.mallocLong(1)

            checkResult(
                vkCreateCommandPool(DeviceManager.vkDevice, poolInfo, null, pCommandPool),
                "Failed to create command pool"
            )
            commandPool = pCommandPool[0]
        }
    }

    fun setupDepthFormat() {
        DEFAULT_DEPTH_FORMAT = findDepthFormat(use24BitsDepthFormat)
    }

    companion object {
        val ENABLE_VALIDATION_LAYERS: Boolean = false
        var VALIDATION_LAYERS: HashSet<String>? = null
        const val DYNAMIC_RENDERING: Boolean = false
        val REQUIRED_EXTENSION: Set<String> = getRequiredExtensionSet()
        var DEFAULT_DEPTH_FORMAT: Int = 0

        var canSetLineWidth: Boolean = false
        var use24BitsDepthFormat: Boolean = true

        private var surface: Long = 0
        var allocator: Long = 0
        var commandPool: Long = 0
        private var debugMessenger: Long = 0

        private fun getRequiredExtensionSet(): Set<String> {
            val extensions: ArrayList<String> = ArrayList(listOf(VK_KHR_SWAPCHAIN_EXTENSION_NAME))

            if (DYNAMIC_RENDERING) {
                extensions.add(VK_KHR_DYNAMIC_RENDERING_EXTENSION_NAME)
            }

            return HashSet(extensions)
        }

        private fun debugCallback(messageSeverity: Int, messageType: Int, pCallbackData: Long, pUserData: Long): Int {
            val callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData)
            Log.errTag("Vulkan", callbackData.pMessageString())
            if ((messageSeverity and VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0) System.nanoTime()
            return VK_FALSE
        }

        fun getDevice(): Device {
            return DeviceManager.device!!
        }

        fun getVkDevice(): VkDevice {
            return DeviceManager.vkDevice!!
        }

        fun getSurface(): Long {
            return surface
        }
    }
}