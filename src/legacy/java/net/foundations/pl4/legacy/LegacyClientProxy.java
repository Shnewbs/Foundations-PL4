package net.foundations.pl4.legacy;
public final class LegacyClientProxy extends LegacyProxy {public void init(){cpw.mods.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(LegacyTile.class,new LegacyDisplayRenderer());}}
