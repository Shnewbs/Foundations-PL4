package net.foundations.pl4.legacy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
/** Built-in inspection and actual recipe lookup, without mandatory viewer/tooltip mods. */
public final class LegacyCommands extends CommandBase {
    @Override public String getName(){return "pl4legacy";}
    @Override public String getUsage(ICommandSender sender){return "/pl4legacy recipes [page] | inspect <x> <y> <z> | status";}
    @Override public int getRequiredPermissionLevel(){return 0;}
    @Override public void execute(MinecraftServer server,ICommandSender sender,String[] args)throws CommandException{
        if(args.length==0||"status".equals(args[0])){send(sender,"Foundations PL4 1.12.2 native legacy preview: item/fluid/energy transport, persisted escrow, owner-partitioned cables and recipe/inspection fallbacks. Advanced displays, wireless storage and third-party power parity remain pending. Further API testing is still required.");return;}
        if("recipes".equals(args[0])){
            int page=args.length>1?parseInt(args[1],1,10000):1;List<IRecipe> recipes=new ArrayList<IRecipe>();
            for(IRecipe recipe:ForgeRegistries.RECIPES)if(recipe.getRegistryName()!=null&&LegacyPL4.ID.equals(recipe.getRegistryName().getResourceDomain()))recipes.add(recipe);
            Collections.sort(recipes,new java.util.Comparator<IRecipe>(){public int compare(IRecipe a,IRecipe b){return a.getRegistryName().toString().compareTo(b.getRegistryName().toString());}});
            int pages=Math.max(1,(recipes.size()+3)/4);if(page>pages)throw new CommandException("Page exceeds "+pages);send(sender,"PL4 registered recipes - page "+page+"/"+pages);
            for(int i=(page-1)*4;i<Math.min(page*4,recipes.size());i++){
                IRecipe recipe=recipes.get(i);ItemStack output=recipe.getRecipeOutput();StringBuilder text=new StringBuilder(recipe.getRegistryName().toString()).append(" -> ").append(output.getCount()).append(" x ").append(output.getDisplayName()).append(" | Ingredients: ");
                for(Ingredient ingredient:recipe.getIngredients()){ItemStack[] choices=ingredient.getMatchingStacks();if(choices.length==0)continue;text.append(choices[0].getDisplayName());if(choices.length>1)text.append(" (+").append(choices.length-1).append(" alternatives)");text.append("; ");}
                send(sender,text.toString());
            }return;
        }
        if("inspect".equals(args[0])&&args.length==4){
            BlockPos pos=parseBlockPos(sender,args,1,false);
            if(!(sender instanceof EntityPlayer)||sender.getPosition().distanceSq(pos)>64||!sender.getEntityWorld().isBlockLoaded(pos,false))throw new CommandException("Stand within 8 blocks of a loaded PL4 host.");
            TileEntity raw=sender.getEntityWorld().getTileEntity(pos);if(!(raw instanceof LegacyTile)||!((LegacyTile)raw).canEdit((EntityPlayer)sender))throw new CommandException("No accessible PL4 host at that position.");send(sender,((LegacyTile)raw).describe());return;
        }
        throw new CommandException(getUsage(sender));
    }
    private static void send(ICommandSender sender,String message){sender.sendMessage(new TextComponentString(message));}
}
