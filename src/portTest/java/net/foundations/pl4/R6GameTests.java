package net.foundations.pl4;

import java.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Direction;
import net.foundations.pl4.compat.scenarios.GameTest;
import net.foundations.pl4.compat.scenarios.GameTestHelper;
import net.minecraft.block.Blocks;
import net.foundations.pl4.compat.scenarios.PrefixGameTestTemplate;

/** Native acceptance fixtures. Execution requires a full Minecraft/NeoForge test server. */
@PrefixGameTestTemplate(false)
public final class R6GameTests {
    private static final UUID OWNER=UUID.fromString("aaaa0000-0000-0000-0000-000000000006");
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void r6PartSettingsPersist(GameTestHelper h){
        Part display=new Part(Kind.LARGE_DISPLAY,Direction.NORTH,OWNER);display.displayOutward=true;display.label="Power";display.selected="plant";
        display.elements.add(new Part.Element("", "", "storage:eu",10,10,0xFFFFFF,false));
        Part loaded=Part.load(display.save(h.getLevel().registryAccess(),false),h.getLevel().registryAccess());
        net.foundations.pl4.compat.PortAssertions.check(loaded.displayOutward&&loaded.face==Direction.NORTH&&loaded.slot()==display.slot(),"Front change must not change mount or port");
        net.foundations.pl4.compat.PortAssertions.check(loaded.elements.equals(display.elements)&&loaded.selected.equals("plant"),"Retain display layout");
        Part energy=new Part(Kind.ENERGY_READER,Direction.DOWN,OWNER);energy.energySystem="EU";
        net.foundations.pl4.compat.PortAssertions.check(Part.load(energy.save(h.getLevel().registryAccess(),false),h.getLevel().registryAccess()).energySystem.equals("EU"),"Energy system must persist");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void r5SaveDoesNotSilentlyFlip(GameTestHelper h){
        Part old=new Part(Kind.LARGE_DISPLAY,Direction.SOUTH,OWNER);old.label="Keep this layout";
        var tag=old.save(h.getLevel().registryAccess(),false);tag.remove("displayOutward");tag.remove("energySystem");
        var loaded=Part.load(tag,h.getLevel().registryAccess());
        net.foundations.pl4.compat.PortAssertions.check(!loaded.displayOutward&&loaded.energySystem.equals("AUTO")&&loaded.label.equals(old.label),"Legacy saves keep their front/controller; explicit flip is user-controlled");h.succeed();
    }
    private static NetworkEngine.Ref reader(GameTestHelper h){
        BlockPos pos=new BlockPos(2,1,1);h.setBlock(pos,FoundationsPL4.HOST.get());var host=(HostEntity)h.getBlockEntity(pos);
        Part part=new Part(Kind.ENERGY_READER,Direction.WEST,OWNER);host.parts.put(part.slot(),part);host.changed();return new NetworkEngine.Ref(host,part);
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void unsupportedEnergyIsNotEmptyBattery(GameTestHelper h){
        h.setBlock(new BlockPos(1,1,1),Blocks.STONE);var ref=reader(h);
        var rows=DataSampler.sample(h.getLevel().getServer(),ref,List.of(ref.adjacent()),1);
        net.foundations.pl4.compat.PortAssertions.check(rows.isEmpty()&&ref.part().status.contains("No supported energy"),"Missing capability must not synthesize Storage 0 FE");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void energyReaderNeverLoadsRemoteChunk(GameTestHelper h){
        var ref=reader(h);BlockPos far=new BlockPos(1000000,80,1000000);
        net.foundations.pl4.compat.PortAssertions.check(!h.getLevel().hasChunkAt(far),"Fixture must be unloaded");
        var rows=DataSampler.sample(h.getLevel().getServer(),ref,List.of(new Part.Link(h.getLevel().dimension().location().toString(),far,Direction.UP,null,null)),1);
        net.foundations.pl4.compat.PortAssertions.check(rows.isEmpty()&&!h.getLevel().hasChunkAt(far)&&ref.part().status.contains("unloaded"),"Unloaded is not zero energy and must not force-load");h.succeed();
    }
}
