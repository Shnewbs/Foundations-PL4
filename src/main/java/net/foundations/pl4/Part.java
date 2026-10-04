package net.foundations.pl4;

import java.util.*;
import net.foundations.pl4.core.DisplayElements;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** A face attachment or centre cable. Only the server mutates persistent state. */
public final class Part {
    public final Kind kind;
    public final Direction face;
    public UUID owner;
    public UUID identity = UUID.randomUUID();
    public String label = "", filter = "", selected = "", metric = "", mode = "LIST", comparison = ">=";
    // Old saves keep their inward front until explicitly flipped; no automatic controller/layout migration.
    public boolean displayOutward;
    public int hologramView = 3; // SOUTH for legacy floor/ceiling projectors; wall view is derived.
    public long layoutRevision; // Shared large-display edits; persisted so expansion/reload keeps the layout.
    public String energySystem = "AUTO";
    public String targetChannel = "", targetQuery = "", targetLabel = "";
    public int targetPage,targetCount;
    public final Map<String,String> channelNames=new LinkedHashMap<>();
    public final List<ReaderChoice> targetChoices=new ArrayList<>(); // Derived from this reader's network; sync-only.
    public String energyInput="FE",energyOutput="FE",pendingEnergyUnit="FE";
    public int energyVoltage=32,pendingEnergyJRate,pendingEnergyEURate,pendingEnergyEDRate;
    public long pendingEnergyCredits;
    public String energyTransferStatus="";
    public boolean energyConvert;
    public long energyCredits(){return pendingEnergyCredits>0?pendingEnergyCredits:(long)pendingEnergy*net.foundations.pl4.core.EnergyConversion.FE;}
    public void energyCredits(long value){pendingEnergyCredits=Math.max(0,value);pendingEnergy=(int)Math.min(Integer.MAX_VALUE,pendingEnergyCredits/net.foundations.pl4.core.EnergyConversion.FE);energyEscrow=pendingEnergyCredits>0;}
    public boolean energyEscrow;
    public boolean energyRouteEditable(){return energyCredits()==0&&!energyEscrow;}

    public DisplayElements.Mode displayMode=DisplayElements.Mode.AUTO_LIST;
    public int displayPage;
    public int layoutWidth=DisplayElements.WIDTH,layoutHeight=DisplayElements.HEIGHT; // Persisted logical coordinate space; R11 large canvases migrate proportionally.
    public final List<ReaderChoice> readerChoices=new ArrayList<>();
    public final Map<String,List<Row>> sourceRows=new LinkedHashMap<>(); // Only visible, explicitly bound sources; sync-only.
    public double threshold = 1;
    public int index, priority, signal, color = 0x79D3FF, transferMode; // 0 passive, 1 add, 2 remove, 3 both
    public boolean items = true, fluids = true, energy = true, descending = true, whitelist = true;
    public long ticks;
    public int blockedFaces; // Six cable ports; zero preserves old saves.
    public int canvasWidth=1,canvasHeight=1,canvasColumn,canvasRow,canvasMask; // Derived, sync-only; no layout destruction on split. // Six cable ports; zero preserves old saves.
    public ItemStack pendingItem = ItemStack.EMPTY;
    public FluidStack pendingFluid = FluidStack.EMPTY;
    public int pendingEnergy;
    public final List<Link> links = new ArrayList<>();
    public final List<Row> rows = new ArrayList<>();
    public final List<Element> elements = new ArrayList<>();
    public String status = "Waiting for network";

    public Part(Kind kind, Direction face, UUID owner) { this.kind = kind; this.face = face; this.owner = owner; }
    public boolean hologram(){return kind==Kind.HOLOGRAM||kind==Kind.ADVANCED_HOLOGRAM;}
    public record DisplaySettings(String label,String selected,String metric,int color,List<Element> elements,DisplayElements.Mode displayMode,int displayPage,int layoutWidth,int layoutHeight) {
        public DisplaySettings { elements=List.copyOf(elements);layoutWidth=Math.clamp(layoutWidth,8,DisplayElements.MAX_CANVAS);layoutHeight=Math.clamp(layoutHeight,9,DisplayElements.MAX_CANVAS); }
        public DisplaySettings(String label,String selected,String metric,int color,List<Element> elements){this(label,selected,metric,color,elements,elements.isEmpty()?DisplayElements.Mode.AUTO_LIST:DisplayElements.Mode.CUSTOM,0,DisplayElements.WIDTH,DisplayElements.HEIGHT);}
        public boolean configured(){return !label.isBlank()||!selected.isBlank()||!metric.isBlank()||color!=0x79D3FF||!elements.isEmpty()||displayMode==DisplayElements.Mode.CUSTOM;}
    }
    public DisplaySettings displaySettings(){return new DisplaySettings(label,selected,metric,color,elements,displayMode,displayPage,layoutWidth,layoutHeight);}
    public boolean applyDisplaySettings(DisplaySettings settings,long revision){
        if(displaySettings().equals(settings)&&layoutRevision==revision)return false;
        label=settings.label();selected=settings.selected();metric=settings.metric();color=settings.color();
        elements.clear();elements.addAll(settings.elements());displayMode=settings.displayMode();displayPage=settings.displayPage();layoutWidth=settings.layoutWidth();layoutHeight=settings.layoutHeight();layoutRevision=revision;return true;
    }
    public Direction displayFront(){return Direction.from3DDataValue(net.foundations.pl4.core.DisplayFacing.front(face.ordinal(),displayOutward));}
    public int slot() { return net.foundations.pl4.core.MultipartTopology.slot(kind,face.ordinal()); }
    public boolean canExtract() { return transferMode == 2 || transferMode == 3; }
    public boolean canInsert() { return transferMode == 1 || transferMode == 3; }
    public String title() { return label.isBlank() ? kind.id : label; }

    public record Link(String dimension, BlockPos pos, Direction side, UUID entity, UUID part) {
        public CompoundTag save() {
            CompoundTag t = new CompoundTag(); t.putString("dimension", dimension); t.putLong("pos", pos.asLong()); t.putInt("side", side.ordinal());
            if (entity != null) t.store("entity",net.minecraft.core.UUIDUtil.CODEC, entity); if (part != null) t.store("part",net.minecraft.core.UUIDUtil.CODEC, part); return t;
        }
        public static Link load(CompoundTag t) {
            return new Link(t.getString("dimension").orElse(""), BlockPos.of(t.getLong("pos").orElse(0L)), Direction.from3DDataValue(t.getInt("side").orElse(0)), t.read("entity",net.minecraft.core.UUIDUtil.CODEC).isPresent() ? t.read("entity",net.minecraft.core.UUIDUtil.CODEC).orElseThrow() : null, t.read("part",net.minecraft.core.UUIDUtil.CODEC).isPresent() ? t.read("part",net.minecraft.core.UUIDUtil.CODEC).orElseThrow() : null);
        }
    }
    public record ReaderChoice(String id,String name,String kind) {}
    /** Preview stacks are normalized to one and bounded by VisualSamples; quantities remain numeric. */
    public record Row(String key,String name,double value,double capacity,String unit,ItemStack item,FluidStack fluid,CompoundTag previewItem,CompoundTag previewFluid) implements DisplayElements.Sample {
        public Row(String key,String name,double value,double capacity,String unit){this(key,name,value,capacity,unit,ItemStack.EMPTY,FluidStack.EMPTY,new CompoundTag(),new CompoundTag());}
        public boolean hasItem(){return !item.isEmpty();}public boolean hasBlock(){return hasItem()&&item.getItem() instanceof net.minecraft.world.item.BlockItem;}public boolean hasFluid(){return !fluid.isEmpty();}
        public String itemId(){return hasItem()?net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem()).toString():"";}
        public String fluidId(){return hasFluid()?net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString():"";}
        public String text(){return name+": "+number(value)+(capacity>0?" / "+number(capacity):"")+(unit.isEmpty()?"":" "+unit);}
        public static String number(double d){return DisplayElements.number(d,false);}
        public CompoundTag save(){CompoundTag t=new CompoundTag();t.putString("key",key);t.putString("name",name);t.putDouble("value",value);t.putDouble("capacity",capacity);t.putString("unit",unit);if(!previewItem.isEmpty())t.put("item",previewItem.copy());if(!previewFluid.isEmpty())t.put("fluid",previewFluid.copy());return t;}
        public static Row load(CompoundTag t){return new Row(t.getString("key").orElse(""),t.getString("name").orElse(""),t.getDouble("value").orElse(0.0),t.getDouble("capacity").orElse(0.0),t.getString("unit").orElse(""));}
        public static Row load(CompoundTag t,HolderLookup.Provider registry){
            ItemStack item=net.foundations.pl4.NbtStacks.item(registry,t.getCompound("item").orElseGet(CompoundTag::new));FluidStack fluid=net.foundations.pl4.NbtStacks.fluid(registry,t.getCompound("fluid").orElseGet(CompoundTag::new));
            return new Row(t.getString("key").orElse(""),t.getString("name").orElse(""),t.getDouble("value").orElse(0.0),t.getDouble("capacity").orElse(0.0),t.getString("unit").orElse(""),item,fluid,t.getCompound("item").orElseGet(CompoundTag::new),t.getCompound("fluid").orElseGet(CompoundTag::new));
        }
    }
    public record Element(DisplayElements.Spec spec) {
        public Element(String text,String reader,String key,int x,int y,int color,boolean bar){this(new DisplayElements.Spec(UUID.randomUUID(),bar?DisplayElements.Type.BAR:DisplayElements.Type.TEXT,text,reader,key,"",new DisplayElements.Rect(x,y,Math.max(8,248-x),bar?18:12),color,true,false,8,0,0,false,false));}
        public UUID id(){return spec.id();} public String text(){return spec.text();}public String reader(){return spec.reader();}public String key(){return spec.key();}
        public int x(){return spec.bounds().x();}public int y(){return spec.bounds().y();}public int color(){return spec.color();}public boolean bar(){return spec.type()==DisplayElements.Type.BAR;}
        public CompoundTag save(){
            CompoundTag t=new CompoundTag();t.store("id",net.minecraft.core.UUIDUtil.CODEC,id());t.putString("type",spec.type().name());t.putString("text",text());t.putString("reader",reader());t.putString("key",key());t.putString("asset",spec.asset());
            t.putInt("x",x());t.putInt("y",y());t.putInt("w",spec.bounds().width());t.putInt("h",spec.bounds().height());t.putInt("color",color());t.putBoolean("bar",bar());
            t.putBoolean("count",spec.count());t.putBoolean("names",spec.names());t.putInt("columns",spec.columns());t.putInt("offset",spec.offset());t.putInt("page",spec.page());t.putBoolean("vertical",spec.vertical());t.putBoolean("compact",spec.compact());
            t.putString("textAlign",spec.textAlign().name());t.putBoolean("wrap",spec.wrap());t.putFloat("textScale",spec.textScale());return t;
        }
        public static Element load(CompoundTag t){
            boolean legacy=!t.contains("type");DisplayElements.Type type=legacy?(t.getBoolean("bar").orElse(false)?DisplayElements.Type.BAR:DisplayElements.Type.TEXT):DisplayElements.Type.parse(t.getString("type").orElse(""));
            UUID id=t.read("id",net.minecraft.core.UUIDUtil.CODEC).isPresent()?t.read("id",net.minecraft.core.UUIDUtil.CODEC).orElseThrow():UUID.nameUUIDFromBytes(t.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return new Element(new DisplayElements.Spec(id,type,t.getString("text").orElse(""),t.getString("reader").orElse(""),t.getString("key").orElse(""),t.getString("asset").orElse(""),
                new DisplayElements.Rect(t.getInt("x").orElse(0),t.getInt("y").orElse(0),legacy?248-t.getInt("x").orElse(0):t.getInt("w").orElse(0),legacy?(type==DisplayElements.Type.BAR?18:12):t.getInt("h").orElse(0)),
                t.getInt("color").orElse(0),legacy||t.getBoolean("count").orElse(false),t.getBoolean("names").orElse(false),legacy?8:t.getInt("columns").orElse(0),t.getInt("offset").orElse(0),t.getInt("page").orElse(0),t.getBoolean("vertical").orElse(false),t.getBoolean("compact").orElse(false),
                DisplayElements.TextAlign.parse(t.getString("textAlign").orElse("")),t.getBoolean("wrap").orElse(false),t.contains("textScale")?t.getFloat("textScale").orElse(0f):1F));
        }
    }
    public CompoundTag save(HolderLookup.Provider registry, boolean sync) {
        CompoundTag t = new CompoundTag(); t.putString("kind",kind.id); t.putInt("face",face.ordinal()); t.store("identity",net.minecraft.core.UUIDUtil.CODEC,identity);
        if (owner != null) t.store("owner",net.minecraft.core.UUIDUtil.CODEC,owner);
        t.putString("displayMode",displayMode.name());t.putInt("displayPage",displayPage);t.putInt("layoutWidth",layoutWidth);t.putInt("layoutHeight",layoutHeight);
        t.putInt("hologramView",hologramView);t.putLong("layoutRevision",layoutRevision);
        t.putBoolean("displayOutward",displayOutward);t.putString("energySystem",energySystem);
        t.putString("targetChannel",targetChannel);t.putString("targetQuery",targetQuery);t.putInt("targetPage",targetPage);
        ListTag aliases=new ListTag();for(var entry:channelNames.entrySet()){CompoundTag c=new CompoundTag();c.putString("id",entry.getKey());c.putString("name",entry.getValue());aliases.add(c);}t.put("channelNames",aliases);
        t.putString("label",label); t.putString("filter",filter); t.putString("selected",selected); t.putString("metric",metric); t.putString("mode",mode);
        t.putString("comparison",comparison); t.putDouble("threshold",threshold); t.putInt("index",index); t.putInt("priority",priority); t.putInt("signal",signal);
        t.putInt("color",color); t.putInt("transferMode",transferMode); t.putBoolean("items",items); t.putBoolean("fluids",fluids); t.putBoolean("energy",energy);
        t.putBoolean("energyConvert",energyConvert);t.putString("energyInput",energyInput);t.putString("energyOutput",energyOutput);t.putInt("energyVoltage",energyVoltage);
        // Amount remains server-only; the boolean prevents editing a route while escrow exists.
        t.putBoolean("energyEscrow",energyCredits()>0);
        t.putInt("blockedFaces",blockedFaces & 63); t.putBoolean("descending",descending); t.putBoolean("whitelist",whitelist); t.putLong("ticks",ticks);
        if (!sync) {
            if (!pendingItem.isEmpty()) t.put("pendingItem",net.foundations.pl4.NbtStacks.save(pendingItem,registry));
            if (!pendingFluid.isEmpty()) t.put("pendingFluid",net.foundations.pl4.NbtStacks.save(pendingFluid,registry));
            t.putInt("pendingEnergy",pendingEnergy);t.putLong("pendingEnergyCredits",energyCredits());
            t.putString("pendingEnergyUnit",pendingEnergyUnit);t.putInt("pendingEnergyJRate",pendingEnergyJRate);t.putInt("pendingEnergyEURate",pendingEnergyEURate);t.putInt("pendingEnergyEDRate",pendingEnergyEDRate);
        }
        ListTag l = new ListTag(); links.forEach(a -> l.add(a.save())); t.put("links",l);
        ListTag e = new ListTag(); elements.forEach(a -> e.add(a.save())); t.put("elements",e);
        if (sync) {
            t.putInt("canvasWidth",canvasWidth);t.putInt("canvasHeight",canvasHeight);t.putInt("canvasColumn",canvasColumn);t.putInt("canvasRow",canvasRow);t.putInt("canvasMask",canvasMask);
            ListTag r = new ListTag(); rows.forEach(a -> r.add(a.save())); t.put("rows",r); t.putString("status",status);
            ListTag choices=new ListTag();for(var choice:readerChoices){CompoundTag c=new CompoundTag();c.putString("id",choice.id());c.putString("name",choice.name());c.putString("kind",choice.kind());choices.add(c);}t.put("readerChoices",choices);
            t.putString("targetLabel",targetLabel);t.putInt("targetCount",targetCount);
            ListTag channels=new ListTag();for(var choice:targetChoices){CompoundTag c=new CompoundTag();c.putString("id",choice.id());c.putString("name",choice.name());c.putString("kind",choice.kind());channels.add(c);}t.put("targetChoices",channels);
            ListTag sources=new ListTag();for(var entry:sourceRows.entrySet()){CompoundTag c=new CompoundTag();c.putString("id",entry.getKey());ListTag data=new ListTag();entry.getValue().forEach(row->data.add(row.save()));c.put("data",data);sources.add(c);}t.put("sources",sources);
        }
        return t;
    }
    public static Part load(CompoundTag t, HolderLookup.Provider registry) {
        Kind k = Kind.byId(t.getString("kind").orElse("")); if (k == null) return null;
        Part p = new Part(k, Direction.from3DDataValue(t.getInt("face").orElse(0)), t.read("owner",net.minecraft.core.UUIDUtil.CODEC).isPresent() ? t.read("owner",net.minecraft.core.UUIDUtil.CODEC).orElseThrow() : null);
        if (t.read("identity",net.minecraft.core.UUIDUtil.CODEC).isPresent()) p.identity = t.read("identity",net.minecraft.core.UUIDUtil.CODEC).orElseThrow();
        p.hologramView=net.foundations.pl4.core.HologramProjection.view(p.face.ordinal(),t.contains("hologramView")?t.getInt("hologramView").orElse(0):3);
        p.layoutRevision=Math.max(0,t.getLong("layoutRevision").orElse(0L));
        p.displayOutward=t.getBoolean("displayOutward").orElse(false);p.energySystem=net.foundations.pl4.core.EnergyValues.system(t.getString("energySystem").orElse(""));
        p.targetChannel=ReaderChannels.sanitize(t.getString("targetChannel").orElse(""));
        p.targetQuery=ReaderChannels.clean(t.getString("targetQuery").orElse(""));p.targetLabel=t.getString("targetLabel").orElse("");
        p.targetPage=Math.clamp(t.getInt("targetPage").orElse(0),0,65535);p.targetCount=Math.max(0,t.getInt("targetCount").orElse(0));
        ListTag aliases=t.getList("channelNames").orElseGet(ListTag::new);for(int i=0;i<Math.min(64,aliases.size());i++){CompoundTag c=aliases.getCompound(i).orElseGet(CompoundTag::new);String id=ReaderChannels.sanitize(c.getString("id").orElse(""));String label=ReaderChannels.clean(c.getString("name").orElse(""));if(!id.isEmpty()&&!label.isBlank())p.channelNames.put(id,label);}
        p.label=t.getString("label").orElse(""); p.filter=t.getString("filter").orElse(""); p.selected=t.getString("selected").orElse(""); p.metric=t.getString("metric").orElse(""); p.mode=t.getString("mode").orElse("");
        p.comparison=t.getString("comparison").orElse(""); p.threshold=t.getDouble("threshold").orElse(0.0); p.index=t.getInt("index").orElse(0); p.priority=t.getInt("priority").orElse(0); p.signal=t.getInt("signal").orElse(0);
        p.color=t.getInt("color").orElse(0); p.transferMode=t.getInt("transferMode").orElse(0); p.items=t.getBoolean("items").orElse(false); p.fluids=t.getBoolean("fluids").orElse(false); p.energy=t.getBoolean("energy").orElse(false);
        p.energyConvert=t.getBoolean("energyConvert").orElse(false);p.energyInput=net.foundations.pl4.core.EnergyConversion.unit(t.getString("energyInput").orElse(""));p.energyOutput=net.foundations.pl4.core.EnergyConversion.unit(t.getString("energyOutput").orElse(""));
        p.energyVoltage=t.contains("energyVoltage")?Math.clamp(t.getInt("energyVoltage").orElse(0),1,1048576):32;
        p.pendingEnergyUnit=net.foundations.pl4.core.EnergyConversion.unit(t.getString("pendingEnergyUnit").orElse(""));
        p.pendingEnergyJRate=Math.max(0,t.getInt("pendingEnergyJRate").orElse(0));p.pendingEnergyEURate=Math.max(0,t.getInt("pendingEnergyEURate").orElse(0));p.pendingEnergyEDRate=Math.max(0,t.getInt("pendingEnergyEDRate").orElse(0));
        p.pendingEnergyCredits=Math.max(0,t.getLong("pendingEnergyCredits").orElse(0L));p.energyEscrow=!t.contains("pendingEnergy")&&t.getBoolean("energyEscrow").orElse(false);
        p.blockedFaces=t.getInt("blockedFaces").orElse(0) & 63; p.descending=t.getBoolean("descending").orElse(false); p.whitelist=t.getBoolean("whitelist").orElse(false); p.ticks=t.getLong("ticks").orElse(0L);
        p.pendingItem=net.foundations.pl4.NbtStacks.item(registry,t.getCompound("pendingItem").orElseGet(CompoundTag::new)); p.pendingFluid=net.foundations.pl4.NbtStacks.fluid(registry,t.getCompound("pendingFluid").orElseGet(CompoundTag::new)); p.pendingEnergy=Math.max(0,t.getInt("pendingEnergy").orElse(0));
        ListTag links=t.getList("links").orElseGet(ListTag::new); for(int i=0;i<Math.min(links.size(),64);i++) p.links.add(Link.load(links.getCompound(i).orElseGet(CompoundTag::new)));
        ListTag elements=t.getList("elements").orElseGet(ListTag::new); for(int i=0;i<Math.min(elements.size(),32);i++){Element e=Element.load(elements.getCompound(i).orElseGet(CompoundTag::new));UUID original=e.id();if(p.elements.stream().anyMatch(old->old.id().equals(original)))e=new Element(e.spec().identity(UUID.randomUUID()));p.elements.add(e);}
        p.displayMode=t.contains("displayMode")?DisplayElements.Mode.parse(t.getString("displayMode").orElse("")):(p.elements.isEmpty()?DisplayElements.Mode.AUTO_LIST:DisplayElements.Mode.CUSTOM);p.displayPage=Math.clamp(t.getInt("displayPage").orElse(0),0,7);p.layoutWidth=t.contains("layoutWidth")?Math.clamp(t.getInt("layoutWidth").orElse(0),8,DisplayElements.MAX_CANVAS):DisplayElements.WIDTH;p.layoutHeight=t.contains("layoutHeight")?Math.clamp(t.getInt("layoutHeight").orElse(0),9,DisplayElements.MAX_CANVAS):DisplayElements.HEIGHT;
        ListTag rows=t.getList("rows").orElseGet(ListTag::new); for(int i=0;i<Math.min(rows.size(),256);i++) p.rows.add(Row.load(rows.getCompound(i).orElseGet(CompoundTag::new),registry));
        ListTag choices=t.getList("readerChoices").orElseGet(ListTag::new);for(int i=0;i<Math.min(64,choices.size());i++){CompoundTag c=choices.getCompound(i).orElseGet(CompoundTag::new);p.readerChoices.add(new ReaderChoice(c.getString("id").orElse(""),c.getString("name").orElse(""),c.getString("kind").orElse("")));}
        ListTag sources=t.getList("sources").orElseGet(ListTag::new);int budget=256;for(int i=0;i<Math.min(8,sources.size());i++){CompoundTag c=sources.getCompound(i).orElseGet(CompoundTag::new);ListTag data=c.getList("data").orElseGet(ListTag::new);List<Row> list=new ArrayList<>();for(int n=0;n<Math.min(64,data.size())&&budget>0;n++,budget--)list.add(Row.load(data.getCompound(n).orElseGet(CompoundTag::new),registry));p.sourceRows.put(c.getString("id").orElse(""),List.copyOf(list));}
        ListTag channels=t.getList("targetChoices").orElseGet(ListTag::new);for(int i=0;i<Math.min(64,channels.size());i++){CompoundTag c=channels.getCompound(i).orElseGet(CompoundTag::new);p.targetChoices.add(new ReaderChoice(c.getString("id").orElse(""),c.getString("name").orElse(""),c.getString("kind").orElse("")));}
        p.status=t.getString("status").orElse("");
        p.canvasWidth=Math.clamp(t.getInt("canvasWidth").orElse(0),1,16);p.canvasHeight=Math.clamp(t.getInt("canvasHeight").orElse(0),1,16);
        p.canvasColumn=Math.clamp(t.getInt("canvasColumn").orElse(0),0,p.canvasWidth-1);p.canvasRow=Math.clamp(t.getInt("canvasRow").orElse(0),0,p.canvasHeight-1);p.canvasMask=t.getInt("canvasMask").orElse(0)&15;
        return p;
    }
}
