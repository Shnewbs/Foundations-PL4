package net.foundations.pl4.compat;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
public final class PortInteractions {
 public static ActionResultType sidedSuccess(boolean client){return client?ActionResultType.SUCCESS:ActionResultType.CONSUME;}
 public static <T> ActionResult<T> sidedSuccess(T result,boolean client){return new ActionResult<>(sidedSuccess(client),result);}
 private PortInteractions(){}
}
