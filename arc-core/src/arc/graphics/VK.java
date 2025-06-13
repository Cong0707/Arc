package arc.graphics;

import java.nio.*;

/**
 * Interface wrapping core Vulkan functionality
 */
public interface VK {
    // Vulkan 版本常量
    int VK_API_VERSION_1_0 = 0x00400000;
    int VK_API_VERSION_1_1 = 0x00401000;
    int VK_API_VERSION_1_2 = 0x00402000;
    int VK_API_VERSION_1_3 = 0x00403000;

    // 结果代码
    int VK_SUCCESS = 0;
    int VK_NOT_READY = 1;
    int VK_TIMEOUT = 2;
    int VK_ERROR_OUT_OF_HOST_MEMORY = -1;
    int VK_ERROR_OUT_OF_DEVICE_MEMORY = -2;
    // ... 其他错误代码

    // 格式常量
    int VK_FORMAT_R8G8B8A8_UNORM = 37;
    int VK_FORMAT_B8G8R8A8_UNORM = 44;
    int VK_FORMAT_R32G32B32A32_SFLOAT = 109;
    // ... 其他格式

    // 内存属性
    int VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT = 0x00000001;
    int VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT = 0x00000002;
    int VK_MEMORY_PROPERTY_HOST_COHERENT_BIT = 0x00000004;

    // 使用标志
    int VK_BUFFER_USAGE_VERTEX_BUFFER_BIT = 0x00000001;
    int VK_BUFFER_USAGE_INDEX_BUFFER_BIT = 0x00000002;
    int VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT = 0x00000010;
    int VK_IMAGE_USAGE_SAMPLED_BIT = 0x00000001;
    int VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT = 0x00000010;

    // 着色器阶段
    int VK_SHADER_STAGE_VERTEX_BIT = 0x00000001;
    int VK_SHADER_STAGE_FRAGMENT_BIT = 0x00000010;
    int VK_SHADER_STAGE_COMPUTE_BIT = 0x00000020;

    // 管线绑定点
    int VK_PIPELINE_BIND_POINT_GRAPHICS = 0;
    int VK_PIPELINE_BIND_POINT_COMPUTE = 1;

    // 顶点输入率
    int VK_VERTEX_INPUT_RATE_VERTEX = 0;
    int VK_VERTEX_INPUT_RATE_INSTANCE = 1;

    // 比较操作
    int VK_COMPARE_OP_NEVER = 0;
    int VK_COMPARE_OP_LESS = 1;
    int VK_COMPARE_OP_EQUAL = 2;
    int VK_COMPARE_OP_LESS_OR_EQUAL = 3;
    int VK_COMPARE_OP_GREATER = 4;
    int VK_COMPARE_OP_NOT_EQUAL = 5;
    int VK_COMPARE_OP_GREATER_OR_EQUAL = 6;
    int VK_COMPARE_OP_ALWAYS = 7;

    // 剔除模式
    int VK_CULL_MODE_NONE = 0;
    int VK_CULL_MODE_FRONT_BIT = 0x00000001;
    int VK_CULL_MODE_BACK_BIT = 0x00000002;
    int VK_CULL_MODE_FRONT_AND_BACK = 0x00000003;

    // 多边形模式
    int VK_POLYGON_MODE_FILL = 0;
    int VK_POLYGON_MODE_LINE = 1;
    int VK_POLYGON_MODE_POINT = 2;

    // 图元拓扑
    int VK_PRIMITIVE_TOPOLOGY_POINT_LIST = 0;
    int VK_PRIMITIVE_TOPOLOGY_LINE_LIST = 1;
    int VK_PRIMITIVE_TOPOLOGY_LINE_STRIP = 2;
    int VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST = 3;
    int VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP = 4;
    int VK_PRIMITIVE_TOPOLOGY_TRIANGLE_FAN = 5;

    // ========== 实例和设备管理 ==========
    void vkGetDeviceQueue(int queueFamilyIndex, int queueIndex, LongBuffer pQueue);

    // ========== 交换链 ==========
    long vkCreateSwapchainKHR(ByteBuffer pCreateInfo, ByteBuffer pAllocator, LongBuffer pSwapchain);
    void vkDestroySwapchainKHR(long swapchain, ByteBuffer pAllocator);
    int vkAcquireNextImageKHR(long swapchain, long timeout, long semaphore, long fence, IntBuffer pImageIndex);
    int vkQueuePresentKHR(long queue, ByteBuffer pPresentInfo);

    // ========== 资源创建 ==========
    long vkCreateImage(ByteBuffer pCreateInfo, ByteBuffer pAllocator, LongBuffer pImage);
    void vkDestroyImage(long image, ByteBuffer pAllocator);
    long vkCreateBuffer(ByteBuffer pCreateInfo, ByteBuffer pAllocator, LongBuffer pBuffer);
    void vkDestroyBuffer(long buffer, ByteBuffer pAllocator);
    long vkAllocateMemory(ByteBuffer pAllocateInfo, ByteBuffer pAllocator, LongBuffer pMemory);
    void vkFreeMemory(long memory, ByteBuffer pAllocator);
    void vkBindImageMemory(long image, long memory, long memoryOffset);
    void vkBindBufferMemory(long buffer, long memory, long memoryOffset);
    long vkCreateImageView(ByteBuffer pCreateInfo, ByteBuffer pAllocator, LongBuffer pView);
    void vkDestroyImageView(long imageView, ByteBuffer pAllocator);

    // ========== 渲染管线 ==========
    long vkCreateShaderModule(ByteBuffer pCreateInfo, ByteBuffer pAllocator, LongBuffer pShaderModule);
    void vkDestroyShaderModule(long shaderModule, ByteBuffer pAllocator);
    long vkCreatePipelineLayout(ByteBuffer pCreateInfo, ByteBuffer pAllocator, LongBuffer pPipelineLayout);
    void vkDestroyPipelineLayout(long pipelineLayout, ByteBuffer pAllocator);
    long vkCreateGraphicsPipelines(long pipelineCache, int createInfoCount, ByteBuffer pCreateInfos, ByteBuffer pAllocator, LongBuffer pPipelines);
    void vkDestroyPipeline(long pipeline, ByteBuffer pAllocator);
    long vkCreateRenderPass(ByteBuffer pCreateInfo, ByteBuffer pAllocator, LongBuffer pRenderPass);
    void vkDestroyRenderPass(long renderPass, ByteBuffer pAllocator);
    long vkCreateFramebuffer(ByteBuffer pCreateInfo, ByteBuffer pAllocator, LongBuffer pFramebuffer);
    void vkDestroyFramebuffer(long framebuffer, ByteBuffer pAllocator);

    // ========== 描述符 ==========
    long vkCreateDescriptorSetLayout(ByteBuffer pCreateInfo, ByteBuffer pAllocator, LongBuffer pSetLayout);
    void vkDestroyDescriptorSetLayout(long descriptorSetLayout, ByteBuffer pAllocator);
    long vkCreateDescriptorPool(ByteBuffer pCreateInfo, ByteBuffer pAllocator, LongBuffer pDescriptorPool);
    void vkDestroyDescriptorPool(long descriptorPool, ByteBuffer pAllocator);
    void vkAllocateDescriptorSets(ByteBuffer pAllocateInfo, LongBuffer pDescriptorSets);
    void vkUpdateDescriptorSets(int descriptorWriteCount, ByteBuffer pDescriptorWrites, int descriptorCopyCount, ByteBuffer pDescriptorCopies);

    // ========== 命令缓冲 ==========
    long vkCreateCommandPool(ByteBuffer pCreateInfo, ByteBuffer pAllocator, LongBuffer pCommandPool);
    void vkDestroyCommandPool(long commandPool, ByteBuffer pAllocator);
    void vkAllocateCommandBuffers(ByteBuffer pAllocateInfo, LongBuffer pCommandBuffers);
    void vkFreeCommandBuffers(long commandPool, int commandBufferCount, LongBuffer pCommandBuffers);
    void vkBeginCommandBuffer(long commandBuffer, ByteBuffer pBeginInfo);
    void vkEndCommandBuffer(long commandBuffer);
    void vkCmdBeginRenderPass(long commandBuffer, ByteBuffer pRenderPassBegin, int contents);
    void vkCmdEndRenderPass(long commandBuffer);
    void vkCmdBindPipeline(long commandBuffer, int pipelineBindPoint, long pipeline);
    void vkCmdBindVertexBuffers(long commandBuffer, int firstBinding, int bindingCount, LongBuffer pBuffers, LongBuffer pOffsets);
    void vkCmdBindIndexBuffer(long commandBuffer, long buffer, long offset, int indexType);
    void vkCmdBindDescriptorSets(long commandBuffer, int pipelineBindPoint, long pipelineLayout, int firstSet, int descriptorSetCount, LongBuffer pDescriptorSets, int dynamicOffsetCount, IntBuffer pDynamicOffsets);
    void vkCmdDraw(long commandBuffer, int vertexCount, int instanceCount, int firstVertex, int firstInstance);
    void vkCmdDrawIndexed(long commandBuffer, int indexCount, int instanceCount, int firstIndex, int vertexOffset, int firstInstance);
    void vkCmdCopyBuffer(long commandBuffer, long srcBuffer, long dstBuffer, int regionCount, ByteBuffer pRegions);
    void vkCmdPipelineBarrier(long commandBuffer, int srcStageMask, int dstStageMask, int dependencyFlags, int memoryBarrierCount, ByteBuffer pMemoryBarriers, int bufferMemoryBarrierCount, ByteBuffer pBufferMemoryBarriers, int imageMemoryBarrierCount, ByteBuffer pImageMemoryBarriers);

    // ========== 同步对象 ==========
    long vkCreateSemaphore(ByteBuffer pCreateInfo, ByteBuffer pAllocator, LongBuffer pSemaphore);
    void vkDestroySemaphore(long semaphore, ByteBuffer pAllocator);
    long vkCreateFence(ByteBuffer pCreateInfo, ByteBuffer pAllocator, LongBuffer pFence);
    void vkDestroyFence(long fence, ByteBuffer pAllocator);
    void vkWaitForFences(int fenceCount, LongBuffer pFences, boolean waitAll, long timeout);
    void vkResetFences(int fenceCount, LongBuffer pFences);
    int vkQueueSubmit(long queue, int submitCount, ByteBuffer pSubmits, long fence);

    // ========== 内存映射 ==========
    int vkMapMemory(long memory, long offset, long size, int flags, ByteBuffer ppData);
    void vkUnmapMemory(long memory);
    void vkFlushMappedMemoryRanges(int memoryRangeCount, ByteBuffer pMemoryRanges);
    void vkInvalidateMappedMemoryRanges(int memoryRangeCount, ByteBuffer pMemoryRanges);

    // ========== 实用方法 ==========
    void vkGetBufferMemoryRequirements(long buffer, ByteBuffer pMemoryRequirements);
    void vkGetImageMemoryRequirements(long image, ByteBuffer pMemoryRequirements);
    int vkFindMemoryType(int typeFilter, int properties);
    long vkCreateSampler(ByteBuffer pCreateInfo, ByteBuffer pAllocator, LongBuffer pSampler);
    void vkDestroySampler(long sampler, ByteBuffer pAllocator);
}