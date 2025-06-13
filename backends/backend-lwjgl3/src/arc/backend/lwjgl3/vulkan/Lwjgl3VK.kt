package arc.backend.lwjgl3.vulkan

import arc.graphics.VK
import org.lwjgl.system.MemoryStack.stackGet
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkInstance
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.nio.LongBuffer

class Lwjgl3VK(val vulkanInstance: VkInstance, val vulkanDevice: VkDevice): VK {
    override fun vkGetDeviceQueue(queueFamilyIndex: Int, queueIndex: Int, pQueue: LongBuffer) {
        VK10.vkGetDeviceQueue(vulkanDevice, queueFamilyIndex, queueIndex, stackGet().mallocPointer(pQueue.limit()).put(pQueue))
    }

    override fun vkCreateSwapchainKHR(
        pCreateInfo: ByteBuffer?,
        pAllocator: ByteBuffer?,
        pSwapchain: LongBuffer?
    ): Long {
        TODO("Not yet implemented")
    }

    override fun vkDestroySwapchainKHR(swapchain: Long, pAllocator: ByteBuffer?) {
        TODO("Not yet implemented")
    }

    override fun vkAcquireNextImageKHR(
        swapchain: Long,
        timeout: Long,
        semaphore: Long,
        fence: Long,
        pImageIndex: IntBuffer?
    ): Int {
        TODO("Not yet implemented")
    }

    override fun vkQueuePresentKHR(queue: Long, pPresentInfo: ByteBuffer?): Int {
        TODO("Not yet implemented")
    }

    override fun vkCreateImage(
        pCreateInfo: ByteBuffer?,
        pAllocator: ByteBuffer?,
        pImage: LongBuffer?
    ): Long {
        TODO("Not yet implemented")
    }

    override fun vkDestroyImage(image: Long, pAllocator: ByteBuffer?) {
        TODO("Not yet implemented")
    }

    override fun vkCreateBuffer(
        pCreateInfo: ByteBuffer?,
        pAllocator: ByteBuffer?,
        pBuffer: LongBuffer?
    ): Long {
        TODO("Not yet implemented")
    }

    override fun vkDestroyBuffer(buffer: Long, pAllocator: ByteBuffer?) {
        TODO("Not yet implemented")
    }

    override fun vkAllocateMemory(
        pAllocateInfo: ByteBuffer?,
        pAllocator: ByteBuffer?,
        pMemory: LongBuffer?
    ): Long {
        TODO("Not yet implemented")
    }

    override fun vkFreeMemory(memory: Long, pAllocator: ByteBuffer?) {
        TODO("Not yet implemented")
    }

    override fun vkBindImageMemory(image: Long, memory: Long, memoryOffset: Long) {
        TODO("Not yet implemented")
    }

    override fun vkBindBufferMemory(buffer: Long, memory: Long, memoryOffset: Long) {
        TODO("Not yet implemented")
    }

    override fun vkCreateImageView(
        pCreateInfo: ByteBuffer?,
        pAllocator: ByteBuffer?,
        pView: LongBuffer?
    ): Long {
        TODO("Not yet implemented")
    }

    override fun vkDestroyImageView(imageView: Long, pAllocator: ByteBuffer?) {
        TODO("Not yet implemented")
    }

    override fun vkCreateShaderModule(
        pCreateInfo: ByteBuffer?,
        pAllocator: ByteBuffer?,
        pShaderModule: LongBuffer?
    ): Long {
        TODO("Not yet implemented")
    }

    override fun vkDestroyShaderModule(shaderModule: Long, pAllocator: ByteBuffer?) {
        TODO("Not yet implemented")
    }

    override fun vkCreatePipelineLayout(
        pCreateInfo: ByteBuffer?,
        pAllocator: ByteBuffer?,
        pPipelineLayout: LongBuffer?
    ): Long {
        TODO("Not yet implemented")
    }

    override fun vkDestroyPipelineLayout(pipelineLayout: Long, pAllocator: ByteBuffer?) {
        TODO("Not yet implemented")
    }

    override fun vkCreateGraphicsPipelines(
        pipelineCache: Long,
        createInfoCount: Int,
        pCreateInfos: ByteBuffer?,
        pAllocator: ByteBuffer?,
        pPipelines: LongBuffer?
    ): Long {
        TODO("Not yet implemented")
    }

    override fun vkDestroyPipeline(pipeline: Long, pAllocator: ByteBuffer?) {
        TODO("Not yet implemented")
    }

    override fun vkCreateRenderPass(
        pCreateInfo: ByteBuffer?,
        pAllocator: ByteBuffer?,
        pRenderPass: LongBuffer?
    ): Long {
        TODO("Not yet implemented")
    }

    override fun vkDestroyRenderPass(renderPass: Long, pAllocator: ByteBuffer?) {
        TODO("Not yet implemented")
    }

    override fun vkCreateFramebuffer(
        pCreateInfo: ByteBuffer?,
        pAllocator: ByteBuffer?,
        pFramebuffer: LongBuffer?
    ): Long {
        TODO("Not yet implemented")
    }

    override fun vkDestroyFramebuffer(framebuffer: Long, pAllocator: ByteBuffer?) {
        TODO("Not yet implemented")
    }

    override fun vkCreateDescriptorSetLayout(
        pCreateInfo: ByteBuffer?,
        pAllocator: ByteBuffer?,
        pSetLayout: LongBuffer?
    ): Long {
        TODO("Not yet implemented")
    }

    override fun vkDestroyDescriptorSetLayout(descriptorSetLayout: Long, pAllocator: ByteBuffer?) {
        TODO("Not yet implemented")
    }

    override fun vkCreateDescriptorPool(
        pCreateInfo: ByteBuffer?,
        pAllocator: ByteBuffer?,
        pDescriptorPool: LongBuffer?
    ): Long {
        TODO("Not yet implemented")
    }

    override fun vkDestroyDescriptorPool(descriptorPool: Long, pAllocator: ByteBuffer?) {
        TODO("Not yet implemented")
    }

    override fun vkAllocateDescriptorSets(pAllocateInfo: ByteBuffer?, pDescriptorSets: LongBuffer?) {
        TODO("Not yet implemented")
    }

    override fun vkUpdateDescriptorSets(
        descriptorWriteCount: Int,
        pDescriptorWrites: ByteBuffer?,
        descriptorCopyCount: Int,
        pDescriptorCopies: ByteBuffer?
    ) {
        TODO("Not yet implemented")
    }

    override fun vkCreateCommandPool(
        pCreateInfo: ByteBuffer?,
        pAllocator: ByteBuffer?,
        pCommandPool: LongBuffer?
    ): Long {
        TODO("Not yet implemented")
    }

    override fun vkDestroyCommandPool(commandPool: Long, pAllocator: ByteBuffer?) {
        TODO("Not yet implemented")
    }

    override fun vkAllocateCommandBuffers(pAllocateInfo: ByteBuffer?, pCommandBuffers: LongBuffer?) {
        TODO("Not yet implemented")
    }

    override fun vkFreeCommandBuffers(
        commandPool: Long,
        commandBufferCount: Int,
        pCommandBuffers: LongBuffer?
    ) {
        TODO("Not yet implemented")
    }

    override fun vkBeginCommandBuffer(commandBuffer: Long, pBeginInfo: ByteBuffer?) {
        TODO("Not yet implemented")
    }

    override fun vkEndCommandBuffer(commandBuffer: Long) {
        TODO("Not yet implemented")
    }

    override fun vkCmdBeginRenderPass(commandBuffer: Long, pRenderPassBegin: ByteBuffer?, contents: Int) {
        TODO("Not yet implemented")
    }

    override fun vkCmdEndRenderPass(commandBuffer: Long) {
        TODO("Not yet implemented")
    }

    override fun vkCmdBindPipeline(commandBuffer: Long, pipelineBindPoint: Int, pipeline: Long) {
        TODO("Not yet implemented")
    }

    override fun vkCmdBindVertexBuffers(
        commandBuffer: Long,
        firstBinding: Int,
        bindingCount: Int,
        pBuffers: LongBuffer?,
        pOffsets: LongBuffer?
    ) {
        TODO("Not yet implemented")
    }

    override fun vkCmdBindIndexBuffer(commandBuffer: Long, buffer: Long, offset: Long, indexType: Int) {
        TODO("Not yet implemented")
    }

    override fun vkCmdBindDescriptorSets(
        commandBuffer: Long,
        pipelineBindPoint: Int,
        pipelineLayout: Long,
        firstSet: Int,
        descriptorSetCount: Int,
        pDescriptorSets: LongBuffer?,
        dynamicOffsetCount: Int,
        pDynamicOffsets: IntBuffer?
    ) {
        TODO("Not yet implemented")
    }

    override fun vkCmdDraw(
        commandBuffer: Long,
        vertexCount: Int,
        instanceCount: Int,
        firstVertex: Int,
        firstInstance: Int
    ) {
        TODO("Not yet implemented")
    }

    override fun vkCmdDrawIndexed(
        commandBuffer: Long,
        indexCount: Int,
        instanceCount: Int,
        firstIndex: Int,
        vertexOffset: Int,
        firstInstance: Int
    ) {
        TODO("Not yet implemented")
    }

    override fun vkCmdCopyBuffer(
        commandBuffer: Long,
        srcBuffer: Long,
        dstBuffer: Long,
        regionCount: Int,
        pRegions: ByteBuffer?
    ) {
        TODO("Not yet implemented")
    }

    override fun vkCmdPipelineBarrier(
        commandBuffer: Long,
        srcStageMask: Int,
        dstStageMask: Int,
        dependencyFlags: Int,
        memoryBarrierCount: Int,
        pMemoryBarriers: ByteBuffer?,
        bufferMemoryBarrierCount: Int,
        pBufferMemoryBarriers: ByteBuffer?,
        imageMemoryBarrierCount: Int,
        pImageMemoryBarriers: ByteBuffer?
    ) {
        TODO("Not yet implemented")
    }

    override fun vkCreateSemaphore(
        pCreateInfo: ByteBuffer?,
        pAllocator: ByteBuffer?,
        pSemaphore: LongBuffer?
    ): Long {
        TODO("Not yet implemented")
    }

    override fun vkDestroySemaphore(semaphore: Long, pAllocator: ByteBuffer?) {
        TODO("Not yet implemented")
    }

    override fun vkCreateFence(
        pCreateInfo: ByteBuffer?,
        pAllocator: ByteBuffer?,
        pFence: LongBuffer?
    ): Long {
        TODO("Not yet implemented")
    }

    override fun vkDestroyFence(fence: Long, pAllocator: ByteBuffer?) {
        TODO("Not yet implemented")
    }

    override fun vkWaitForFences(fenceCount: Int, pFences: LongBuffer?, waitAll: Boolean, timeout: Long) {
        TODO("Not yet implemented")
    }

    override fun vkResetFences(fenceCount: Int, pFences: LongBuffer?) {
        TODO("Not yet implemented")
    }

    override fun vkQueueSubmit(queue: Long, submitCount: Int, pSubmits: ByteBuffer?, fence: Long): Int {
        TODO("Not yet implemented")
    }

    override fun vkMapMemory(
        memory: Long,
        offset: Long,
        size: Long,
        flags: Int,
        ppData: ByteBuffer?
    ): Int {
        TODO("Not yet implemented")
    }

    override fun vkUnmapMemory(memory: Long) {
        TODO("Not yet implemented")
    }

    override fun vkFlushMappedMemoryRanges(memoryRangeCount: Int, pMemoryRanges: ByteBuffer?) {
        TODO("Not yet implemented")
    }

    override fun vkInvalidateMappedMemoryRanges(memoryRangeCount: Int, pMemoryRanges: ByteBuffer?) {
        TODO("Not yet implemented")
    }

    override fun vkGetBufferMemoryRequirements(buffer: Long, pMemoryRequirements: ByteBuffer?) {
        TODO("Not yet implemented")
    }

    override fun vkGetImageMemoryRequirements(image: Long, pMemoryRequirements: ByteBuffer?) {
        TODO("Not yet implemented")
    }

    override fun vkFindMemoryType(typeFilter: Int, properties: Int): Int {
        TODO("Not yet implemented")
    }

    override fun vkCreateSampler(
        pCreateInfo: ByteBuffer?,
        pAllocator: ByteBuffer?,
        pSampler: LongBuffer?
    ): Long {
        TODO("Not yet implemented")
    }

    override fun vkDestroySampler(sampler: Long, pAllocator: ByteBuffer?) {
        TODO("Not yet implemented")
    }
}