package net.foundations.pl4;

import java.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.state.properties.ChestType;

/** Per-sample identities only; never retain world handlers across ticks. */
final class SampleSources {
    private record PlayerInventory(ServerWorld level,BlockPos pos) {}
    private final Set<Object> handlers=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<PlayerInventory> inventories=new HashSet<>();
    boolean inventory(ServerWorld level,Part.Link link,Object handler,int slots){
        if(!handlers.add(handler))return false;
        var state=level.getBlockState(link.pos());BlockPos canonical=link.pos();
        if((state.getBlock()==Blocks.CHEST)||(state.getBlock()==Blocks.TRAPPED_CHEST)){
            if(slots==54&&state.getValue(ChestBlock.TYPE)!=ChestType.SINGLE){
                BlockPos other=link.pos().relative(ChestBlock.getConnectedDirection(state));
                if(other.asLong()<canonical.asLong())canonical=other;
            }
            return inventories.add(new PlayerInventory(level,canonical));
        }
        if((state.getBlock()==Blocks.BARREL)||state.getBlock() instanceof net.minecraft.block.ShulkerBoxBlock)
            return inventories.add(new PlayerInventory(level,canonical));
        return true; // Distinct sided mod wrappers may expose different slots; do not guess.
    }
    boolean fluid(Object handler){return handlers.add(handler);}
}
