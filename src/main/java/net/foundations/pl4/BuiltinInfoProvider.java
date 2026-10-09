package net.foundations.pl4;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.CropBlock;

/** Vanilla read-only telemetry; historical keys remain stable for existing displays. */
public final class BuiltinInfoProvider {
    private BuiltinInfoProvider() {}
    public static void sample(net.foundations.pl4.api.InfoProviders.Context context,net.foundations.pl4.api.InfoProviders.Sink sink){
        if(context.entity()!=null)entity(sink,context.entity());else info(sink,context.level(),context.target());
    }
    private static void put(net.foundations.pl4.api.InfoProviders.Sink rows,String key,String name,double value,double capacity,String unit){rows.add(key,name,value,capacity,unit);}
    private static void info(net.foundations.pl4.api.InfoProviders.Sink rows,ServerLevel l,Part.Link link){
        BlockPos pos=link.pos();var state=l.getBlockState(pos);
        put(rows,"x","X",pos.getX(),0,"");put(rows,"y","Y",pos.getY(),0,"");put(rows,"z","Z",pos.getZ(),0,"");
        put(rows,"redstone","Redstone",l.getBestNeighborSignal(pos),15,"");
        put(rows,"light","Block light",l.getBrightness(LightLayer.BLOCK,pos),15,"");put(rows,"sky_light","Sky light",l.getBrightness(LightLayer.SKY,pos),15,"");
        put(rows,"rain","Raining",l.isRaining()?1:0,1,"");put(rows,"thunder","Thundering",l.isThundering()?1:0,1,"");
        put(rows,"day_time","Day time",l.getDayTime()%24000,24000,"ticks");put(rows,"hardness","Hardness",state.getDestroySpeed(l,pos),0,"");
        if(state.getBlock() instanceof CropBlock crop)put(rows,"crop_age","Crop growth",crop.getAge(state),crop.getMaxAge(),"");
        for(var property:state.getProperties())if(state.getValue(property) instanceof Number number)put(rows,"state."+property.getName(),property.getName(),number.doubleValue(),0,"");
        if(l.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity furnace){
            var tag=furnace.saveWithoutMetadata();
            put(rows,"burn_time","Burn time",tag.getShort("BurnTime"),0,"ticks");put(rows,"cook_time","Cooking",tag.getShort("CookTime"),tag.getShort("CookTimeTotal"),"ticks");
        }
    }
    private static void entity(net.foundations.pl4.api.InfoProviders.Sink rows,Entity e){
        put(rows,"x","X",e.getX(),0,"");put(rows,"y","Y",e.getY(),0,"");put(rows,"z","Z",e.getZ(),0,"");
        put(rows,"speed","Speed",e.getDeltaMovement().length()*20,0,"blocks/s");
        if(e instanceof LivingEntity living){put(rows,"health","Health",living.getHealth(),living.getMaxHealth(),"HP");put(rows,"armor","Armor",living.getArmorValue(),20,"");}
        if(e instanceof Player player){put(rows,"food","Hunger",player.getFoodData().getFoodLevel(),20,"");put(rows,"saturation","Saturation",player.getFoodData().getSaturationLevel(),20,"");put(rows,"xp","XP level",player.experienceLevel,0,"");}
    }
}
