package net.foundations.pl4;

import java.util.List;
import com.electronwill.nightconfig.core.CommentedConfig;
import net.foundations.pl4.compat.scenarios.GameTest;
import net.foundations.pl4.compat.scenarios.GameTestHelper;
import net.foundations.pl4.compat.scenarios.PrefixGameTestTemplate;

/** Actual Forge 35 configuration behavior; compiled only in the disposable server test module. */
@PrefixGameTestTemplate(false)
public final class VersionConfigGameTests {
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void emptyAccountHistorySurvivesNativeCorrection(GameTestHelper h){
        var memory=CommentedConfig.inMemory();PLClientConfig.SPEC.correct(memory);
        Object history=memory.get("communityLinksShownTo");
        if(!(history instanceof List<?> values)||!values.isEmpty()||!PLClientConfig.SPEC.isCorrect(memory))throw new AssertionError("Native Forge config must accept an empty first-launch history");
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void nativeHistoryRejectsMalformedAccountValues(GameTestHelper h){
        String uuid="aaaa0000-0000-0000-0000-000000000001";
        if(!PLClientConfig.validHistory(List.of())||!PLClientConfig.validHistory(List.of(uuid)))throw new AssertionError("Valid account history rejected");
        for(Object invalid:List.of("not-a-list",List.of(123),List.of("bad"),List.of("AAAA0000-0000-0000-0000-000000000001")))if(PLClientConfig.validHistory(invalid))throw new AssertionError("Malformed account history accepted");
        var memory=CommentedConfig.inMemory();PLClientConfig.SPEC.correct(memory);memory.set("communityLinksShownTo",List.of("bad"));
        if(PLClientConfig.SPEC.isCorrect(memory))throw new AssertionError("Native config failed to validate entries");
        PLClientConfig.SPEC.correct(memory);
        if(!((List<?>)memory.get("communityLinksShownTo")).isEmpty())throw new AssertionError("Invalid history did not reset safely");
        memory.set("communityLinksShownTo",List.of(uuid));
        if(!PLClientConfig.SPEC.isCorrect(memory))throw new AssertionError("Native config rejected a saved canonical account UUID");
        h.succeed();
    }
}
