package net.foundations.pl4.compat;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
public final class PortInteractions {
 public static EnumActionResult sidedSuccess(boolean client){return client?EnumActionResult.SUCCESS:EnumActionResult.SUCCESS;}
 public static <T> ActionResult<T> sidedSuccess(T result,boolean client){return new ActionResult<>(sidedSuccess(client),result);}
 private PortInteractions(){}
}
