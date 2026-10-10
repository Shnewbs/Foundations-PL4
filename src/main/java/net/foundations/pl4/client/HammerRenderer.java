package net.foundations.pl4.client;
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.foundations.pl4.*;
import org.lwjgl.opengl.GL11;

/** Forge28 native TESR; preserves original moving hammer pieces and the
 * real inventory item lying on its working surface.
 */
public final class HammerRenderer extends TileEntityRenderer<HammerEntity> {
    private static final ResourceLocation TEXTURE=FoundationsPL4.id("textures/block/model/forging_hammer_stone.png");
    private final HammerModel model=new HammerModel();
    private final ItemRenderer items=net.minecraft.client.Minecraft.getInstance().getItemRenderer();
    public HammerRenderer(){super();}
    @Override public void render(HammerEntity hammer,double rx,double ry,double rz,float partial,int destroyStage){
        GL11.glPushMatrix();GL11.glPushAttrib(GL11.GL_ENABLE_BIT|GL11.GL_TEXTURE_BIT|GL11.GL_COLOR_BUFFER_BIT);
        try{
            GL11.glTranslated(rx,ry,rz);
            GL11.glPushMatrix();
            try{
                GL11.glTranslated(.5,1.5,.5);
                GL11.glRotatef(-hammer.getBlockState().getValue(HammerBlock.FACING).toYRot(),0,1,0);
                GL11.glRotatef(180,1,0,0);
                net.minecraft.client.Minecraft.getInstance().getTextureManager().bind(TEXTURE);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                model.render(hammer.animationFraction(partial),hammer.structureReady());
            }finally{GL11.glPopMatrix();}
            ItemStack stack=hammer.inventory.getStackInSlot(0);
            if(stack.isEmpty())stack=hammer.inventory.getStackInSlot(1);
            if(!stack.isEmpty()){
                GL11.glPushMatrix();
                try{
                    GL11.glTranslated(.5,.89,.5);GL11.glScalef(.5F,.5F,.5F);
                    items.renderStatic(stack,TransformType.GROUND);
                }finally{GL11.glPopMatrix();}
            }
        }finally{GL11.glPopAttrib();GL11.glPopMatrix();}
    }
    public AxisAlignedBB getRenderBoundingBox(HammerEntity hammer){return new AxisAlignedBB(hammer.getBlockPos()).expandTowards(0,2,0);}
    public int getViewDistance(){return 64;}
}
