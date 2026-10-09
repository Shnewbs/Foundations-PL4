package net.foundations.pl4;

import java.util.*;
import net.minecraft.core.Direction;
import net.foundations.pl4.compat.scenarios.*;
import net.minecraft.world.item.*;
import net.foundations.pl4.compat.scenarios.PrefixGameTestTemplate;
import net.foundations.pl4.core.*;

/** Native fixtures for display page/save paths. No claims about client item-pose appearance. */
@PrefixGameTestTemplate(false)
public final class R10GameTests {
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void newBlockElementRevealsItsPage(GameTestHelper h){
        var before=new LayoutTransactions.State(List.of(),DisplayElements.Mode.AUTO_LIST,0,0);
        var element=DisplayElements.create(DisplayElements.Type.BLOCK,3);
        var result=LayoutTransactions.apply(before,0,"add",element.id(),element,"");
        net.foundations.pl4.compat.PortAssertions.check(result.accepted()&&result.state().page()==3&&result.state().mode()==DisplayElements.Mode.CUSTOM,"A new graphic must reveal its page and exit automatic view");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void revealedCustomPageSurvivesSave(GameTestHelper h){
        Part p=new Part(Kind.DISPLAY,Direction.NORTH,UUID.randomUUID());p.displayMode=DisplayElements.Mode.CUSTOM;p.displayPage=5;
        p.elements.add(new Part.Element(DisplayElements.create(DisplayElements.Type.ITEM,5)));
        Part restored=Part.load(p.save(h.getLevel().registryAccess(),false),h.getLevel().registryAccess());
        net.foundations.pl4.compat.PortAssertions.check(restored!=null&&restored.displayPage==5&&restored.displayMode==DisplayElements.Mode.CUSTOM&&restored.elements.get(0).spec().page()==5,"Native save keeps the revealed custom page");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void realStoneStillUsesBlockPicture(GameTestHelper h){
        VisualSamples samples=new VisualSamples();samples.item(new ItemStack(Items.STONE,22));
        var rows=samples.rows(h.getLevel().registryAccess(),true,128,0);
        var scene=DisplayElements.plan(DisplayElements.create(DisplayElements.Type.BLOCK,0),rows);
        net.foundations.pl4.compat.PortAssertions.check(scene.draws().stream().anyMatch(d->d instanceof DisplayElements.Icon icon&&icon.block()),"Block presentation cannot silently become text list");
        net.foundations.pl4.compat.PortAssertions.check(scene.draws().stream().anyMatch(d->d instanceof DisplayElements.Text text&&text.overlay()&&text.value().equals("22")),"Quantity remains independent of picture stack size");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void restingAutoModeDoesNotDiscardTypedElements(GameTestHelper h){
        Part p=new Part(Kind.LARGE_DISPLAY,Direction.NORTH,UUID.randomUUID());p.displayMode=DisplayElements.Mode.AUTO_LIST;
        p.elements.add(new Part.Element(DisplayElements.create(DisplayElements.Type.BLOCK,2)));
        Part restored=Part.load(p.save(h.getLevel().registryAccess(),false),h.getLevel().registryAccess());
        net.foundations.pl4.compat.PortAssertions.check(restored!=null&&restored.elements.size()==1&&restored.elements.get(0).spec().type()==DisplayElements.Type.BLOCK,"Switching view retains layout");
        net.foundations.pl4.compat.PortAssertions.check(!MonitorPresentation.automatic(restored.displayMode,true)&&MonitorPresentation.automatic(restored.displayMode,false),"Editor preview is independent of saved resting view");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void stalePageChangeLeavesSavedLayoutIntact(GameTestHelper h){
        var element=DisplayElements.create(DisplayElements.Type.BLOCK,4);
        var before=new LayoutTransactions.State(List.of(element),DisplayElements.Mode.CUSTOM,4,2);
        var rejected=LayoutTransactions.apply(before,1,"update",element.id(),element.onPage(1),"");
        net.foundations.pl4.compat.PortAssertions.check(!rejected.accepted()&&rejected.state().equals(before),"Rejected older properties do not reveal wrong page or change the layout");h.succeed();
    }
}
