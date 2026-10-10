package net.foundations.pl4;

import java.util.*;
import net.minecraft.util.EnumFacing;
import net.foundations.pl4.compat.scenarios.GameTest;
import net.foundations.pl4.compat.scenarios.GameTestHelper;
import net.foundations.pl4.compat.scenarios.PrefixGameTestTemplate;
import net.foundations.pl4.core.*;

/** Native fixtures for R11 logical-canvas persistence/migration. Client rendering still requires graphical acceptance. */
@PrefixGameTestTemplate(false)
public final class R11GameTests {
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void joinedCanvasGetsPhysicalAspect(GameTestHelper h){
        var s=DynamicCanvasLayout.large(6,2);double ratio=(double)s.width()/s.height();
        net.foundations.pl4.compat.PortAssertions.check(Math.abs(ratio-(5.88/1.88))/(5.88/1.88)<.004,"6x2 logical canvas follows whole joined surface");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void legacyLayoutMigratesProportionally(GameTestHelper h){
        var target=DynamicCanvasLayout.large(6,2);var r=DynamicCanvasLayout.migrate(new DisplayElements.Rect(62,30,124,60),248,120,target.width(),target.height());
        net.foundations.pl4.compat.PortAssertions.check(Math.abs((r.x()+r.width()/2.0)/target.width()-.5)<.02&&Math.abs((r.y()+r.height()/2.0)/target.height()-.5)<.02,"Migration keeps centre-relative placement");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void logicalSpacePersists(GameTestHelper h){
        Part p=new Part(Kind.LARGE_DISPLAY,EnumFacing.NORTH,UUID.randomUUID());p.layoutWidth=720;p.layoutHeight=240;p.displayMode=DisplayElements.Mode.CUSTOM;p.elements.add(new Part.Element(DisplayElements.create(DisplayElements.Type.BLOCK,0,720,240)));
        Part restored=Part.load(p.save(null,false),null);
        net.foundations.pl4.compat.PortAssertions.check(restored!=null&&restored.layoutWidth==720&&restored.layoutHeight==240,"Dynamic logical dimensions survive save/load");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void editorMoveUsesExpandedBounds(GameTestHelper h){
        var e=DisplayElements.create(DisplayElements.Type.ITEM,0,900,300);var moved=DisplayElements.move(e,850,260,false,false,900,300);
        net.foundations.pl4.compat.PortAssertions.check(moved.bounds().right()<=900&&moved.bounds().bottom()<=300&&moved.bounds().x()>248,"Joined canvas editor can use space beyond legacy width");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void resizeCannotLeaveDynamicCanvas(GameTestHelper h){
        var e=DisplayElements.create(DisplayElements.Type.INVENTORY,0,900,300).bounds(new DisplayElements.Rect(700,200,100,60));var resized=DisplayElements.move(e,1000,1000,true,false,900,300);
        net.foundations.pl4.compat.PortAssertions.check(resized.bounds().right()<=900&&resized.bounds().bottom()<=300,"Resize remains inside expanded canvas");h.succeed();
    }
}
