package arc.backend.lwjgl3.vulkan.memory


import arc.backend.lwjgl3.vulkan.Vulkan
import arc.backend.lwjgl3.vulkan.VulkanUtil.translateVulkanResult
import arc.backend.lwjgl3.vulkan.device.DeviceManager
import arc.backend.lwjgl3.vulkan.memory.buffer.Buffer
import arc.backend.lwjgl3.vulkan.queue.VulkanQueue
import arc.struct.LongMap
import arc.util.ArcRuntimeException
import arc.util.Log
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.vma.Vma
import org.lwjgl.util.vma.VmaAllocationCreateInfo
import org.lwjgl.util.vma.VmaBudget
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkBufferCreateInfo
import org.lwjgl.vulkan.VkImageCreateInfo
import java.nio.LongBuffer
import java.util.function.Consumer

class MemoryManager internal constructor() {
    private var currentFrame = 0

    private val freeableBuffers: Array<MutableList<Buffer.BufferInfo>?> = arrayOfNulls(Frames)
    //private val freeableImages: Array<MutableList<VulkanImage>?> = arrayOfNulls(Frames)

    private val frameOps: Array<MutableList<Runnable>?> = arrayOfNulls(Frames)
    //private val segmentsToFree: Array<MutableList<Pair<AreaBuffer, Int>>?> = arrayOfNulls(Frames)

    //debug
    private var stackTraces: Array<MutableList<Array<StackTraceElement>>?>? = null

    init {
        for (i in 0..<Frames) {
            freeableBuffers[i] = mutableListOf()
            //freeableImages[i] = mutableListOf()

            frameOps[i] = mutableListOf()
            //segmentsToFree[i] = mutableListOf()
        }

        if (DEBUG) {
            this.stackTraces = arrayOfNulls(Frames)
            for (i in 0..<Frames) {
                stackTraces!![i] = mutableListOf()
            }
        }
    }

    @Synchronized
    fun initFrame(frame: Int) {
        this.setCurrentFrame(frame)
        this.freeBuffers(frame)
        //this.freeImages(frame)
        this.doFrameOps(frame)
        //this.freeSegments(frame)
    }

    fun setCurrentFrame(frame: Int) {
        if (frame < Frames) {
            throw ArcRuntimeException("Out of bounds frame index")
        }
        this.currentFrame = frame
    }

    fun freeAllBuffers() {
        for (frame in 0..<Frames) {
            this.freeBuffers(frame)
            //this.freeImages(frame)
            this.doFrameOps(frame)
        }

        //        buffers.values().forEach(buffer -> freeBuffer(buffer.getId(), buffer.getAllocation()));
//        images.values().forEach(image -> image.doFree(this));
    }

    fun createBuffer(size: Long, usage: Int, properties: Int, pBuffer: LongBuffer?, pBufferMemory: PointerBuffer?) {
        MemoryStack.stackPush().use { stack ->
            val bufferInfo = VkBufferCreateInfo.calloc(stack)
            bufferInfo.sType(VK10.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
            bufferInfo.size(size)
            bufferInfo.usage(usage)

            val allocationInfo = VmaAllocationCreateInfo.calloc(stack)
            allocationInfo.requiredFlags(properties)

            val result = Vma.vmaCreateBuffer(ALLOCATOR, bufferInfo, allocationInfo, pBuffer, pBufferMemory, null)
            if (result != VK10.VK_SUCCESS) {
                Log.info(
                    String.format(
                        "Failed to create buffer with size: %.3f MB",
                        (size.toFloat() / BYTES_IN_MB)
                    )
                )
                Log.info(
                    String.format(
                        "Tracked Device Memory used: %d/%d MB",
                        allocatedDeviceMemoryMB,
                        deviceMemoryMB
                    )
                )
                Log.info(heapStats)

                throw RuntimeException("Failed to create buffer: %s".format(translateVulkanResult(result)))
            }
        }
    }

    @Synchronized
    fun createBuffer(buffer: Buffer, size: Long, usage: Int, properties: Int) {
        MemoryStack.stackPush().use { stack ->
            val pBuffer = stack.mallocLong(1)
            val pAllocation = stack.pointers(VK10.VK_NULL_HANDLE)

            this.createBuffer(size, usage, properties, pBuffer, pAllocation)

            buffer.id = pBuffer[0]
            buffer.allocation = pAllocation[0]
            buffer.bufferSize = size

            if ((properties and VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT) != 0) {
                deviceMemory += size
            } else {
                nativeMemory += size
            }
            buffers.put(buffer.id, buffer)
        }
    }

    fun createImage(
        width: Int, height: Int, mipLevels: Int, format: Int, tiling: Int, usage: Int,
        memProperties: Int,
        pTextureImage: LongBuffer?, pTextureImageMemory: PointerBuffer?
    ) {
        MemoryStack.stackPush().use { stack ->
            val imageInfo = VkImageCreateInfo.calloc(stack)
            imageInfo.sType(VK10.VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
            imageInfo.imageType(VK10.VK_IMAGE_TYPE_2D)
            imageInfo.extent().width(width)
            imageInfo.extent().height(height)
            imageInfo.extent().depth(1)
            imageInfo.mipLevels(mipLevels)
            imageInfo.arrayLayers(1)
            imageInfo.format(format)
            imageInfo.tiling(tiling)
            imageInfo.initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED)
            imageInfo.usage(usage)
            imageInfo.samples(VK10.VK_SAMPLE_COUNT_1_BIT)
            //            imageInfo.sharingMode(VK_SHARING_MODE_CONCURRENT);
            imageInfo.pQueueFamilyIndices(
                stack.ints(VulkanQueue.queueFamilies!!.graphicsFamily, VulkanQueue.queueFamilies!!.computeFamily)
            )

            val allocationInfo = VmaAllocationCreateInfo.calloc(stack)
            allocationInfo.requiredFlags(memProperties)

            val result =
                Vma.vmaCreateImage(ALLOCATOR, imageInfo, allocationInfo, pTextureImage, pTextureImageMemory, null)
            if (result != VK10.VK_SUCCESS) {
                Log.info(String.format("Failed to create image with size: %dx%d", width, height))

                throw RuntimeException("Failed to create image: %s".format(translateVulkanResult(result)))
            }
        }
    }

    fun Map(allocation: Long): PointerBuffer {
        val data = MemoryUtil.memAllocPointer(1)

        Vma.vmaMapMemory(ALLOCATOR, allocation, data)

        return data
    }

    @Synchronized
    fun addToFreeable(buffer: Buffer) {
        val bufferInfo: Buffer.BufferInfo = buffer.bufferInfo

        checkBuffer(bufferInfo)

        freeableBuffers[currentFrame]?.add(bufferInfo)

        if (DEBUG) stackTraces?.get(currentFrame)?.add(Throwable().stackTrace)
    }

/*    @Synchronized
    fun addToFreeable(image: VulkanImage?) {
        freeableImages[currentFrame].add(image)
    }*/

    @Synchronized
    fun addFrameOp(runnable: Runnable) {
        frameOps[currentFrame]?.add(runnable)
    }

    fun doFrameOps(frame: Int) {
        for (runnable in frameOps[frame]!!) {
            runnable.run()
        }

        frameOps[frame]?.clear()
    }

    private fun freeBuffers(frame: Int) {
        val bufferList: MutableList<Buffer.BufferInfo>? = freeableBuffers[frame]
        if (bufferList != null) {
            for (bufferInfo in bufferList) {
                freeBuffer(bufferInfo)
            }
        }

        bufferList!!.clear()

        if (DEBUG) stackTraces?.get(frame)?.clear()
    }

    /*private fun freeImages(frame: Int) {
        val bufferList: MutableList<VulkanImage>? = freeableImages[frame]
        for (image in bufferList) {
            image.doFree()
        }

        bufferList!!.clear()
    }*/

    private fun checkBuffer(bufferInfo: Buffer.BufferInfo) {
        if (buffers.get(bufferInfo.id) == null) {
            throw RuntimeException("trying to free not present buffer")
        }
    }

    /*private fun freeSegments(frame: Int) {
        val list: MutableList<Pair<AreaBuffer, Int>> = segmentsToFree[frame]
        for (pair in list) {
            pair.first.setSegmentFree(pair.second)
        }

        list.clear()
    }*/

    /*fun addToFreeSegment(areaBuffer: AreaBuffer?, offset: Int) {
        segmentsToFree[currentFrame].add(Pair(areaBuffer, offset))
    }*/

    val nativeMemoryMB: Int
        get() = bytesInMb(nativeMemory)

    val allocatedDeviceMemoryMB: Int
        get() = bytesInMb(deviceMemory)

    val deviceMemoryMB: Int
        get() = bytesInMb(MemoryTypes.GPU_MEM!!.vkMemoryHeap.size())

    fun bytesInMb(bytes: Long): Int {
        return (bytes / BYTES_IN_MB).toInt()
    }

    val heapStats: String
        get() {
            MemoryStack.stackPush().use { stack ->
                val vmaBudgets = VmaBudget.calloc(DeviceManager.memoryProperties!!.memoryHeapCount(), stack)
                Vma.vmaGetHeapBudgets(ALLOCATOR, vmaBudgets)

                val vmaBudget: VmaBudget = vmaBudgets.get(MemoryTypes.GPU_MEM!!.vkMemoryType.heapIndex())
                val usage = vmaBudget.usage()
                val budget = vmaBudget.budget()
                return String.format("Device Memory Heap Usage: %d/%dMB", bytesInMb(usage), bytesInMb(budget))
            }
        }

    companion object {
        private const val DEBUG = false
        const val BYTES_IN_MB: Long = (1024 * 1024).toLong()

        var instance: MemoryManager? = null
            private set
        private val ALLOCATOR: Long = Vulkan.allocator

        private val buffers: LongMap<Buffer> = LongMap()
        //private val images: Long2ReferenceOpenHashMap<VulkanImage> = Long2ReferenceOpenHashMap()

        var Frames: Int = 0

        private var deviceMemory: Long = 0
        private var nativeMemory: Long = 0

        fun createInstance(frames: Int) {
            Frames = frames

            instance = MemoryManager()
        }

        /*fun addImage(image: VulkanImage) {
            images.putIfAbsent(image.getId(), image)

            deviceMemory += image.size
        }*/

        fun MapAndCopy(allocation: Long, consumer: Consumer<PointerBuffer?>) {
            MemoryStack.stackPush().use { stack ->
                val data = stack.mallocPointer(1)
                Vma.vmaMapMemory(ALLOCATOR, allocation, data)
                consumer.accept(data)
                Vma.vmaUnmapMemory(ALLOCATOR, allocation)
            }
        }

        fun freeBuffer(buffer: Long, allocation: Long) {
            Vma.vmaDestroyBuffer(ALLOCATOR, buffer, allocation)

            buffers.remove(buffer)
        }

        private fun freeBuffer(bufferInfo: Buffer.BufferInfo) {
            Vma.vmaDestroyBuffer(ALLOCATOR, bufferInfo.id, bufferInfo.allocation)

            if (bufferInfo.type === MemoryType.Type.DEVICE_LOCAL) {
                deviceMemory -= bufferInfo.bufferSize
            } else {
                nativeMemory -= bufferInfo.bufferSize
            }

            buffers.remove(bufferInfo.id)
        }

        /*fun freeImage(imageId: Long, allocation: Long) {
            Vma.vmaDestroyImage(ALLOCATOR, imageId, allocation)

            val image: VulkanImage = images.remove(imageId)
            deviceMemory -= image.size
        }*/
    }
}