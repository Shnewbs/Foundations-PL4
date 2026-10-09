package net.foundations.pl4.core;

import java.util.*;
import java.util.function.Function;

/** Bounded, fail-closed automation rules; no Minecraft or provider dependencies. */
public final class SignalRules {
    public static final int MAX_STATEMENTS=16;
    public static final Set<String> OPERATORS=Set.of(">=",">","<","<=","=","!=");
    public record Statement(UUID id,String reader,String key,String operator,double threshold) {
        public Statement {
            Objects.requireNonNull(id);
            if(reader==null||key==null||reader.length()>64||key.length()>128||!OPERATORS.contains(operator)||!Double.isFinite(threshold)||Math.abs(threshold)>1e15)
                throw new IllegalArgumentException("Invalid statement");
        }
    }
    public static boolean compare(Double value,double threshold,String operator){
        if(value==null||!Double.isFinite(value)||!Double.isFinite(threshold))return false;
        return switch(operator){case ">="->value>=threshold;case ">"->value>threshold;case "<"->value<threshold;case "<="->value<=threshold;case "="->value==threshold;case "!="->value!=threshold;default->false;};
    }
    public static int signal(List<Statement> statements,boolean all,int strength,Function<Statement,Double> lookup){
        if(statements.isEmpty()||statements.size()>MAX_STATEMENTS)return 0;
        boolean result=all;
        for(Statement statement:statements){boolean pass=compare(lookup.apply(statement),statement.threshold(),statement.operator());if(all)result&=pass;else result|=pass;}
        return result?net.foundations.pl4.compat.PortMath.clamp(strength,0,15):0;
    }
    private SignalRules(){}
}
