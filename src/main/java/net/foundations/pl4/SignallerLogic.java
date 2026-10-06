package net.foundations.pl4;

import java.util.*;
import net.foundations.pl4.core.SignalRules;

final class SignallerLogic {
    static Part reader(List<NetworkEngine.Ref> readers,String selection){
        if(selection.isEmpty())return readers.isEmpty()?null:readers.getFirst().part();
        for(var ref:readers)if(ref.part().identity.toString().equals(selection))return ref.part();
        var named=readers.stream().filter(ref->ref.part().label.equals(selection)).toList();
        return named.size()==1?named.getFirst().part():null;
    }
    static Double value(List<NetworkEngine.Ref> readers,SignalRules.Statement statement){
        Part reader=reader(readers,statement.reader());if(reader==null)return null;
        return reader.rows.stream().filter(row->statement.key().isEmpty()||row.key().equals(statement.key())).map(Part.Row::value).findFirst().orElse(null);
    }
    static int evaluate(Part p,List<NetworkEngine.Ref> readers){
        if(p.statements.isEmpty()){
            Part reader=reader(readers,p.selected);
            Double value=reader==null?null:reader.rows.stream().filter(row->p.metric.isEmpty()||row.key().equals(p.metric)).map(Part.Row::value).findFirst().orElse(null);
            return SignalRules.compare(value,p.threshold,p.comparison)?p.signalStrength:0;
        }
        return SignalRules.signal(p.statements,p.statementsAll,p.signalStrength,s->value(readers,s));
    }
    private SignallerLogic(){}
}
