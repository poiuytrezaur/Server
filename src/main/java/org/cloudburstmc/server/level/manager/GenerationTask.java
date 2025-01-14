package org.cloudburstmc.server.level.manager;

import com.nukkitx.protocol.bedrock.BedrockRakNetSessionListener;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.daporkchop.lib.random.PRandom;
import net.daporkchop.lib.random.impl.FastPRandom;
import org.cloudburstmc.server.level.chunk.Chunk;
import org.cloudburstmc.server.level.chunk.IChunk;
import org.cloudburstmc.server.level.chunk.LockableChunk;
import org.cloudburstmc.server.level.generator.Generator;

import java.util.function.Function;

/**
 * Delegates chunk generation to a {@link Generator}.
 *
 * @author DaPorkchop_
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GenerationTask implements Function<Chunk, Chunk> {
    public static final GenerationTask INSTANCE = new GenerationTask();

    @Override
    public Chunk apply(@NonNull Chunk chunk) {
        if (chunk.isGenerated()) {
            return chunk;
        }

        PRandom random = new FastPRandom(chunk.getX() * 3053330778986901431L ^ chunk.getZ() * 1517227374085824433L ^ chunk.getLevel().getSeed());
        LockableChunk lockable = chunk.writeLockable();

        lockable.lock();
        try {
            System.out.println("Generating chunk!");
            chunk.getLevel().getGenerator().generate(random, lockable, chunk.getX(), chunk.getZ());
            chunk.setState(IChunk.STATE_GENERATED);
            chunk.setDirty();
        } finally {
            lockable.unlock();
        }
        return chunk;
    }
}
