package net.foundations.pl4;

import java.util.List;
import java.util.UUID;
import net.minecraftforge.common.ForgeConfigSpec;

/** Local per-account history; never synchronized by a server. Compatible with Forge 35. */
public final class PLClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> COMMUNITY_LINKS_SEEN;
    static {
        var builder=new ForgeConfigSpec.Builder();
        // Forge 35 lacks defineListAllowEmpty. Validate the entire list using its native generic API.
        COMMUNITY_LINKS_SEEN=builder.comment("Minecraft account UUIDs that have already received the one-time Discord/Ko-fi chat links. Local client history; keep this file across updates.")
                .<List<? extends String>>define("communityLinksShownTo",List.<String>of(),PLClientConfig::validHistory);
        SPEC=builder.build();
    }
    public static boolean validHistory(Object input){
        if(!(input instanceof List<?> entries))return false;
        for(Object entry:entries){
            if(!(entry instanceof String value))return false;
            try{if(!UUID.fromString(value).toString().equals(value))return false;}
            catch(IllegalArgumentException failure){return false;}
        }
        return true;
    }
    private PLClientConfig(){}
}
