package net.foundations.pl4.legacy;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.world.ChunkEvent;
/** One pass when a server chunk becomes available; no all-world scan or provider call. */
public final class LegacyItemFaultImport {
    @SubscribeEvent public void loaded(ChunkEvent.Load event){
        if(event.getWorld().isRemote)return;
        for(Object value:event.getChunk().getTileEntityMap().values())if(value instanceof LegacyTile)LegacyItemSafety.importSaved((LegacyTile)value);
    }
}
