"""One-time, idempotent corrections found by the first real Forge GameTest run."""
from pathlib import Path
import json
R=Path(__file__).resolve().parents[2];J=R/'src/main/java/net/foundations/pl4'
p=J/'core/ForgingRecipe.java';s=p.read_text()
s=s.replace('Codec.intRange(1,64).optionalFieldOf("input_count",1)','optional(Codec.intRange(1,64),"input_count",1)').replace('Codec.intRange(1,72000).optionalFieldOf("processing_ticks",100)','optional(Codec.intRange(1,72000),"processing_ticks",100)').replace('Codec.intRange(0,72000).optionalFieldOf("cooldown_ticks",200)','optional(Codec.intRange(0,72000),"cooldown_ticks",200)')
if 'private static <A> MapCodec<A> optional' not in s:
 marker='  public MapCodec<ForgingRecipe> codec(){return CODEC;}'
 assert marker in s
 s=s.replace(marker,'''  private static <A> MapCodec<A> optional(Codec<A> codec,String name,A fallback){return new MapCodec<>(){
   @Override public <T> DataResult<A> decode(DynamicOps<T> ops,MapLike<T> input){T value=input.get(name);return value==null?DataResult.success(fallback):codec.parse(ops,value);}
   @Override public <T> RecordBuilder<T> encode(A value,DynamicOps<T> ops,RecordBuilder<T> prefix){return prefix.add(name,codec.encodeStart(ops,value));}
   @Override public <T> java.util.stream.Stream<T> keys(DynamicOps<T> ops){return java.util.stream.Stream.of(ops.createString(name));}
  };}
'''+marker)
p.write_text(s)
root=R/'src/main/resources/data'
aliases={'chests/wooden':['minecraft:chest','minecraft:trapped_chest'],'dusts/redstone':['minecraft:redstone'],'ingots/iron':['minecraft:iron_ingot'],'rods/wooden':['minecraft:stick'],'stones':['minecraft:stone','minecraft:andesite','minecraft:diorite','minecraft:granite'],'ender_pearls':['minecraft:ender_pearl'],'gems/diamond':['minecraft:diamond']}
for name,fallback in aliases.items():
 p=root/'c/tags/items'/(name+'.json');p.parent.mkdir(parents=True,exist_ok=True)
 expected={'replace':False,'values':[{'id':'#forge:'+name,'required':False}]+fallback}
 if p.exists():
  assert json.loads(p.read_text())==expected,('Refusing to overwrite changed tag',p)
 else:p.write_text(json.dumps(expected,indent=2)+'\n')
p=J/'PLGameTests.java';s=p.read_text()
if 'void isolatedConfig(' not in s:
 marker='public final class PLGameTests {';assert marker in s
 s=s.replace(marker,marker+'''
    /** CI-only isolation: transient test values must not race Forge's autosave file watcher.
     * Normal server configuration remains file-backed; no fixture assertions are relaxed. */
    @net.minecraft.gametest.framework.BeforeBatch(batch="defaultBatch")
    public static void isolatedConfig(net.minecraft.server.level.ServerLevel level){
        if(!Boolean.getBoolean("foundations_pl4.isolatedGameTestConfig"))return;
        var memory=com.electronwill.nightconfig.core.CommentedConfig.inMemory();
        PLConfig.SPEC.correct(memory);PLConfig.SPEC.setConfig(memory);
    }
''')
p.write_text(s)
p=R/'build.gradle';s=p.read_text().replace("gameTestServer { property 'forge.enabledGameTestNamespaces', 'foundations_pl4' }","gameTestServer { property 'forge.enabledGameTestNamespaces', 'foundations_pl4'; property 'foundations_pl4.isolatedGameTestConfig','true' }");p.write_text(s)
print('Strict optional fields, native tag aliases and CI-only config isolation are installed')
