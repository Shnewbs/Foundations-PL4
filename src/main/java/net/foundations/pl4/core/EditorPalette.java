package net.foundations.pl4.core;

import java.util.List;

/** Small deterministic colour palette for the PL4 display editor. Hex remains authoritative; presets only fill it. */
public final class EditorPalette {
    public record Swatch(String name,int rgb) { public Swatch { rgb&=0xFFFFFF; } }
    public static final int DEFAULT=0x79D3FF;
    public static final List<Swatch> PRESETS=List.of(
        new Swatch("PL4 cyan",DEFAULT),new Swatch("Ice",0xB8F1FF),new Swatch("Blue",0x5EA7FF),new Swatch("Navy",0x45658E),
        new Swatch("Teal",0x44D6C5),new Swatch("Green",0x73D86B),new Swatch("Lime",0xB6E568),new Swatch("Yellow",0xFFE66B),
        new Swatch("Amber",0xFFC45C),new Swatch("Orange",0xFF914D),new Swatch("Red",0xFF6B6B),new Swatch("Rose",0xFF7EAE),
        new Swatch("Magenta",0xE06BFF),new Swatch("Purple",0xA77BFF),new Swatch("Lavender",0xC9B7FF),new Swatch("White",0xFFFFFF),
        new Swatch("Silver",0xC7D1D8),new Swatch("Grey",0x89959D),new Swatch("Dark grey",0x48545B),new Swatch("Black",0x111820),
        new Swatch("Copper",0xD7865A),new Swatch("Gold",0xE5BF62),new Swatch("Mint",0x9DE6C2),new Swatch("Aqua",0x69E4FF)
    );
    public static String hex(int rgb){return String.format("%06X",rgb&0xFFFFFF);}
    public static int parse(String text,int fallback){
        if(text==null)return fallback&0xFFFFFF;
        try{return Integer.parseUnsignedInt(text.trim().replace("#",""),16)&0xFFFFFF;}catch(RuntimeException ignored){return fallback&0xFFFFFF;}
    }
    private EditorPalette(){}
}
