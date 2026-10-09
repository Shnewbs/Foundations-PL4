package net.foundations.pl4;
import java.util.List;
import java.util.UUID;
import net.minecraftforge.common.ForgeConfigSpec;
/** Per-account local client history using the generic config API available on older Forge. */
public final class PLClientConfig {
 public static final ForgeConfigSpec SPEC;
 public static final ForgeConfigSpec.ConfigValue<List<? extends String>> COMMUNITY_LINKS_SEEN;
 static {
  var builder=new ForgeConfigSpec.Builder();
  COMMUNITY_LINKS_SEEN=builder.comment("Minecraft account UUIDs that received the one-time community links. Local client history.")
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
