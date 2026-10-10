package net.foundations.pl4.client;
import net.foundations.pl4.core.HammerGeometry;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/** Preserve original 128x64 PL2 hammer box UVs, pivots and animation using
 * Forge 28's pre-MatrixStack immediate tessellation. No model parts are removed.
 */
public final class HammerModel {
    public void render(double progress,boolean assembled){
        for(var piece:HammerGeometry.PIECES){
            if(piece.upper()&&!assembled)continue;
            GL11.glPushMatrix();
            try{
                float move=piece.moving()?HammerGeometry.TRAVEL*(float)progress:0;
                GL11.glTranslatef(piece.px()/16F,(piece.py()+move)/16F,piece.pz()/16F);
                GL11.glRotatef((float)Math.toDegrees(piece.rz()),0,0,1);
                GL11.glRotatef((float)Math.toDegrees(piece.ry()),0,1,0);
                GL11.glRotatef((float)Math.toDegrees(piece.rx()),1,0,0);
                box(piece);
            }finally{GL11.glPopMatrix();}
        }
    }
    private static void face(BufferBuilder b,
       float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz,float dx,float dy,float dz,
       float u0,float v0,float u1,float v1,float nx,float ny,float nz){
        // Shader-less Forge28 pipeline expects normalized UV and a real normal.
        vertex(b,ax,ay,az,u0,v0,nx,ny,nz);
        vertex(b,bx,by,bz,u1,v0,nx,ny,nz);
        vertex(b,cx,cy,cz,u1,v1,nx,ny,nz);
        vertex(b,dx,dy,dz,u0,v1,nx,ny,nz);
    }
    private static void vertex(BufferBuilder b,float x,float y,float z,float u,float v,float nx,float ny,float nz){
        b.vertex(x,y,z).uv(u/HammerGeometry.TEXTURE_WIDTH,v/HammerGeometry.TEXTURE_HEIGHT).normal(nx,ny,nz).endVertex();
    }
    private static void box(HammerGeometry.Piece p){
        float x0=p.x()/16F,x1=(p.x()+p.width())/16F;
        float y0=p.y()/16F,y1=(p.y()+p.height())/16F;
        float z0=p.z()/16F,z1=(p.z()+p.depth())/16F;
        float u=p.u(),v=p.v(),du=p.depth(),wu=p.width(),hu=p.height();
        Tessellator tess=Tessellator.getInstance();BufferBuilder b=tess.getBuilder();
        b.begin(GL11.GL_QUADS,DefaultVertexFormats.POSITION_TEX_NORMAL);
        face(b,x1,y0,z0,x0,y0,z0,x0,y0,z1,x1,y0,z1,u+du,v,u+du+wu,v+du,0,-1,0);
        face(b,x0,y1,z0,x1,y1,z0,x1,y1,z1,x0,y1,z1,u+du+wu,v,u+du+2*wu,v+du,0,1,0);
        face(b,x0,y0,z0,x1,y0,z0,x1,y1,z0,x0,y1,z0,u+du,v+du,u+du+wu,v+du+hu,0,0,-1);
        face(b,x1,y0,z1,x0,y0,z1,x0,y1,z1,x1,y1,z1,u+2*du+wu,v+du,u+2*du+2*wu,v+du+hu,0,0,1);
        face(b,x0,y0,z1,x0,y0,z0,x0,y1,z0,x0,y1,z1,u,v+du,u+du,v+du+hu,-1,0,0);
        face(b,x1,y0,z0,x1,y0,z1,x1,y1,z1,x1,y1,z0,u+du+wu,v+du,u+2*du+wu,v+du+hu,1,0,0);
        tess.end();
    }
}
