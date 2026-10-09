"""Reproduce the 1.19.2 source port from a pinned native Forge 1.20.1 source tree.
PL4_PORT_WORK/base must contain that source, not the NeoForge baseline.
"""
from pathlib import Path
import os,shutil,re,subprocess
W=Path(os.environ['PL4_PORT_WORK']).resolve();R=W/'1.19.2';J=R/'src/main/java/net/foundations/pl4'
if R.exists():raise SystemExit('Refusing to overwrite an existing migration workspace')
shutil.copytree(W/'base',R)
def write(n,s):
 p=J/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf-8')
def patch(n,a,b):
 p=J/n;s=p.read_text();assert a in s,(n,a);p.write_text(s.replace(a,b),encoding='utf-8')
keys=set()
for p in J.rglob('*.java'):
 s=p.read_text();keys.update(re.findall(r'\bRegistries\.([A-Z_]+)',s))
 s=s.replace('net.minecraft.core.registries.BuiltInRegistries','net.minecraft.core.Registry').replace('BuiltInRegistries.','Registry.')
 s=s.replace('net.minecraft.core.registries.Registries','net.foundations.pl4.compat.Registries')
 s=s.replace('import net.minecraft.core.registries.*;','import net.minecraft.core.Registry;\nimport net.foundations.pl4.compat.Registries;')
 s=s.replace('net.minecraft.world.level.material.MapColor','net.minecraft.world.level.material.MaterialColor').replace('MapColor.','MaterialColor.')
 s=s.replace('import net.minecraft.world.flag.FeatureFlags;','').replace(',FeatureFlags.DEFAULT_FLAGS','')
 s=s.replace('net.minecraft.client.gui.GuiGraphics','net.foundations.pl4.compat.GuiGraphics')
 for name in ['Button','EditBox','Tooltip']:s=s.replace('net.minecraft.client.gui.components.'+name,'net.foundations.pl4.compat.'+name)
 if 'import net.minecraft.client.gui.components.*;' in s:s=s.replace('import net.minecraft.client.gui.components.*;','import net.minecraft.client.gui.components.*;\nimport net.foundations.pl4.compat.Button;\nimport net.foundations.pl4.compat.EditBox;\nimport net.foundations.pl4.compat.Tooltip;')
 s=s.replace('com.mojang.math.Axis','com.mojang.math.Vector3f').replace('Axis.XP','Vector3f.XP').replace('Axis.YP','Vector3f.YP').replace('Axis.ZP','Vector3f.ZP')
 s=s.replace('org.joml.Matrix4f','com.mojang.math.Matrix4f')
 s=s.replace('net.minecraft.world.item.ItemDisplayContext','net.minecraft.client.renderer.block.model.ItemTransforms.TransformType').replace('ItemDisplayContext.','TransformType.')
 s=s.replace('Font.DisplayMode.NORMAL','false')
 if 'GuiGraphics' in s and p.parent.name=='client' and 'import net.foundations.pl4.compat.GuiGraphics;' not in s:s=s.replace('package net.foundations.pl4.client;','package net.foundations.pl4.client;\nimport net.foundations.pl4.compat.GuiGraphics;')
 if 'TransformType.' in s and 'import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;' not in s:s=s.replace('package net.foundations.pl4.client;','package net.foundations.pl4.client;\nimport net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;')
 s=s.replace('pose,buffers,mc.level,0','pose,buffers,0').replace('pose,buffer,hammer.getLevel(),hammer.getBlockPos().hashCode()','pose,buffer,hammer.getBlockPos().hashCode()')
 s=s.replace('player.serverLevel()','player.getLevel()').replace('player.level()','player.level').replace('entity.level()','entity.level')
 s=s.replace('net.minecraft.core.RegistryAccess.EMPTY','null')
 s=s.replace('h.assertTrue(','net.foundations.pl4.compat.PortAssertions.check(')
 s=s.replace('BlockBehaviour.Properties.of()','BlockBehaviour.Properties.of(net.minecraft.world.level.material.Material.STONE)')
 s=s.replace('.pushReaction(PushReaction.BLOCK)','')
 s=s.replace('.canBeReplaced()','.getMaterial().isReplaceable()')
 s=re.sub(r'Properties\.of\(net.minecraft.world.level.material.Material.STONE\)\.mapColor\((MaterialColor\.\w+)\)',r'Properties.of(net.minecraft.world.level.material.Material.STONE,\1)',s)
 p.write_text(s,encoding='utf-8')
keys.discard('CREATIVE_MODE_TAB')
types={'BLOCK':'net.minecraft.world.level.block.Block','ITEM':'net.minecraft.world.item.Item','FLUID':'net.minecraft.world.level.material.Fluid','BLOCK_ENTITY_TYPE':'net.minecraft.world.level.block.entity.BlockEntityType<?>','RECIPE_TYPE':'net.minecraft.world.item.crafting.RecipeType<?>','RECIPE_SERIALIZER':'net.minecraft.world.item.crafting.RecipeSerializer<?>','MENU':'net.minecraft.world.inventory.MenuType<?>','DIMENSION':'net.minecraft.world.level.Level'}
assert keys.issubset(types),keys-set(types)
write('compat/Registries.java','package net.foundations.pl4.compat;\npublic final class Registries {\n'+''.join(f' public static final net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<{types[k]}>> {k}=net.minecraft.core.Registry.{k}_REGISTRY;\n' for k in sorted(keys))+' private Registries(){}\n}\n')
write('compat/PortAssertions.java','''package net.foundations.pl4.compat;
/** Identical assertion semantics for native GameTestHelper versions without assertTrue. */
public final class PortAssertions {
 public static void check(boolean value,String message){if(!value)throw new net.minecraft.gametest.framework.GameTestAssertException(message);}
 private PortAssertions(){}
}
''')
write('compat/PortScreen.java','''package net.foundations.pl4.compat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public abstract class PortScreen extends Screen {
 protected PortScreen(Component title){super(title);}
 public void renderBackground(GuiGraphics g,int x,int y,float partial){super.renderBackground(g.pose());}
 public void render(GuiGraphics g,int x,int y,float partial){renderBackground(g,x,y,partial);super.render(g.pose(),x,y,partial);}
 @Override public final void render(PoseStack pose,int x,int y,float partial){render(new GuiGraphics(pose),x,y,partial);}
 public boolean mouseScrolled(double x,double y,double horizontal,double vertical){return super.mouseScrolled(x,y,vertical);}
 @Override public boolean mouseScrolled(double x,double y,double vertical){return mouseScrolled(x,y,0,vertical);}
}
''')
write('compat/Tooltip.java','''package net.foundations.pl4.compat;
import net.minecraft.network.chat.Component;
public record Tooltip(Component text){public static Tooltip create(Component text){return new Tooltip(text);}}
''')
write('compat/Button.java','''package net.foundations.pl4.compat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
public class Button extends net.minecraft.client.gui.components.Button {
 public interface OnPress {void onPress(Button button);}
 public static final Object DEFAULT_NARRATION=new Object();
 private Tooltip tooltip;
 public Button(int x,int y,int width,int height,Component message,OnPress press,Object narration){super(x,y,width,height,message,b->press.onPress((Button)b));}
 public static Builder builder(Component message,OnPress press){return new Builder(message,press);}
 public int getX(){return x;} public int getY(){return y;} public void setX(int value){x=value;} public void setY(int value){y=value;}
 public void setTooltip(Tooltip value){tooltip=value;}
 @Override public void renderButton(PoseStack pose,int x,int y,float partial){renderWidget(new GuiGraphics(pose),x,y,partial);if(tooltip!=null&&isMouseOver(x,y))new GuiGraphics(pose).renderTooltip(Minecraft.getInstance().font,tooltip.text(),x,y);}
 protected void renderWidget(GuiGraphics graphics,int x,int y,float partial){super.renderButton(graphics.pose(),x,y,partial);}
 public static final class Builder {
  private final Component message;private final OnPress press;private int x,y,w=150,h=20;private Tooltip tip;
  Builder(Component message,OnPress press){this.message=message;this.press=press;}
  public Builder bounds(int x,int y,int w,int h){this.x=x;this.y=y;this.w=w;this.h=h;return this;}
  public Builder tooltip(Tooltip value){tip=value;return this;}
  public Button build(){Button b=new Button(x,y,w,h,message,press,DEFAULT_NARRATION);b.setTooltip(tip);return b;}
 }
}
''')
write('compat/EditBox.java','''package net.foundations.pl4.compat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
public class EditBox extends net.minecraft.client.gui.components.EditBox {
 private Tooltip tooltip;public void setTooltip(Tooltip tip){tooltip=tip;}
 public EditBox(Font font,int x,int y,int width,int height,Component message){super(font,x,y,width,height,message);}
 public int getX(){return x;}public int getY(){return y;}public void setX(int value){x=value;}public void setY(int value){y=value;}
 @Override public void renderButton(PoseStack pose,int x,int y,float partial){renderWidget(new GuiGraphics(pose),x,y,partial);if(tooltip!=null&&isMouseOver(x,y))new GuiGraphics(pose).renderTooltip(net.minecraft.client.Minecraft.getInstance().font,tooltip.text(),x,y);}
 public void renderWidget(GuiGraphics g,int x,int y,float partial){super.renderButton(g.pose(),x,y,partial);}
}
''')
write('compat/GuiGraphics.java','''package net.foundations.pl4.compat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
/** Native PoseStack-backed GUI bridge; no replacement Minecraft classes are injected. */
public final class GuiGraphics {
 private final PoseStack pose;private final Minecraft mc=Minecraft.getInstance();
 private final java.util.Deque<int[]> clips=new java.util.ArrayDeque<>();
 public GuiGraphics(PoseStack pose){this.pose=pose;}
 public PoseStack pose(){return pose;}
 public void fill(int x,int y,int right,int bottom,int color){GuiComponent.fill(pose,x,y,right,bottom,color);}
 public void renderOutline(int x,int y,int w,int h,int color){fill(x,y,x+w,y+1,color);fill(x,y+h-1,x+w,y+h,color);fill(x,y,x+1,y+h,color);fill(x+w-1,y,x+w,y+h,color);}
 public int drawString(Font font,String text,int x,int y,int color,boolean shadow){return shadow?font.drawShadow(pose,text,x,y,color):font.draw(pose,text,x,y,color);}
 public int drawString(Font font,Component text,int x,int y,int color,boolean shadow){return drawString(font,text.getVisualOrderText(),x,y,color,shadow);}
 public int drawString(Font font,FormattedCharSequence text,int x,int y,int color,boolean shadow){return shadow?font.drawShadow(pose,text,x,y,color):font.draw(pose,text,x,y,color);}
 public void blit(ResourceLocation texture,int x,int y,int u,int v,int w,int h){RenderSystem.setShaderTexture(0,texture);GuiComponent.blit(pose,x,y,u,v,w,h,256,256);}
 public void renderTooltip(Font font,Component text,int x,int y){if(mc.screen!=null)mc.screen.renderTooltip(pose,text,x,y);}
 public void renderItem(ItemStack stack,int x,int y){
  var model=RenderSystem.getModelViewStack();model.pushPose();model.mulPoseMatrix(pose.last().pose());RenderSystem.applyModelViewMatrix();
  try{mc.getItemRenderer().renderAndDecorateFakeItem(stack,x,y);}finally{model.popPose();RenderSystem.applyModelViewMatrix();}
 }
 public void enableScissor(int x,int y,int right,int bottom){if(!clips.isEmpty()){int[] p=clips.peek();x=Math.max(x,p[0]);y=Math.max(y,p[1]);right=Math.min(right,p[2]);bottom=Math.min(bottom,p[3]);}clips.push(new int[]{x,y,Math.max(x,right),Math.max(y,bottom)});applyClip();}
 public void disableScissor(){if(!clips.isEmpty())clips.pop();applyClip();}
 private void applyClip(){if(clips.isEmpty()){RenderSystem.disableScissor();return;}int[] r=clips.peek();double s=mc.getWindow().getGuiScale();RenderSystem.enableScissor((int)(r[0]*s),(int)(mc.getWindow().getHeight()-r[3]*s),(int)((r[2]-r[0])*s),(int)((r[3]-r[1])*s));}
}
''')
write('compat/PortContainerScreen.java','''package net.foundations.pl4.compat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
public abstract class PortContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
 protected PortContainerScreen(T menu,Inventory inventory,Component title){super(menu,inventory,title);}
 @Override protected final void renderBg(PoseStack pose,float partial,int x,int y){renderBg(new GuiGraphics(pose),partial,x,y);}
 protected abstract void renderBg(GuiGraphics g,float partial,int x,int y);
 @Override protected final void renderLabels(PoseStack pose,int x,int y){renderLabels(new GuiGraphics(pose),x,y);}
 protected void renderLabels(GuiGraphics g,int x,int y){super.renderLabels(g.pose(),x,y);}
 @Override public final void render(PoseStack pose,int x,int y,float partial){render(new GuiGraphics(pose),x,y,partial);}
 public void render(GuiGraphics g,int x,int y,float partial){super.render(g.pose(),x,y,partial);}
 public void renderBackground(GuiGraphics g){super.renderBackground(g.pose());}
 protected void renderTooltip(GuiGraphics g,int x,int y){super.renderTooltip(g.pose(),x,y);}
}
''')
patch('client/HammerScreen.java','extends AbstractContainerScreen<HammerMenu>','extends net.foundations.pl4.compat.PortContainerScreen<HammerMenu>')
patch('client/DisplayEditorScreen.java','var matrix=new Matrix4f(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix()).mul(localPose);','var matrix=new Matrix4f(RenderSystem.getProjectionMatrix());matrix.multiply(RenderSystem.getModelViewMatrix());matrix.multiply(localPose);')
patch('client/DisplayEditorScreen.java','float[] values=new float[16];matrix.get(values);','float[] values=new float[16];var buffer=java.nio.FloatBuffer.wrap(values);matrix.store(buffer);')
for n in ['client/DisplayPropertiesScreen.java','client/PartScreen.java']:
 p=J/n;s=p.read_text();s=re.sub(r'\b(widget|b)\.setY\(([^;]*)\);',r'\1.y=\2;',s);s=s.replace('widget.getY()','widget.y').replace('b.getY()','b.y');p.write_text(s)
p=J/'FoundationsPL4.java';s=p.read_text();s=re.sub(r'    public static final DeferredRegister<CreativeModeTab> TABS =[^\n]+\n','''    public static final CreativeModeTab TAB=new CreativeModeTab("foundations_pl4"){
        @Override public ItemStack makeIcon(){return new ItemStack(item("sapphire"));}
        @Override public void fillItemList(net.minecraft.core.NonNullList<ItemStack> items){ITEMS.getEntries().forEach(entry->items.add(new ItemStack(entry.get())));}
    };
''',s);s=re.sub(r'        TABS.register\("main",[^\n]+\n','',s);s=s.replace(' TABS.register(bus);','');p.write_text(s)
for n in ['HammerBlock.java','HammerSpaceBlock.java']:
 p=J/n;s=p.read_text();s=s.rstrip()[:-1]+'''    @Override public net.minecraft.world.level.material.PushReaction getPistonPushReaction(BlockState state){return net.minecraft.world.level.material.PushReaction.BLOCK;}
}\n''';p.write_text(s)
p=J/'core/ForgingRecipe.java';s=p.read_text();s=s.replace('@Override public ItemStack assemble(SingleRecipeInput input,RegistryAccess registries)','public ItemStack assemble(SingleRecipeInput input,RegistryAccess registries)').replace('@Override public ItemStack getResultItem(RegistryAccess registries)','public ItemStack getResultItem(RegistryAccess registries)');s=s.replace(' public ForgingRecipe value(){return this;}',' @Override public ItemStack assemble(SingleRecipeInput input){return result.copy();}\n @Override public ItemStack getResultItem(){return result.copy();}\n public ForgingRecipe value(){return this;}');s=s.replace('Ingredient.fromJson(dynamic.convert(JsonOps.INSTANCE).getValue(),false)','Ingredient.fromJson(dynamic.convert(JsonOps.INSTANCE).getValue())').replace('DataResult.error(bad::getMessage)','DataResult.error(bad.getMessage())').replace('Ingredient.fromJson(json.get("ingredient"),false)','Ingredient.fromJson(json.get("ingredient"))');p.write_text(s)
patch('client/GuideScreen.java','getLanguageManager().getSelected()','getLanguageManager().getSelected().getCode()')
patch('BuiltinInfoProvider.java','crop.getAge(state)','state.getValue(crop.getAgeProperty())')
patch('PLClientConfig.java','defineListAllowEmpty("communityLinksShownTo",List.<String>of(),','defineListAllowEmpty(List.of("communityLinksShownTo"),()->List.<String>of(),')
# AST source ranges preserve nested receivers and operator precedence.
a=Path(__file__).with_name('RewriteCalls.java').read_text();a=a.replace('public class RewriteCalls','public class Rewrite119').replace('case "getLast" ->','case "getCenter" -> {if(a.isEmpty())replacement="net.minecraft.world.phys.Vec3.atCenterOf("+receiver+")";}\n                                    case "copyWithCount" -> {replacement=DATA+".copyWithCount("+receiver+","+String.join(",",a)+")";}\n                                    case "getLast" ->')
(W/'Rewrite119.java').write_text(a)
subprocess.run(['javac','-d',str(W),str(W/'Rewrite119.java')],check=True);subprocess.run(['java','-cp',str(W),'Rewrite119',str(J)],check=True)
p=J/'compat/PortData.java';s=p.read_text();s=s.replace(' private PortData(){}',' public static ItemStack copyWithCount(ItemStack stack,int count){ItemStack copy=stack.copy();copy.setCount(count);return copy;}\n private PortData(){}');p.write_text(s)
print('Native 1.19.2 source port applied; runtime acceptance is a separate gate')
