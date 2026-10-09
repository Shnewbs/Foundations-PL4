package net.foundations.pl4.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.foundations.pl4.*;

public final class HostRenderer implements BlockEntityRenderer<HostEntity> {
    private static final Direction[] FACES=Direction.values();
    private final DisplayPainter displayPainter=new DisplayPainter();
    private final net.minecraft.world.level.block.state.BlockState[][][] cableStates=new net.minecraft.world.level.block.state.BlockState[3][4][6];
    private final net.minecraft.world.level.block.state.BlockState[][] kineticStates=new net.minecraft.world.level.block.state.BlockState[2][6],ae2States=new net.minecraft.world.level.block.state.BlockState[2][6];
    private final java.util.EnumMap<Kind,net.minecraft.world.level.block.state.BlockState[][]> partStates=new java.util.EnumMap<>(Kind.class);
    private final net.minecraft.world.level.block.state.BlockState[][][] largeStates=new net.minecraft.world.level.block.state.BlockState[2][6][16];
    private final java.util.EnumMap<Kind,net.minecraft.world.level.block.state.BlockState[][]> leadModels=new java.util.EnumMap<>(Kind.class);
    public HostRenderer(BlockEntityRendererProvider.Context c){
        String[] materials={"data","redstone_off","redstone_on"},connectors={"centre","cable","internal","half"};
        var leadStates=new java.util.HashMap<String,net.minecraft.world.level.block.state.BlockState[]>();
        for(int m=0;m<3;m++)for(int type=0;type<4;type++)for(Direction face:FACES)
            cableStates[m][type][face.ordinal()]=FoundationsPL4.CABLE_MODELS.get(materials[m]+"_"+connectors[type]).get().defaultBlockState().setValue(CableModelBlock.FACING,face);
        for(Kind kind:Kind.values()){
            var states=new net.minecraft.world.level.block.state.BlockState[2][6];
            for(int side=0;side<2;side++)for(Direction face:FACES)states[side][face.ordinal()]=FoundationsPL4.MODELS.get(kind).get().defaultBlockState().setValue(PartModelBlock.FACING,face).setValue(PartModelBlock.FRONT_OUTWARD,side==1).setValue(PartModelBlock.HAS_DISPLAY,kind.reader()&&side==1);
            partStates.put(kind,states);
        }
        for(int covered=0;covered<2;covered++)for(Direction face:FACES){
            kineticStates[covered][face.ordinal()]=FoundationsPL4.KINETIC_READER_MODEL.get().defaultBlockState().setValue(PartModelBlock.FACING,face).setValue(PartModelBlock.HAS_DISPLAY,covered==1);
            ae2States[covered][face.ordinal()]=FoundationsPL4.AE2_READER_MODEL.get().defaultBlockState().setValue(PartModelBlock.FACING,face).setValue(PartModelBlock.HAS_DISPLAY,covered==1);
        }
        for(String material:materials)for(String depth:new String[]{"1","15","2","3","4","6"}){
            String key=material+"_lead_"+depth;var states=new net.minecraft.world.level.block.state.BlockState[6];
            for(Direction face:FACES)states[face.ordinal()]=FoundationsPL4.CABLE_MODELS.get(key).get().defaultBlockState().setValue(CableModelBlock.FACING,face);
            leadStates.put(key,states);
        }
        for(Kind kind:Kind.values()){
            var states=new net.minecraft.world.level.block.state.BlockState[3][];
            for(int material=0;material<3;material++)states[material]=leadStates.get(materials[material]+"_lead_"+MultipartShapes.leadKey(kind));
            leadModels.put(kind,states);
        }
        for(int side=0;side<2;side++)for(Direction face:FACES)for(int mask=0;mask<16;mask++)largeStates[side][face.ordinal()][mask]=FoundationsPL4.LARGE_MODEL.get().defaultBlockState().setValue(LargeDisplayModelBlock.FACING,face).setValue(LargeDisplayModelBlock.CONNECTIONS,mask).setValue(LargeDisplayModelBlock.FRONT_OUTWARD,side==1);
    }
    @Override public void render(HostEntity host,float partial,PoseStack pose,MultiBufferSource buffer,int light,int overlay){
        Minecraft mc=Minecraft.getInstance();
        for(Part part:host.parts.values()){
            if(part.kind.cable()) {
                int material=part.kind==Kind.DATA_CABLE?0:(part.signal>0?2:1);
                mc.getBlockRenderer().renderSingleBlock(cableStates[material][0][0],pose,buffer,light,overlay);
                for(Direction d:FACES) {
                    int type=host.connection(d);if(type==0)continue;
                    mc.getBlockRenderer().renderSingleBlock(cableStates[material][type][d.ordinal()],pose,buffer,light,overlay);
                }
            } else if(part.kind==Kind.LARGE_DISPLAY){
                var model=largeStates[part.displayOutward?1:0][part.face.ordinal()][part.canvasMask];
                mc.getBlockRenderer().renderSingleBlock(model,pose,buffer,light,overlay);
            } else {
                pose.pushPose();
                if(part.hologram()){
                    int yaw=net.foundations.pl4.core.HologramProjection.baseYaw(part.face.ordinal(),part.hologramView);
                    pose.translate(.5,.5,.5);pose.mulPose(Axis.YP.rotationDegrees(yaw));pose.translate(-.5,-.5,-.5);
                }
                var model=partStates.get(part.kind)[part.kind.reader()?(host.readerHasDisplay(part)?1:0):(part.displayOutward?1:0)][part.face.ordinal()];
                if(part.kind==Kind.ENERGY_READER){if(part.energySystem.equals("CREATE"))model=kineticStates[host.readerHasDisplay(part)?1:0][part.face.ordinal()];else if(part.energySystem.equals("AE2"))model=ae2States[host.readerHasDisplay(part)?1:0][part.face.ordinal()];}
                mc.getBlockRenderer().renderSingleBlock(model,pose,buffer,light,overlay);
                pose.popPose();
            }
            if(host.externalLead(part)){
                int material=part.kind.redstone()?(part.signal>0?2:1):0;
                mc.getBlockRenderer().renderSingleBlock(leadModels.get(part.kind)[material][part.face.ordinal()],pose,buffer,light,overlay);
            }
            if(part.kind.display()&&(part.kind!=Kind.LARGE_DISPLAY||(part.canvasColumn==0&&part.canvasRow==0)))display(host,part,pose,buffer,mc.font);
        }
    }
    private void display(HostEntity host,Part p,PoseStack pose,MultiBufferSource buffer,Font font){
        var eye=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        net.foundations.pl4.core.DisplayFacing.Frame frame;
        double ox,oy,oz;
        if(p.hologram()){
            var projection=net.foundations.pl4.core.HologramProjection.forCamera(p.face.ordinal(),p.hologramView,p.kind==Kind.ADVANCED_HOLOGRAM,
                new net.foundations.pl4.core.HologramProjection.Point(eye.x-host.getBlockPos().getX(),eye.y-host.getBlockPos().getY(),eye.z-host.getBlockPos().getZ()));
            frame=projection.frame();ox=projection.centre().x();oy=projection.centre().y();oz=projection.centre().z();
        }else{
            frame=net.foundations.pl4.core.DisplayFacing.frame(p.face.ordinal(),p.displayOutward);
            double offset=net.foundations.pl4.core.DisplayFacing.planeOffset(p.displayOutward);
            ox=.5+p.face.getStepX()*offset;oy=.5+p.face.getStepY()*offset;oz=.5+p.face.getStepZ()*offset;
            if((eye.x-host.getBlockPos().getX()-ox)*frame.normal().x()+(eye.y-host.getBlockPos().getY()-oy)*frame.normal().y()+(eye.z-host.getBlockPos().getZ()-oz)*frame.normal().z()<=0)return;
        }
        pose.pushPose();pose.translate(ox,oy,oz);
        if(frame.rotationX()!=0)pose.mulPose(Axis.XP.rotationDegrees(frame.rotationX()));
        if(frame.rotationY()!=0)pose.mulPose(Axis.YP.rotationDegrees(frame.rotationY()));
        float scale=p.kind==Kind.MINI_DISPLAY?.0018F:.0035F;
        int logicalW=net.foundations.pl4.core.DisplayElements.WIDTH,logicalH=net.foundations.pl4.core.DisplayElements.HEIGHT;
        if(p.kind==Kind.LARGE_DISPLAY){
            var space=net.foundations.pl4.core.DynamicCanvasLayout.large(p.canvasWidth,p.canvasHeight);logicalW=space.width();logicalH=space.height();scale=(float)space.scale();
            pose.translate((p.canvasWidth-1)/2.0,-(p.canvasHeight-1)/2.0,0);
            pose.translate(-logicalW*scale/2.0,logicalH*scale/2.0,0);
        }else pose.translate(-124*scale,60*scale,0);
        pose.scale(scale,-scale,scale);
        var canvas=new DisplayCanvas(pose,buffer,scale);
        var active=DisplayEditorScreen.active();
        var editor=active!=null&&active.matches(host,p)?active:null;
        displayPainter.paint(p,editor==null?p.elements:editor.preview(p),canvas,
            p.kind==Kind.MINI_DISPLAY?new net.foundations.pl4.core.MonitorPresentation.Region(0,-64,248,248):
            p.kind==Kind.LARGE_DISPLAY?new net.foundations.pl4.core.MonitorPresentation.Region(0,0,logicalW,logicalH):new net.foundations.pl4.core.MonitorPresentation.Region(0,0,248,120),editor!=null);
        if(editor!=null){editor.capture(pose.last().pose());editor.drawOnMonitor(canvas,p);}
        pose.popPose();
    }
    private void text(Font font,String text,int x,int y,int color,PoseStack pose,MultiBufferSource buffer){if(x<0||x>=248||y<0||y>111)return;font.drawInBatch(font.plainSubstrByWidth(text,Math.max(0,248-x)),x,y,0xFF000000|color,false,pose.last().pose(),buffer,Font.DisplayMode.NORMAL,0,LightTexture.FULL_BRIGHT);}
    public static net.minecraft.world.phys.AABB getRenderBoundingBox(HostEntity host){
        var bounds=new net.minecraft.world.phys.AABB(host.getBlockPos());
        for(Part p:host.parts.values())if(p.kind==Kind.LARGE_DISPLAY&&p.canvasColumn==0&&p.canvasRow==0){
            var end=host.getBlockPos().relative(DisplayNetworks.right(p),p.canvasWidth-1).relative(DisplayNetworks.up(p).getOpposite(),p.canvasHeight-1);
            bounds=bounds.minmax(new net.minecraft.world.phys.AABB(end));
        }
        // Projected text extends outside the projector base; include both view faces in frustum bounds.
        for(Part p:host.parts.values())if(p.hologram()){
            var centre=net.foundations.pl4.core.HologramProjection.centre(p.face.ordinal(),p.hologramView,p.kind==Kind.ADVANCED_HOLOGRAM);
            var origin=host.getBlockPos();
            bounds=bounds.minmax(new net.minecraft.world.phys.AABB(origin.getX()+centre.x()-.55,origin.getY()+centre.y()-.35,origin.getZ()+centre.z()-.55,
                origin.getX()+centre.x()+.55,origin.getY()+centre.y()+.35,origin.getZ()+centre.z()+.55));
        }
        // Left controls are 17 logical pixels outside the canvas. Include them at the largest
        // supported scale while editing, without expanding every unloaded/closed display scan.
        var editor=DisplayEditorScreen.active();
        if(editor!=null&&host.parts.values().stream().anyMatch(p->p.kind.display()&&editor.matches(host,p)))return bounds.inflate(1.25);
        return bounds.inflate(.063);
    }
    @Override public int getViewDistance(){return 64;}
}
