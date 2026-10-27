package net.foundations.pl4.legacy.tests;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.foundations.pl4.legacy.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.entity.item.EntityItem;
import java.io.FileWriter;
import java.util.*;
/** Separate disposable decode worlds; never packaged into production. */
@Mod(modid="foundations_pl4_decodetests",name="PL4 decode restart acceptance",version="1",dependencies="required-after:foundations_pl4")
public final class DecodeRestartScenarios {
    private WorldServer world;private final List<String> cases=new ArrayList<String>(),tokens=new ArrayList<String>();
    private final List<Map<String,Object>> outcomes=new ArrayList<Map<String,Object>>();
    private static final String OWNER="fae1b877-c6b4-4538-a26a-630190b54205";
    private String suite;
    @Mod.EventHandler public void started(FMLServerStartedEvent event)throws Exception{
        suite=System.getProperty("foundations_pl4.decodeRestart","");if(suite.isEmpty())return;
        if(!Arrays.asList("raw","missing","compound","owner","dimension","scope","rewrite","live","live-context","live-owner").contains(suite))throw new IllegalStateException("Explicit decode suite required");
        String phase=System.getProperty("foundations_pl4.itemRestart","");
        if(!Arrays.asList("write","verify","crash-write","crash-verify").contains(phase))throw new IllegalStateException("Explicit restart phase required");
        MinecraftServer server=FMLCommonHandler.instance().getMinecraftServerInstance();
        if(!"pl4-item-restart-world".equals(server.getFolderName()))throw new IllegalStateException("Refusing non-fixture world");
        if(phase.equals("crash-write")&&!Boolean.getBoolean("foundations_pl4.restartHalt"))throw new IllegalStateException("Explicit halt opt-in required");
        world=server.getWorld(0);LegacyPL4.enabled=false;boolean passed=false;
        try{
            if(phase.endsWith("write"))prepare();
            verify(64,false);verify(80,true);verifyJournal();
            if(phase.endsWith("write")){world.saveAllChunks(true,null);world.getPerWorldStorage().saveAllData();world.getSaveHandler().flush();}
            passed=true;System.out.println("PL4 ITEM RESTART "+phase+": PASS");
        }finally{
            Map<String,Object> report=new LinkedHashMap<String,Object>();report.put("minecraft","1.12.2");report.put("suite","decode-"+suite);report.put("phase",phase);report.put("passed",passed);report.put("assertions",cases);report.put("tokens",tokens);report.put("decode_outcomes",outcomes);report.put("process",java.lang.management.ManagementFactory.getRuntimeMXBean().getName());
            FileWriter out=new FileWriter("item-decode-"+suite+"-restart-"+phase+".json");try{new com.google.gson.Gson().toJson(report,out);}finally{out.close();}
            if(passed&&phase.equals("crash-write")){System.out.println("PL4 ITEM RESTART KILL READY");System.out.flush();System.err.flush();while(true)Thread.sleep(1000);}
            server.initiateShutdown();
        }
    }
    private void prepare(){
        for(int x:new int[]{64,80}){
            load(x);load(x+5);air(x);air(x+5);place(x);chestBlock(x+5);
            LegacyTile n=node(x);n.owner=OWNER;n.pending=new ItemStack(Items.IRON_INGOT,8);n.itemFault="Original decode fixture uncertainty";n.itemImportPending=true;LegacyItemSafety.importSaved(n);
            check(n.itemJournalContext!=null,"seed unknown context "+x);
            String token=n.itemJournalContext.getString("Token");n.itemLegacyEvidence=evidence(x,token);n.fluidDirty();liveGuards(n,"seed "+x);
        }
        for(int x:new int[]{64,80}){
            LegacyTile n=node(x);NBTTagCompound tag=save(n);
            if(liveSuite()){mutateLive(n,x);tag=save(n);}
            else if(compoundSuite())tag.setTag("ItemJournalContext",rejected(x,n.itemLegacyEvidence.getString("OriginalToken")));
            else if(suite.equals("missing")){tag.removeTag("ItemJournalContext");tag.setString("ItemTransferFault","");}
            else if(x==64)tag.setInteger("ItemJournalContext",71);else tag.setString("ItemJournalContext","current-raw-context");
            n.readFromNBT(tag);LegacyItemSafety.importSaved(n);LegacyItemSafety.escrow(n).clearBlock();
            check(LegacyItemSafety.blocked(n),"generic settlement retains uncertainty "+x);n.fluidDirty();
        }
        air(80);ItemStack receipt=ItemStack.EMPTY;
        for(EntityItem e:world.getEntitiesWithinAABB(EntityItem.class,new AxisAlignedBB(78,199,31,84,203,34))){ if(!e.isDead&&e.getItem().getItem()==LegacyPL4.ITEM_CELL){check(receipt.isEmpty(),"single sealed removal receipt");receipt=e.getItem().copy();e.setDead();}}
        check(!receipt.isEmpty(),"actual sealed removal receipt");chest(85).setInventorySlotContents(0,receipt);chest(85).markDirty();
    }
    private void verify(int x,boolean removed){
        load(x);load(x+5);NBTTagCompound tag;
        if(removed){check(node(x)==null,"removed host stays absent "+x);ItemStack receipt=chest(x+5).getStackInSlot(0);check(receipt!=null&&!receipt.isEmpty()&&receipt.getItem()==LegacyPL4.ITEM_CELL,"receipt persists in native chest "+x);tag=receipt.getTagCompound();if(fieldSuite())authorize(receipt);}
        else{LegacyTile n=node(x);check(n!=null,"retained host reloads "+x);liveGuards(n,"retained "+x);LegacyItemSafety.importSaved(n);LegacyItemSafety.escrow(n).clearBlock();check(LegacyItemSafety.blocked(n),"reloaded uncertainty cannot settle "+x);tag=save(n);}
        NBTTagCompound original=tag.getCompoundTag("ItemLegacyEvidence");String token=original.getString("OriginalToken");tokens.add(token);
        check(UUID.fromString(token).toString().equals(token),"original token preserved "+x);
        check(original.equals(evidence(x,token)),"complete original evidence unchanged "+x);
        check(tag.getInteger("ItemEscrowCount")==8,"eight units remain sealed "+x);
        check(!tag.getString("ItemTransferFault").isEmpty(),"missing fault text cannot erase uncertainty "+x);
        if(compoundSuite())check(tag.getTag("ItemJournalContext") instanceof NBTTagCompound&&tag.getCompoundTag("ItemJournalContext").equals(rejected(x,token)),"rejected compound and nested payload retained "+x);
        else if(suite.equals("missing"))check(!tag.hasKey("ItemJournalContext"),"missing context never synthesized "+x);
        else if(x==64)check(tag.getTag("ItemJournalContext") instanceof net.minecraft.nbt.NBTTagInt&&tag.getInteger("ItemJournalContext")==71,"raw integer type and value retained");
        else check(tag.getTag("ItemJournalContext") instanceof net.minecraft.nbt.NBTTagString&&tag.getString("ItemJournalContext").equals("current-raw-context"),"raw string type and value retained");
        check(!original.hasKey("X")&&!original.hasKey("Phase"),"no original route or phase invented "+x);
        LegacyWirelessSafety journal=LegacyWirelessSafety.data(world);
        check(journal.blocked(OWNER,pos(200))&&journal.blocked("unrelated-owner",pos(240)),"all owners and targets remain fenced "+x);
        Map<String,Object> row=new LinkedHashMap<String,Object>();row.put("host_x",x);row.put("host_removed",removed);row.put("original_token",token);row.put("owner",OWNER);row.put("dimension",0);row.put("context_kind",compoundSuite()?"compound":suite.equals("missing")?"absent":x==64?"int":"string");row.put("context_value",compoundSuite()?contextValue():suite.equals("missing")?"":x==64?"71":"current-raw-context");row.put("sealed_units",8);row.put("evidence_marker","first-raw-payload");row.put("original_route_known",false);outcomes.add(row);
        if(liveSuite()&&!removed)verifyLive(node(x));
    }
    private void verifyJournal(){
        NBTTagCompound saved=journalTag(),opaque=saved;
        check(opaque.getCompoundTag("ExistingLegacyEvidence").equals(evidence(64,tokens.get(0))),"first rejection original evidence retained");
        if(compoundSuite())check(opaque.getCompoundTag("RejectedContext").equals(rejected(64,tokens.get(0))),"rejected compound opaque barrier preserved");
        else if(suite.equals("missing"))check(opaque.getBoolean("MissingContext")&&!opaque.hasKey("RejectedContext"),"missing opaque barrier preserved");
        else check(opaque.getTag("RejectedContext") instanceof net.minecraft.nbt.NBTTagInt&&opaque.getInteger("RejectedContext")==71,"raw opaque rejection type preserved");
        net.minecraft.nbt.NBTTagList entries=opaque.getCompoundTag("PreviousJournal").getTagList("Entries",10);Set<String> prior=new HashSet<String>();
        for(int i=0;i<entries.tagCount();i++){NBTTagCompound row=entries.getCompoundTagAt(i);prior.add(row.getString("Token"));check(!row.hasKey("X")&&!row.hasKey("Phase"),"prior journal remains unknown-route "+i);}
        check(prior.equals(new HashSet<String>(tokens))&&entries.tagCount()==2,"all prior tokens retained without replacement");
        for(int i=0;i<5;i++)LegacyItemSafety.importSaved(node(64));
        check(journalTag().equals(saved),"repeated admission does not grow opaque history");
        boolean enabled=LegacyPL4.enabled;LegacyPL4.enabled=true;
        try{load(200);place(200);node(200).owner="unrelated-owner";check(node(200).transferOnce()==0&&node(200).status.contains("fenced"),"replacement host cannot bypass dimension barrier");air(200);}finally{LegacyPL4.enabled=enabled;}
        check(journalTag().equals(saved),"inspection and replacement never clear journal");
    }
    private boolean liveSuite(){return suite.equals("live")||suite.equals("live-context")||suite.equals("live-owner");}
    private String liveOwner(){return suite.equals("live-context")?OWNER:"live-owner";}
    private void mutateLive(LegacyTile n,int x){
        LegacyItemSafety.importSaved(n);
        check(!n.itemImportPending&&(x!=64||!LegacyWirelessSafety.data(world).blocked("unrelated-owner",pos(240))),"live mutation starts after scoped admission latch cleared "+x);
        NBTTagCompound original=(NBTTagCompound)n.itemLegacyEvidence.copy();
        world.setBlockState(new BlockPos(x,199,32),Blocks.CHEST.getDefaultState(),3);TileEntityChest stock=(TileEntityChest)world.getTileEntity(new BlockPos(x,199,32));
        stock.setInventorySlotContents(0,new ItemStack(Items.IRON_INGOT,32));stock.markDirty();
        n.side=0;n.owner=liveOwner();n.itemFault="";if(!suite.equals("live-owner"))n.itemJournalContext=rejected(x,original.getString("OriginalToken"));
        check(n.transferOnce()==0&&LegacyTile.STACKS.count(n.pending)==8&&n.itemLegacyEvidence.equals(original),"live mutation transfer preserves evidence and escrow "+x);
        check(!n.itemImportPending&&LegacyWirelessSafety.data(world).blocked("unrelated-owner",pos(240)),"live mutation readmission freezes changed identity "+x);
        n.fluidDirty();
    }
    private void verifyLive(LegacyTile n){
        TileEntityChest stock=(TileEntityChest)world.getTileEntity(new BlockPos(64,199,32));
        check(n.owner.equals(liveOwner())&&stock!=null&&stock.getStackInSlot(0)!=null&&stock.getStackInSlot(0).getCount()==32,"live mutation persisted owner and physical source");
        NBTTagCompound original=(NBTTagCompound)n.itemLegacyEvidence.copy();
        boolean enabled=LegacyPL4.enabled;LegacyPL4.enabled=true;
        try{world.setBlockState(new BlockPos(64,198,32),LegacyPL4.EXPORT.getDefaultState(),3);LegacyTile alias=(LegacyTile)world.getTileEntity(new BlockPos(64,198,32));
            alias.owner=liveOwner();alias.side=1;
            check(alias.transferOnce()==0&&alias.status.contains("fenced")&&stock.getStackInSlot(0).getCount()==32,"live mutation disconnected alias stays fenced after restart");
        }finally{world.setBlockToAir(new BlockPos(64,198,32)); LegacyPL4.enabled=enabled;}
        check(LegacyTile.STACKS.count(n.pending)==8&&n.itemLegacyEvidence.equals(original),"live mutation alias leaves retained sealed claim unchanged");
    }
    private void liveGuards(LegacyTile n,String stage){
        n.itemFault="";
        check(n.itemLegacyEvidence!=null&&LegacyItemSafety.blocked(n),"live cleared fault retains evidence quarantine "+stage);
        ItemStack held=n.pending;int face=n.side;n.pending=ItemStack.EMPTY;
        check(!LegacyItemSafety.selectFace(n,(face+1)%6)&&n.side==face,"live cleared fault rejects empty escrow face edit "+stage);
        n.pending=held;NBTTagCompound original=(NBTTagCompound)n.itemLegacyEvidence.copy();
        n.readFromNBT(save(n));
        check(LegacyItemSafety.blocked(n)&&n.itemImportPending&&LegacyTile.STACKS.count(n.pending)==8&&n.itemLegacyEvidence.equals(original),"live cleared fault NBT reload preserves evidence and count "+stage);
        n.fluidDirty();
    }
    private boolean fieldSuite(){return Arrays.asList("owner","dimension","scope","rewrite").contains(suite);}
    private void authorize(ItemStack receipt){
        net.minecraft.entity.player.EntityPlayer owner=actor(true,false),other=actor(false,false),operator=actor(false,true);
        boolean allowed=!denied(world,owner,receipt);
        check(allowed==suite.equals("scope"),"reloaded receipt owner authorization is typed and scoped");
        check(denied(world,other,receipt),"reloaded receipt rejects unrelated profile");
        String expected=suite.equals("owner")||suite.equals("dimension")?"malformed owner/dimension":suite.equals("rewrite")?"different dimension":"opaque journal";
        check(LegacyWirelessSafety.receiptDiagnostic(world,operator,receipt).contains(expected),"reloaded receipt operator diagnostic preserves uncertainty");
        WorldServer foreign=world.getMinecraftServer().getWorld(-1);
        check(foreign!=null&&foreign!=world&&denied(foreign,operator,receipt),"receipt diagnostic rejects foreign world argument");
    }
    private boolean denied(net.minecraft.world.World query,net.minecraft.entity.player.EntityPlayer player,ItemStack receipt){try{LegacyWirelessSafety.receiptDiagnostic(query,player,receipt);return false;}catch(IllegalArgumentException expected){return true;}}
    private net.minecraft.entity.player.EntityPlayer actor(boolean own,boolean op){return new DiagnosticPlayer(world,own,op);}
    private static final class DiagnosticPlayer extends net.minecraftforge.common.util.FakePlayer {
        final boolean operator;
        DiagnosticPlayer(WorldServer world,boolean own,boolean op){super(world,new com.mojang.authlib.GameProfile(java.util.UUID.fromString(own?OWNER:"00000000-0000-0000-0000-000000000009"),own?"Owner":"Other"));operator=op;}
        @Override public boolean canUseCommand(int level,String name){return operator;}
    }
    private boolean compoundSuite(){return !suite.equals("raw")&&!suite.equals("missing");}
    private String contextValue(){
        if(suite.equals("live-owner"))return "original-context-host-owner-rewrite";
        if(suite.equals("owner"))return "rejected-owner-int71";
        if(suite.equals("dimension"))return "rejected-dimension-string0";
        if(suite.equals("scope"))return "rejected-scope-compound";
        if(suite.equals("rewrite")||liveSuite())return "rewritten-owner-dimension-host";
        return "rejected-token-int71";
    }
    private NBTTagCompound rejected(int x,String token){
        NBTTagCompound r=new NBTTagCompound();
        if(suite.equals("compound"))r.setInteger("Token",71);else r.setString("Token",token);
        r.setString("Owner",OWNER);r.setInteger("Dimension",0);r.setString("Scope","legacy-unknown");
        r.setInteger("HostX",x);r.setInteger("HostY",200);r.setInteger("HostZ",32);
        if(suite.equals("live-owner"))return r;
        NBTTagCompound p=new NBTTagCompound();p.setByteArray("Bytes",new byte[]{4,5,6});r.setTag("RejectedPayload",p);
        if(suite.equals("owner"))r.setInteger("Owner",71);
        if(suite.equals("dimension"))r.setString("Dimension","0");
        if(suite.equals("scope")){NBTTagCompound scope=new NBTTagCompound();scope.setInteger("Marker",71);scope.setByteArray("Bytes",new byte[]{7,8,9});r.setTag("Scope",scope);}
        if(suite.equals("rewrite")||liveSuite()){r.setString("Owner","rewritten-owner");r.setInteger("Dimension",7);r.setInteger("HostX",x+128);}
        return r;
    }
    private NBTTagCompound evidence(int x,String token){NBTTagCompound e=new NBTTagCompound();e.setString("Fault","Original decode fixture uncertainty");e.setString("ObservedOwner",OWNER);e.setInteger("ObservedDimension",0);e.setInteger("HostX",x);e.setInteger("HostY",200);e.setInteger("HostZ",32);e.setString("OriginalToken",token);e.setString("MalformedContext","first-raw-payload");NBTTagCompound nested=new NBTTagCompound();nested.setInteger("Sentinel",12345);nested.setByteArray("Bytes",new byte[]{1,2,3});e.setTag("OriginalPayload",nested);return e;}
    private NBTTagCompound save(LegacyTile n){NBTTagCompound tag=new NBTTagCompound();n.writeToNBT(tag);return tag;}
    private NBTTagCompound journalTag(){NBTTagCompound tag=new NBTTagCompound();tag=LegacyWirelessSafety.data(world).writeToNBT(tag);return tag;}
    private void check(boolean ok,String name){if(!ok)throw new AssertionError(name);cases.add(name);}
        private BlockPos pos(int x){return new BlockPos(x,200,32);}
    private void place(int x){world.setBlockState(pos(x),LegacyPL4.EXPORT.getDefaultState(),3);node(x).owner=OWNER;node(x).fluidDirty();}
    private void chestBlock(int x){world.setBlockState(pos(x),Blocks.CHEST.getDefaultState(),3);}
    private LegacyTile node(int x){return (LegacyTile)world.getTileEntity(pos(x));}
    private TileEntityChest chest(int x){return (TileEntityChest)world.getTileEntity(pos(x));}

    private void load(int x){world.getChunkFromBlockCoords(pos(x));}
    private void air(int x){world.setBlockToAir(pos(x));}
}
