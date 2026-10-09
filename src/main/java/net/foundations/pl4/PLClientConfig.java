package net.foundations.pl4;

import java.util.List;
import java.util.UUID;
import net.minecraftforge.common.ForgeConfigSpec;

/** Local per-account history, shared across worlds/servers and never synchronized by a server. */
public final class PLClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> COMMUNITY_LINKS_SEEN;
    static {
        var builder=new ForgeConfigSpec.Builder();
        COMMUNITY_LINKS_SEEN=builder.comment("Minecraft account UUIDs that have already received the one-time Discord/Ko-fi chat links. Local client history; keep this file across updates.").defineListAllowEmpty("communityLinksShownTo",List.<String>of(),entry->{
            if(!(entry instanceof String value))return false;
            try{return UUID.fromString(value).toString().equals(value);}catch(IllegalArgumentException failure){return false;}
        });
        SPEC=builder.build();
    }
    private PLClientConfig(){}
}
