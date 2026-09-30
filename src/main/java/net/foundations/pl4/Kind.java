package net.foundations.pl4;

/** Registry names deliberately preserve the PL2 names. */
public enum Kind {
    DATA_CABLE("datacable"), REDSTONE_CABLE("redstonecable"), NODE("node"),
    INFO_READER("inforeader"), INVENTORY_READER("inventoryreader"), FLUID_READER("fluidreader"),
    ENERGY_READER("energyreader"), NETWORK_READER("networkreader"), ARRAY("array"),
    ENTITY_NODE("entitynode"), TRANSFER_NODE("transfernode"), REDSTONE_NODE("redstonenode"),
    CLOCK("clock"), SIGNALLER("redstonesignaller"), DISPLAY("displayscreen"),
    MINI_DISPLAY("minidisplay"), LARGE_DISPLAY("largedisplayscreen"), HOLOGRAM("holographicdisplay"),
    ADVANCED_HOLOGRAM("advancedholographicdisplay"), DATA_EMITTER("dataemitter"),
    DATA_RECEIVER("datareceiver"), REDSTONE_EMITTER("redstoneemitter"), REDSTONE_RECEIVER("redstonereceiver");

    public final String id;
    Kind(String id) { this.id = id; }
    public boolean cable() { return this == DATA_CABLE || this == REDSTONE_CABLE; }
    public boolean reader() { return name().endsWith("READER"); }
    public boolean display() { return this == DISPLAY || this == MINI_DISPLAY || this == LARGE_DISPLAY || this == HOLOGRAM || this == ADVANCED_HOLOGRAM; }
    public boolean panelDisplay() { return this == DISPLAY || this == MINI_DISPLAY || this == LARGE_DISPLAY; }
    public boolean redstone() { return this == REDSTONE_CABLE || this == REDSTONE_NODE || this == REDSTONE_EMITTER || this == REDSTONE_RECEIVER; }
    public boolean emitter() { return this == DATA_EMITTER || this == REDSTONE_EMITTER; }
    public boolean receiver() { return this == DATA_RECEIVER || this == REDSTONE_RECEIVER; }
    public static Kind byId(String id) { for (Kind k : values()) if (k.id.equals(id)) return k; return null; }
}
