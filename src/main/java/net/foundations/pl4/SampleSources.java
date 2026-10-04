package net.foundations.pl4;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;

/** Per-sample identities only; never retain world handlers across ticks. */
final class SampleSources {
    private record Inventory(ServerLevel level,BlockPos pos) {}
    private final Set<Object> handlers=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Inventory> inventories=new HashSet<>();
    boolean inventory(ServerLevel level,Part.Link link,Object handler,int slots){
        if(!handlers.add(handler))return false;
        var state=level.getBlockState(link.pos());BlockPos canonical=link.pos();
        if(state.is(Blocks.CHEST)||state.is(Blocks.TRAPPED_CHEST)){
            if(slots==54&&state.getValue(ChestBlock.TYPE)!=ChestType.SINGLE){
                BlockPos other=link.pos().relative(ChestBlock.getConnectedDirection(state));
                if(other.asLong()<canonical.asLong())canonical=other;
            }
            return inventories.add(new Inventory(level,canonical));
        }
        if(state.is(Blocks.BARREL)||state.getBlock() instanceof net.minecraft.world.level.block.ShulkerBoxBlock)
            return inventories.add(new Inventory(level,canonical));
        return true; // Distinct sided mod wrappers may expose different slots; do not guess.
    }
    boolean fluid(Object handler){return handlers.add(handler);}
}
