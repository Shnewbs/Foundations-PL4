package net.foundations.pl4.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.foundations.pl4.FoundationsPL4;
import net.foundations.pl4.core.HammerGeometry;

/** Baked original PL2 boxes/UVs. The texture is a model skin, not a block-face atlas. */
public final class HammerModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(FoundationsPL4.id("hammer"),"main");
    private final ModelPart root;
    public HammerModel(ModelPart root){this.root=root;}
    public static LayerDefinition layer(){
        MeshDefinition mesh=new MeshDefinition();
        for(var p:HammerGeometry.PIECES)mesh.getRoot().addOrReplaceChild(p.name(),
            CubeListBuilder.create().texOffs(p.u(),p.v()).addBox(p.x(),p.y(),p.z(),p.width(),p.height(),p.depth()),
            PartPose.offsetAndRotation(p.px(),p.py(),p.pz(),p.rx(),p.ry(),p.rz()));
        return LayerDefinition.create(mesh,HammerGeometry.TEXTURE_WIDTH,HammerGeometry.TEXTURE_HEIGHT);
    }
    public void render(PoseStack pose,VertexConsumer vertices,int light,int overlay,double progress,boolean assembled){
        for(var piece:HammerGeometry.PIECES){
            ModelPart part=root.getChild(piece.name());
            part.visible=!piece.upper()||assembled;
            part.y=piece.py()+(piece.moving()?HammerGeometry.TRAVEL*(float)progress:0);
        }
        root.render(pose,vertices,light,overlay);
    }
}
