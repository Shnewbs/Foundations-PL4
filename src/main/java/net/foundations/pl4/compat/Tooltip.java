package net.foundations.pl4.compat;
import net.minecraft.util.text.ITextComponent;
public record Tooltip(ITextComponent text){public static Tooltip create(ITextComponent text){return new Tooltip(text);}}
