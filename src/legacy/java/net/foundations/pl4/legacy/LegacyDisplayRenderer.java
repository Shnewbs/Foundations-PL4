package net.foundations.pl4.legacy;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.opengl.GL11;
/** Four bounded lines on the selected horizontal face. Client proxy owns registration. */
public final class LegacyDisplayRenderer extends TileEntitySpecialRenderer {
    @Override public void renderTileEntityAt(TileEntity raw,double x,double y,double z,float partial){
        LegacyTile tile=(LegacyTile)raw;if(tile.role()!=LegacyReadings.DISPLAY)return;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT|GL11.GL_CURRENT_BIT|GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glPushMatrix();
        try{
            GL11.glTranslated(x+.5,y+.5,z+.5);
            int side=tile.displayFacing;GL11.glRotatef(side==2?180:side==4?-90:side==5?90:0,0,1,0);
            GL11.glTranslated(0,0,.501);GL11.glScaled(1D/128,-1D/128,1D/128);
            GL11.glDisable(GL11.GL_LIGHTING);GL11.glDisable(GL11.GL_CULL_FACE);GL11.glColor4f(1,1,1,1);
            // Opaque screen covers the case face; text has a small, ordered depth offset.
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            panel(-61,61,-.02F,.20F,.42F,.43F);
            panel(-59,59,-.01F,.025F,.045F,.05F);
            GL11.glEnable(GL11.GL_TEXTURE_2D);GL11.glColor4f(1,1,1,1);
            FontRenderer font=getFontRenderer();
            for(int i=0;i<4;i++)font.drawString(font.trimStringToWidth(tile.displayLines[i],112),-56,-23+i*13,0xE7F5EF);
        }finally{GL11.glPopMatrix();GL11.glPopAttrib();}
    }
    private static void panel(float lo,float hi,float z,float r,float g,float b){
        GL11.glColor4f(r,g,b,1);GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3f(lo,lo,z);GL11.glVertex3f(hi,lo,z);GL11.glVertex3f(hi,hi,z);GL11.glVertex3f(lo,hi,z);GL11.glEnd();
    }
}
