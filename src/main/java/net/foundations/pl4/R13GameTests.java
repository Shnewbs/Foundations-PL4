package net.foundations.pl4;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;


/** Native item-form persistence fixtures for the final 0.0.1a cleanup. */

public final class R13GameTests {
    @PortGameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void ordinaryBrokenPartsStackWithoutRuntimeNbt(GameTestHelper h){
        Part a=new Part(Kind.DATA_CABLE,Direction.NORTH,UUID.randomUUID()),b=new Part(Kind.DATA_CABLE,Direction.SOUTH,UUID.randomUUID());
        a.signal=15;a.ticks=812;a.layoutRevision=99;b.signal=3;b.ticks=2;b.layoutRevision=7;
        ItemStack sa=PartItem.stack(a,h.getLevel().registryAccess()),sb=PartItem.stack(b,h.getLevel().registryAccess());
        h.assertTrue(sa.get(DataComponents.CUSTOM_DATA)==null&&sb.get(DataComponents.CUSTOM_DATA)==null,"Ordinary broken parts must not carry unique runtime CustomData");
        h.assertTrue(ItemStack.isSameItemSameComponents(sa,sb),"Identical ordinary drops must stack");h.succeed();
    }
    @PortGameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void operatorSavedPartsStripRuntimeIdentityButKeepConfig(GameTestHelper h){
        Part a=new Part(Kind.INVENTORY_READER,Direction.NORTH,UUID.randomUUID()),b=new Part(Kind.INVENTORY_READER,Direction.SOUTH,UUID.randomUUID());
        a.label=b.label="warehouse";a.filter=b.filter="minecraft:iron_ingot";a.identity=UUID.randomUUID();b.identity=UUID.randomUUID();a.signal=15;b.signal=2;a.ticks=99;b.ticks=1000;a.layoutRevision=10;b.layoutRevision=44;
        ItemStack sa=PartItem.savedStack(a,h.getLevel().registryAccess()),sb=PartItem.savedStack(b,h.getLevel().registryAccess());
        var data=sa.get(DataComponents.CUSTOM_DATA);h.assertTrue(data!=null&&data.contains("pl_part"),"Operator removal retains a saved configuration");var tag=data.copyTag().getCompound("pl_part").orElseGet(CompoundTag::new);
        h.assertTrue(!tag.read("identity",net.minecraft.core.UUIDUtil.CODEC).isPresent()&&!tag.read("owner",net.minecraft.core.UUIDUtil.CODEC).isPresent()&&!tag.contains("signal")&&!tag.contains("ticks")&&!tag.contains("layoutRevision"),"Saved item strips runtime-only fields");
        h.assertTrue(ItemStack.isSameItemSameComponents(sa,sb),"Equivalent saved configurations stack despite different runtime identity/orientation");h.succeed();
    }
    @PortGameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void ordinaryTransferDropStillRetainsEscrow(GameTestHelper h){
        Part p=new Part(Kind.TRANSFER_NODE,Direction.WEST,UUID.randomUUID());p.pendingItem=new ItemStack(Items.DIAMOND,17);p.pendingFluid=new FluidStack(Fluids.WATER,500);p.pendingEnergy=1200;
        ItemStack stack=PartItem.stack(p,h.getLevel().registryAccess());var data=stack.get(DataComponents.CUSTOM_DATA);h.assertTrue(data!=null&&data.contains("pl_part"),"Escrow-bearing normal drop must retain a payload");
        Part restored=Part.load(data.copyTag().getCompound("pl_part").orElseGet(CompoundTag::new),h.getLevel().registryAccess());h.assertTrue(restored.pendingItem.getCount()==17&&restored.pendingFluid.getAmount()==500&&restored.pendingEnergy==1200,"Escrow must survive canonical item payload");h.succeed();
    }
}
