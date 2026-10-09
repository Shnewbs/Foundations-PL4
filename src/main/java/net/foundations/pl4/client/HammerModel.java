package net.foundations.pl4.client;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.foundations.pl4.core.HammerGeometry;
/** Same model boxes, UVs and animation rules on the native pre-layer model API. */
public final class HammerModel {
 private final java.util.Map<String,ModelRenderer> parts=new java.util.LinkedHashMap<>();
 public HammerModel(){for(var piece:HammerGeometry.PIECES){
  ModelRenderer part=new ModelRenderer(HammerGeometry.TEXTURE_WIDTH,HammerGeometry.TEXTURE_HEIGHT,piece.u(),piece.v());
  part.addBox(piece.x(),piece.y(),piece.z(),piece.width(),piece.height(),piece.depth());part.setPos(piece.px(),piece.py(),piece.pz());
  part.xRot=piece.rx();part.yRot=piece.ry();part.zRot=piece.rz();parts.put(piece.name(),part);
 }}
 public void render(MatrixStack pose,IVertexBuilder vertices,int light,int overlay,double progress,boolean assembled){for(var piece:HammerGeometry.PIECES){
  var part=parts.get(piece.name());part.visible=!piece.upper()||assembled;part.y=piece.py()+(piece.moving()?HammerGeometry.TRAVEL*(float)progress:0);part.render(pose,vertices,light,overlay);
 }}
}
