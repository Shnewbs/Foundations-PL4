package net.foundations.pl4;

import java.util.UUID;
import net.minecraft.core.Direction;
import net.foundations.pl4.compat.DataComponents;
import net.foundations.pl4.compat.scenarios.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.foundations.pl4.compat.scenarios.PrefixGameTestTemplate;

/** Native item-form persistence fixtures for the final 0.0.1a cleanup. */
@PrefixGameTestTemplate(false)
public final class R13GameTests {
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void ordinaryBrokenPartsStackWithoutRuntimeNbt(GameTestHelper h){
        Part a=new Part(Kind.DATA_CABLE,Direction.NORTH,UUID.randomUUID()),b=new Part(Kind.DATA_CABLE,Direction.SOUTH,UUID.randomUUID());
        a.signal=15;a.ticks=812;a.layoutRevision=99;b.signal=3;b.ticks=2;b.layoutRevision=7;
        ItemStack sa=PartItem.stack(a,h.getLevel().registryAccess()),sb=PartItem.stack(b,h.getLevel().registryAccess());
        net.foundations.pl4.compat.PortAssertions.check(net.foundations.pl4.compat.PortData.get(sa,DataComponents.CUSTOM_DATA)==null&&net.foundations.pl4.compat.PortData.get(sb,DataComponents.CUSTOM_DATA)==null,"Ordinary broken parts must not carry unique runtime CustomData");
        net.foundations.pl4.compat.PortAssertions.check(ItemStack.isSameItemSameTags(sa,sb),"Identical ordinary drops must stack");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void operatorSavedPartsStripRuntimeIdentityButKeepConfig(GameTestHelper h){
        Part a=new Part(Kind.INVENTORY_READER,Direction.NORTH,UUID.randomUUID()),b=new Part(Kind.INVENTORY_READER,Direction.SOUTH,UUID.randomUUID());
        a.label=b.label="warehouse";a.filter=b.filter="minecraft:iron_ingot";a.identity=UUID.randomUUID();b.identity=UUID.randomUUID();a.signal=15;b.signal=2;a.ticks=99;b.ticks=1000;a.layoutRevision=10;b.layoutRevision=44;
        ItemStack sa=PartItem.savedStack(a,h.getLevel().registryAccess()),sb=PartItem.savedStack(b,h.getLevel().registryAccess());
        var data=net.foundations.pl4.compat.PortData.get(sa,DataComponents.CUSTOM_DATA);net.foundations.pl4.compat.PortAssertions.check(data!=null&&data.contains("pl_part"),"Operator removal retains a saved configuration");var tag=data.copyTag().getCompound("pl_part");
        net.foundations.pl4.compat.PortAssertions.check(!tag.hasUUID("identity")&&!tag.hasUUID("owner")&&!tag.contains("signal")&&!tag.contains("ticks")&&!tag.contains("layoutRevision"),"Saved item strips runtime-only fields");
        net.foundations.pl4.compat.PortAssertions.check(ItemStack.isSameItemSameTags(sa,sb),"Equivalent saved configurations stack despite different runtime identity/orientation");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void ordinaryTransferDropStillRetainsEscrow(GameTestHelper h){
        Part p=new Part(Kind.TRANSFER_NODE,Direction.WEST,UUID.randomUUID());p.pendingItem=new ItemStack(Items.DIAMOND,17);p.pendingFluid=new FluidStack(Fluids.WATER,500);p.pendingEnergy=1200;
        ItemStack stack=PartItem.stack(p,h.getLevel().registryAccess());var data=net.foundations.pl4.compat.PortData.get(stack,DataComponents.CUSTOM_DATA);net.foundations.pl4.compat.PortAssertions.check(data!=null&&data.contains("pl_part"),"Escrow-bearing normal drop must retain a payload");
        Part restored=Part.load(data.copyTag().getCompound("pl_part"),h.getLevel().registryAccess());net.foundations.pl4.compat.PortAssertions.check(restored.pendingItem.getCount()==17&&restored.pendingFluid.getAmount()==500&&restored.pendingEnergy==1200,"Escrow must survive canonical item payload");h.succeed();
    }
}
