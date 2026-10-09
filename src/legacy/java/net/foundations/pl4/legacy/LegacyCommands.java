package net.foundations.pl4.legacy;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatMessageComponent;
import net.minecraft.util.ChunkCoordinates;
public final class LegacyCommands extends CommandBase {
    public String getCommandName(){return "pl4legacy";}
    public String getCommandUsage(ICommandSender sender){return "/pl4legacy recipes [page] | inspect <x> <y> <z> | status";}
    @Override public int getRequiredPermissionLevel(){return 0;}
    public void processCommand(ICommandSender sender,String[] args){
        if(args.length==0||"status".equals(args[0])){send(sender,"PL4 1.6.4 experimental native item/fluid/energy transport preview. Name-based legacy ownership, sided inventories, escrow and built-in recipe/inspection fallbacks. Modern displays, wireless, third-party energy and scripting parity pending. Further API testing is still required.");return;}
        if("recipes".equals(args[0])){
            int page=args.length>1?parseIntBounded(sender,args[1],1,10000):1;List<IRecipe> recipes=new ArrayList<IRecipe>();
            for(Object value:CraftingManager.getInstance().getRecipeList())if(value instanceof IRecipe){IRecipe recipe=(IRecipe)value;ItemStack result=recipe.getRecipeOutput();if(result!=null&&(result.itemID==LegacyPL4.CABLE.blockID||result.itemID==LegacyPL4.EXPORT.blockID||result.itemID==LegacyPL4.IMPORT.blockID||result.itemID==LegacyPL4.FLUID_EXPORT.blockID||result.itemID==LegacyPL4.FLUID_IMPORT.blockID||result.itemID==LegacyPL4.TANK.blockID||result.itemID==LegacyPL4.ENERGY_EXPORT.blockID||result.itemID==LegacyPL4.ENERGY_IMPORT.blockID||result.itemID==LegacyPL4.ENERGY_BUFFER.blockID))recipes.add(recipe);}
            int pages=Math.max(1,(recipes.size()+3)/4);if(page>pages)throw new WrongUsageException("Page exceeds "+pages);send(sender,"PL4 native registered recipes - page "+page+"/"+pages);
            for(int i=(page-1)*4;i<Math.min(page*4,recipes.size());i++){IRecipe r=recipes.get(i);ItemStack output=r.getRecipeOutput();StringBuilder s=new StringBuilder().append(output.stackSize).append(" x ").append(output.getDisplayName()).append(" | Ingredients: ");if(r instanceof ShapedRecipes)for(ItemStack stack:((ShapedRecipes)r).recipeItems)append(s,stack);else if(r instanceof ShapelessRecipes)for(Object stack:((ShapelessRecipes)r).recipeItems)append(s,stack);else s.append("Custom recipe - consult provider");send(sender,s.toString());}return;
        }
        if("inspect".equals(args[0])&&args.length==4){int x=parseInt(sender,args[1]),y=parseInt(sender,args[2]),z=parseInt(sender,args[3]);ChunkCoordinates at=sender.getPlayerCoordinates();double dx=(double)x-at.posX,dy=(double)y-at.posY,dz=(double)z-at.posZ;
            if(!(sender instanceof EntityPlayer)||dx*dx+dy*dy+dz*dz>64||!sender.getEntityWorld().blockExists(x,y,z))throw new WrongUsageException("Stand within 8 blocks of a loaded PL4 host");TileEntity tile=sender.getEntityWorld().getBlockTileEntity(x,y,z);if(!(tile instanceof LegacyTile)||!((LegacyTile)tile).canEdit((EntityPlayer)sender))throw new WrongUsageException("No accessible PL4 host");send(sender,((LegacyTile)tile).describe());return;}
        throw new WrongUsageException(getCommandUsage(sender));
    }
    private static void append(StringBuilder s,Object value){if(value instanceof ItemStack)s.append(((ItemStack)value).getDisplayName()).append("; ");}
    private static void send(ICommandSender sender,String message){sender.sendChatToPlayer(ChatMessageComponent.createFromText(message));}
}
