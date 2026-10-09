package net.foundations.pl4;

import com.google.gson.*;
import java.util.List;
import java.util.UUID;
import net.foundations.pl4.core.DisplayElements;
/** Explicit data format; never reflective class instantiation from client input. */
public final class ElementJson {
    /** Whole-layout snapshot, used for undo/redo and future layout import/export. */
    public static String encodeList(List<DisplayElements.Spec> elements){
        JsonArray array=new JsonArray();for(var e:elements)array.add(new JsonParser().parse(encode(e)));return array.toString();
    }
    public static List<DisplayElements.Spec> decodeList(String text){
        if(text.length()>65536)throw new IllegalArgumentException("Layout snapshot too large");
        JsonArray array=new JsonParser().parse(text).getAsJsonArray();
        if(array.size()>DisplayElements.MAX_ELEMENTS)throw new IllegalArgumentException("Too many layout elements");
        List<DisplayElements.Spec> result=new java.util.ArrayList<>();for(var e:array)result.add(decode(e.toString()));return result;
    }
    public static String encode(DisplayElements.Spec e){
        JsonObject j=new JsonObject();j.addProperty("id",e.id().toString());j.addProperty("type",e.type().name());j.addProperty("text",e.text());j.addProperty("reader",e.reader());j.addProperty("key",e.key());j.addProperty("asset",e.asset());
        j.addProperty("x",e.bounds().x());j.addProperty("y",e.bounds().y());j.addProperty("w",e.bounds().width());j.addProperty("h",e.bounds().height());j.addProperty("color",e.color());j.addProperty("count",e.count());j.addProperty("names",e.names());j.addProperty("columns",e.columns());j.addProperty("offset",e.offset());j.addProperty("page",e.page());j.addProperty("vertical",e.vertical());j.addProperty("compact",e.compact());j.addProperty("textAlign",e.textAlign().name());j.addProperty("wrap",e.wrap());j.addProperty("textScale",e.textScale());j.addProperty("group",e.options().group());j.addProperty("locked",e.options().locked());j.addProperty("hidden",e.options().hidden());j.addProperty("background",e.options().background());j.addProperty("border",e.options().border());j.addProperty("actionPage",e.options().actionPage());return j.toString();
    }
    public static DisplayElements.Spec decode(String text){
        if(text.length()>4096)throw new IllegalArgumentException("Element too large");JsonObject j=new JsonParser().parse(text).getAsJsonObject();
        return new DisplayElements.Spec(UUID.fromString(j.get("id").getAsString()),DisplayElements.Type.valueOf(j.get("type").getAsString()),str(j,"text"),str(j,"reader"),str(j,"key"),str(j,"asset"),
            new DisplayElements.Rect(integer(j,"x"),integer(j,"y"),integer(j,"w"),integer(j,"h")),integer(j,"color"),bool(j,"count"),bool(j,"names"),integer(j,"columns"),integer(j,"offset"),integer(j,"page"),bool(j,"vertical"),bool(j,"compact"),
            DisplayElements.TextAlign.parse(str(j,"textAlign")),bool(j,"wrap"),(float)decimal(j,"textScale",1.0),new DisplayElements.Options(str(j,"group"),bool(j,"locked"),bool(j,"hidden"),j.has("background")?integer(j,"background"):-1,j.has("border")?integer(j,"border"):-1,j.has("actionPage")?integer(j,"actionPage"):-1));
    }
    private static String str(JsonObject j,String k){return j.has(k)?j.get(k).getAsString():"";}
    private static int integer(JsonObject j,String k){return j.has(k)?j.get(k).getAsBigDecimal().intValueExact():0;}
    private static double decimal(JsonObject j,String k,double fallback){return j.has(k)&&j.get(k).isJsonPrimitive()&&j.get(k).getAsJsonPrimitive().isNumber()?j.get(k).getAsDouble():fallback;}
    private static boolean bool(JsonObject j,String k){return j.has(k)&&j.get(k).isJsonPrimitive()&&j.get(k).getAsJsonPrimitive().isBoolean()&&j.get(k).getAsBoolean();}
    private ElementJson(){}
}
