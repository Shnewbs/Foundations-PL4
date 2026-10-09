package net.foundations.pl4.compat;
import java.util.function.*;
public record StreamCodec<B,T>(BiConsumer<B,T> writer,Function<B,T> reader){
 public void encode(B buffer,T value){writer.accept(buffer,value);}
 public T decode(B buffer){return reader.apply(buffer);}
 public static <B,T> StreamCodec<B,T> of(BiConsumer<B,T> writer,Function<B,T> reader){return new StreamCodec<>(writer,reader);}
}
