package me.jellysquid.mods.sodium.client.render.chunk.vertex.builder;

import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class ChunkMeshBufferBuilder {
    private final ChunkVertexEncoder encoder;
    private final int stride;

    private final int initialCapacity;

    // Pre-allocate a buffer to reduce reallocations
    private ByteBuffer buffer;
    private int count;
    private int capacity;
    private int sectionIndex;

    public ChunkMeshBufferBuilder(ChunkVertexType vertexType, int initialCapacity) {
        this.encoder = vertexType.getEncoder();
        this.stride = vertexType.getVertexFormat().getStride();

        this.initialCapacity = initialCapacity;
        this.setBufferSize(initialCapacity); // Pre-allocate buffer
    }

    public void push(ChunkVertexEncoder.Vertex[] vertices, Material material) {
        var vertexStart = this.count;
        var vertexCount = vertices.length;

        // Check if we need to grow the buffer before writing
        if (this.count + vertexCount >= this.capacity) {
            this.grow(vertexCount);
        }

        // Direct memory access for writing vertices
        long ptr = MemoryUtil.memAddress(this.buffer, this.count * this.stride);

        for (ChunkVertexEncoder.Vertex vertex : vertices) {
            ptr = this.encoder.write(ptr, material, vertex, this.sectionIndex);
        }

        this.count += vertexCount;
    }

    // Grow the buffer by a calculated amount
    private void grow(int vertexCount) {
        // Calculate the new capacity, at least doubling the current size or
        // adding the required space for the new vertices
        this.capacity = Math.max(this.capacity * 2, this.capacity + vertexCount);

        // Allocate a new buffer and copy existing data
        this.setBufferSize(this.capacity);
    }

    // Resize the buffer with potential data copying
    private void setBufferSize(int capacity) {
        // Reallocate buffer only if needed, avoiding unnecessary copies
        if (this.capacity != capacity) {
            this.buffer = MemoryUtil.memRealloc(this.buffer, capacity * this.stride);
            this.capacity = capacity;
        }
    }

    public void start(int sectionIndex) {
        this.count = 0;
        this.sectionIndex = sectionIndex;

        // Reset the buffer pointer to the beginning
        MemoryUtil.memSet(this.buffer, 0, this.capacity * this.stride);
    }

    public void destroy() {
        if (this.buffer != null) {
            MemoryUtil.memFree(this.buffer);
        }

        this.buffer = null;
    }

    public boolean isEmpty() {
        return this.count == 0;
    }

    public ByteBuffer slice() {
        if (this.isEmpty()) {
            throw new IllegalStateException("No vertex data in buffer");
        }

        // Return a slice of the buffer containing valid vertex data
        return this.buffer.slice(0, this.stride * this.count);
    }

    public int count() {
        return this.count;
    }
}
