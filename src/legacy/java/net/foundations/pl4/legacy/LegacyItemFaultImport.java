package net.foundations.pl4.legacy;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.world.ChunkEvent;
/** One pass when a server chunk becomes available; no all-world scan or provider call. */
public final class LegacyItemFaultImport {
    @ForgeSubscribe public void loaded(ChunkEvent.Load event){
        if(event.world.isRemote)return;
        for(Object value:event.getChunk().chunkTileEntityMap.values())if(value instanceof LegacyTile)LegacyItemSafety.importSaved((LegacyTile)value);
    }
}
