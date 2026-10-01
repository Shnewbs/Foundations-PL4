package net.foundations.pl4.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.foundations.pl4.*;

public final class HostRenderer implements BlockEntityRenderer<HostEntity,HostRenderer.State> {
    public static final class State extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState {RenderCommands commands;}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(HostEntity host,State state,float partial,net.minecraft.world.phys.Vec3 camera,net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay overlay){
        BlockEntityRenderer.super.extractRenderState(host,state,partial,camera,overlay);
        state.commands=new RenderCommands();extract(host,partial,new PoseStack(),state.commands,state.lightCoords,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,net.minecraft.client.renderer.state.level.CameraRenderState camera){state.commands.submit(pose,collector);}
    private static final Direction[] FACES=Direction.values();
    private final DisplayPainter displayPainter=new DisplayPainter();
    private final net.minecraft.world.level.block.state.BlockState[][][] cableStates=new net.minecraft.world.level.block.state.BlockState[3][4][6];
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
    private void extract(HostEntity host,float partial,PoseStack pose,RenderCommands buffer,int light,int overlay){
        Minecraft mc=Minecraft.getInstance();
        for(Part part:host.parts.values()){
            if(part.kind.cable()) {
                int material=part.kind==Kind.DATA_CABLE?0:(part.signal>0?2:1);
                buffer.block(cableStates[material][0][0],pose,light,overlay);
                for(Direction d:FACES) {
                    int type=host.connection(d);if(type==0)continue;
                    buffer.block(cableStates[material][type][d.ordinal()],pose,light,overlay);
                }
            } else if(part.kind==Kind.LARGE_DISPLAY){
                var model=largeStates[part.displayOutward?1:0][part.face.ordinal()][part.canvasMask];
                buffer.block(model,pose,light,overlay);
            } else {
                pose.pushPose();
                if(part.hologram()){
                    int yaw=net.foundations.pl4.core.HologramProjection.baseYaw(part.face.ordinal(),part.hologramView);
                    pose.translate(.5,.5,.5);pose.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(yaw)));pose.translate(-.5,-.5,-.5);
                }
                var model=partStates.get(part.kind)[part.kind.reader()?(host.readerHasDisplay(part)?1:0):(part.displayOutward?1:0)][part.face.ordinal()];
                buffer.block(model,pose,light,overlay);
                pose.popPose();
            }
            if(host.externalLead(part)){
                int material=part.kind.redstone()?(part.signal>0?2:1):0;
                buffer.block(leadModels.get(part.kind)[material][part.face.ordinal()],pose,light,overlay);
            }
            if(part.kind.display()&&(part.kind!=Kind.LARGE_DISPLAY||(part.canvasColumn==0&&part.canvasRow==0)))display(host,part,pose,buffer,mc.font);
        }
    }
    private void display(HostEntity host,Part p,PoseStack pose,RenderCommands buffer,Font font){
        var eye=Minecraft.getInstance().gameRenderer.mainCamera().position();
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
        if(frame.rotationX()!=0)pose.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(frame.rotationX())));
        if(frame.rotationY()!=0)pose.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(frame.rotationY())));
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

    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(HostEntity host){
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
