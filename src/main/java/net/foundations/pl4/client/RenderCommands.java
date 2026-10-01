package net.foundations.pl4.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

/** Snapshot draw commands during extraction; submission never reads a live block entity. */
final class RenderCommands {
    private final List<BiConsumer<PoseStack,SubmitNodeCollector>> commands=new ArrayList<>();
    void add(PoseStack pose,BiConsumer<PoseStack,SubmitNodeCollector> draw){
        Matrix4f transform=new Matrix4f(pose.last().pose());
        commands.add((root,collector)->{root.pushPose();try{root.mulPose(transform);draw.accept(root,collector);}finally{root.popPose();}});
    }
    void block(BlockState state,PoseStack pose,int light,int overlay){
        var model=new BlockModelRenderState();
        Minecraft.getInstance().getBlockModelResolver().update(model,state,BlockDisplayContext.create());
        add(pose,(p,c)->model.submitMultiLayer(p,c,light,overlay,0));
    }
    void item(ItemStack stack,ItemDisplayContext context,PoseStack pose,int light,int overlay){
        var model=new ItemStackRenderState();var mc=Minecraft.getInstance();
        mc.getItemModelResolver().updateForTopItem(model,stack.copy(),context,mc.level,null,0);
        add(pose,(p,c)->model.submit(p,c,light,overlay,0));
    }
    void submit(PoseStack pose,SubmitNodeCollector collector){for(var command:commands)command.accept(pose,collector);}
}
