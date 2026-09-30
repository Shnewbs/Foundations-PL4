package net.foundations.pl4.core;

import java.util.*;

/** Atomic, revision-fenced display edits. A drag sends one final update, never one packet per pixel. */
public final class LayoutTransactions {
    public record State(List<DisplayElements.Spec> elements,DisplayElements.Mode mode,int page,long revision){public State{elements=List.copyOf(elements);}}
    public record Result(boolean accepted,State state,String message){}
    public static Result apply(State before,long expected,String action,UUID target,DisplayElements.Spec element,String value){
        if(expected!=before.revision)return new Result(false,before,"Screen changed; review the current layout and retry.");
        if(before.revision==Long.MAX_VALUE)return new Result(false,before,"Layout revision is exhausted.");
        List<DisplayElements.Spec> list=new ArrayList<>(before.elements);DisplayElements.Mode mode=before.mode;int page=before.page;
        int index=-1;for(int i=0;i<list.size();i++)if(list.get(i).id().equals(target)){index=i;break;}
        try{switch(action){
            case "add" -> {if(element==null||index>=0||list.size()>=DisplayElements.MAX_ELEMENTS)return fail(before,"Maximum 32 elements or duplicate element ID.");list.add(element);mode=DisplayElements.Mode.CUSTOM;page=element.page();}
            case "update" -> {if(index<0||element==null||!element.id().equals(target))return fail(before,"Element no longer exists.");list.set(index,element);mode=DisplayElements.Mode.CUSTOM;page=element.page();}
            case "delete" -> {
                if(value!=null&&!value.isBlank()){
                    var ids=Arrays.stream(value.split(",")).map(String::trim).filter(s->!s.isBlank()).map(UUID::fromString).collect(java.util.stream.Collectors.toSet());
                    if(ids.isEmpty()||list.stream().noneMatch(e->ids.contains(e.id())))return fail(before,"Select an element.");
                    list.removeIf(e->ids.contains(e.id()));
                }else{if(index<0)return fail(before,"Element no longer exists.");list.remove(index);}
                mode=DisplayElements.Mode.CUSTOM;
            }
            case "forward","backward" -> {if(index<0)return fail(before,"Select an element.");int next=index+(action.equals("forward")?1:-1);if(next>=0&&next<list.size())Collections.swap(list,index,next);}
            case "clear" -> {list.clear();mode=DisplayElements.Mode.CUSTOM;}
            case "mode" -> {mode=DisplayElements.Mode.valueOf(value);}
            case "page" -> {page=Integer.parseInt(value);if(page<0||page>=DisplayElements.MAX_PAGES)return fail(before,"Page out of range.");}
            default -> {return fail(before,"Unknown display action.");}
        }}catch(IllegalArgumentException ex){return fail(before,"Invalid display setting.");}
        if(list.stream().map(DisplayElements.Spec::id).distinct().count()!=list.size())return fail(before,"Duplicate element ID.");
        if(list.stream().map(DisplayElements.Spec::reader).filter(s->!s.isBlank()).distinct().count()>8)return fail(before,"Maximum eight explicit reader bindings.");
        return new Result(true,new State(list,mode,page,before.revision+1),"");
    }
    /** Whole-layout snapshot restore for client-side undo/redo and future layout import; no JSON here (this file compiles standalone). */
    public static Result applyReplace(State before,long expected,List<DisplayElements.Spec> elements){
        if(expected!=before.revision)return new Result(false,before,"Screen changed; review the current layout and retry.");
        if(before.revision==Long.MAX_VALUE)return new Result(false,before,"Layout revision is exhausted.");
        if(elements.size()>DisplayElements.MAX_ELEMENTS)return fail(before,"Maximum 32 elements.");
        List<DisplayElements.Spec> list=new ArrayList<>(elements);
        if(list.stream().map(DisplayElements.Spec::id).distinct().count()!=list.size())return fail(before,"Duplicate element ID.");
        if(list.stream().map(DisplayElements.Spec::reader).filter(s->!s.isBlank()).distinct().count()>8)return fail(before,"Maximum eight explicit reader bindings.");
        return new Result(true,new State(list,DisplayElements.Mode.CUSTOM,before.page,before.revision+1),"");
    }
    private static Result fail(State s,String reason){return new Result(false,s,reason);}
    private LayoutTransactions(){}
}
