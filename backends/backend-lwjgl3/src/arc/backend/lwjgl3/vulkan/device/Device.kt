package arc.backend.lwjgl3.vulkan.device

import arc.util.ArcRuntimeException
import org.lwjgl.glfw.GLFW
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import java.util.stream.Collectors

class Device(val physicalDevice: VkPhysicalDevice) {
    val properties: VkPhysicalDeviceProperties = VkPhysicalDeviceProperties.malloc()

    private val vendorId: Int
    val vendorIdString: String
    val deviceName: String
    val driverVersion: String
    val vkVersion: String

    val availableFeatures: VkPhysicalDeviceFeatures2
    val availableFeatures11: VkPhysicalDeviceVulkan11Features

    //    public final VkPhysicalDeviceVulkan13Features availableFeatures13;
    //    public final boolean vulkan13Support;
    var isDrawIndirectSupported: Boolean = false
        private set

    init {
        VK10.vkGetPhysicalDeviceProperties(physicalDevice, properties)

        this.vendorId = properties.vendorID()
        this.vendorIdString = decodeVendor(properties.vendorID())
        this.deviceName = properties.deviceNameString()
        this.driverVersion = decodeDvrVersion(properties.driverVersion(), properties.vendorID())
        this.vkVersion = decDefVersion(vkVer)

        this.availableFeatures = VkPhysicalDeviceFeatures2.calloc()
        availableFeatures.`sType$Default`()

        this.availableFeatures11 = VkPhysicalDeviceVulkan11Features.malloc()
        availableFeatures11.`sType$Default`()
        availableFeatures.pNext(this.availableFeatures11)

        //Vulkan 1.3
//        this.availableFeatures13 = VkPhysicalDeviceVulkan13Features.malloc();
//        this.availableFeatures13.sType$Default();
//        this.availableFeatures11.pNext(this.availableFeatures13.address());
//
//        this.vulkan13Support = this.device.getCapabilities().apiVersion == VK_API_VERSION_1_3;
        VK11.vkGetPhysicalDeviceFeatures2(this.physicalDevice, this.availableFeatures)

        if (availableFeatures.features()
                .multiDrawIndirect() && availableFeatures11.shaderDrawParameters()
        ) this.isDrawIndirectSupported = true
    }

    fun getUnsupportedExtensions(requiredExtensions: Set<String>): Set<String> {
        MemoryStack.stackPush().use { stack ->
            val extensionCount = stack.ints(0)
            VK10.vkEnumerateDeviceExtensionProperties(physicalDevice, null as String?, extensionCount, null)

            val availableExtensions = VkExtensionProperties.malloc(extensionCount[0], stack)

            VK10.vkEnumerateDeviceExtensionProperties(
                physicalDevice,
                null as String?,
                extensionCount,
                availableExtensions
            )

            val extensions = availableExtensions.stream()
                .map { obj: VkExtensionProperties -> obj.extensionNameString() }
                .collect(Collectors.toSet())

            val unsupportedExtensions: MutableSet<String> = HashSet(requiredExtensions)
            unsupportedExtensions.removeAll(extensions)
            return unsupportedExtensions
        }
    }

    val isAMD: Boolean
        // Added these to allow detecting GPU vendor, to allow handling vendor specific circumstances:
        get() = vendorId == 0x1022

    val isNvidia: Boolean
        get() = vendorId == 0x10DE

    val isIntel: Boolean
        get() = vendorId == 0x8086

    companion object {
        private fun decodeVendor(i: Int): String {
            return when (i) {
                (0x10DE) -> "Nvidia"
                (0x1022) -> "AMD"
                (0x8086) -> "Intel"
                else -> "undef"
            }
        }

        // Should Work with AMD: https://gpuopen.com/learn/decoding-radeon-vulkan-versions/
        fun decDefVersion(v: Int): String {
            return VK10.VK_VERSION_MAJOR(v).toString() + "." + VK10.VK_VERSION_MINOR(v) + "." + VK10.VK_VERSION_PATCH(v)
        }

        // 0x10DE = Nvidia: https://pcisig.com/membership/member-companies?combine=Nvidia
        // https://registry.khronos.org/vulkan/specs/1.3-extensions/man/html/VkPhysicalDeviceProperties.html
        // this should work with Nvidia + AMD but is not guaranteed to work with intel drivers in Windows and more obscure/Exotic Drivers/vendors
        private fun decodeDvrVersion(v: Int, i: Int): String {
            return when (i) {
                (0x10DE) -> decodeNvidia(v)
                (0x1022) -> decDefVersion(v)
                (0x8086) -> decIntelVersion(v)
                else -> decDefVersion(v)
            }
        }

        // Source: https://www.intel.com/content/www/us/en/support/articles/000005654/graphics.html
        // Won't Work with older Drivers (15.45 And.or older)
        // May not work as this uses Guess work+Assumptions
        private fun decIntelVersion(v: Int): String {
            return if (GLFW.glfwGetPlatform() == GLFW.GLFW_PLATFORM_WIN32) (v ushr 14).toString() + "." + (v and 0x3fff) else decDefVersion(v)
        }


        private fun decodeNvidia(v: Int): String {
            return (v ushr 22 and 0x3FF).toString() + "." + (v ushr 14 and 0xff) + "." + (v ushr 6 and 0xff) + "." + (v and 0xff)
        }

        val vkVer: Int
            get() {
                MemoryStack.stackPush().use { stack ->
                    val a = stack.mallocInt(1)
                    VK11.vkEnumerateInstanceVersion(a)
                    val vkVer1 = a[0]
                    if (VK10.VK_VERSION_MINOR(vkVer1) < 2) {
                        throw ArcRuntimeException("Vulkan 1.2 not supported: Only Has: %s".format(decDefVersion(vkVer1)))
                    }
                    return vkVer1
                }
            }
    }
}