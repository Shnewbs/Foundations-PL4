package net.foundations.pl4.core;

import java.util.*;

/** Atomic, revision-fenced display edits. A drag sends one final update, never one packet per pixel. */
public final class LayoutTransactions {
    public record State(List<DisplayElements.Spec> elements,DisplayElements.Mode mode,int page,long revision){public State{elements=List.copyOf(elements);}}
    public record Result(boolean accepted,State state,String message){}
    public static Result apply(State before,long expected,String action,UUID target,DisplayElements.Spec element,String value){
        if(expected!=before.revision)return new Result(false,before,"Screen changed; review the current layout and retry.");
        if(before.revision==Long.MAX_VALUE)return new Result(false,before,"Layout revision is exhausted.");
        if(action.equals("forward")||action.equals("backward"))return applyLayers(before,expected,"layer_"+action,List.of(target));
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
    public static boolean arrangement(String action){return switch(action){
        case "align_left","align_right","align_top","align_bottom","align_hcenter","align_vcenter","distribute_x","distribute_y" -> true;
        default -> false;
    };}
    /** Arrange only existing elements on the current page; canvas bounds come from the server. */
    public static Result applyArrange(State before,long expected,String action,List<UUID> ids,int width,int height){
        if(expected!=before.revision)return fail(before,"Screen changed; review the current layout and retry.");
        if(before.revision==Long.MAX_VALUE)return fail(before,"Layout revision is exhausted.");
        if(!arrangement(action))return fail(before,"Unknown arrangement.");
        if(ids.isEmpty()||ids.size()>DisplayElements.MAX_ELEMENTS||new HashSet<>(ids).size()!=ids.size())return fail(before,"Select distinct elements on this page.");
        if(width<8||height<9||width>DisplayElements.MAX_CANVAS||height>DisplayElements.MAX_CANVAS)return fail(before,"Invalid canvas dimensions.");
        Set<UUID> selected=new HashSet<>(ids);
        List<DisplayElements.Spec> picked=before.elements.stream().filter(e->selected.contains(e.id())).toList();
        if(picked.size()!=ids.size()||picked.stream().anyMatch(e->e.page()!=before.page))return fail(before,"Selection changed; select elements on this page.");
        if(picked.stream().anyMatch(e->e.bounds().right()>width||e.bounds().bottom()>height))return fail(before,"Selection is outside the current canvas.");
        boolean horizontal=action.equals("distribute_x"),distributed=horizontal||action.equals("distribute_y");
        Map<UUID,DisplayElements.Rect> arranged=new HashMap<>();
        int left=picked.stream().mapToInt(e->e.bounds().x()).min().orElseThrow(),top=picked.stream().mapToInt(e->e.bounds().y()).min().orElseThrow();
        int right=picked.stream().mapToInt(e->e.bounds().right()).max().orElseThrow(),bottom=picked.stream().mapToInt(e->e.bounds().bottom()).max().orElseThrow();
        if(distributed){
            if(picked.size()<3)return fail(before,"Select at least three elements to distribute.");
            List<DisplayElements.Spec> ordered=new ArrayList<>(picked);
            ordered.sort(Comparator.comparingInt(e->horizontal?e.bounds().x():e.bounds().y()));
            // Keep the outer elements fixed and divide the available space into equal edge gaps.
            int first=horizontal?ordered.getFirst().bounds().x():ordered.getFirst().bounds().y();
            var last=ordered.getLast().bounds();int end=horizontal?last.right():last.bottom();
            int occupied=ordered.stream().mapToInt(e->horizontal?e.bounds().width():e.bounds().height()).sum();
            int free=end-first-occupied;
            if(free<0)return fail(before,"Not enough space for non-overlapping elements.");
            int used=0;
            for(int i=0;i<ordered.size();i++){
                var e=ordered.get(i);var b=e.bounds();int coordinate=first+used+(int)Math.round((double)free*i/(ordered.size()-1));
                arranged.put(e.id(),new DisplayElements.Rect(horizontal?coordinate:b.x(),horizontal?b.y():coordinate,b.width(),b.height()));
                used+=horizontal?b.width():b.height();
            }
        }else{
            if(picked.size()==1){left=0;top=0;right=width;bottom=height;}
            for(var e:picked){var b=e.bounds();int x=b.x(),y=b.y();switch(action){
                case "align_left" -> x=left;case "align_right" -> x=right-b.width();
                case "align_hcenter" -> x=left+(right-left-b.width())/2;
                case "align_top" -> y=top;case "align_bottom" -> y=bottom-b.height();
                case "align_vcenter" -> y=top+(bottom-top-b.height())/2;
                default -> {return fail(before,"Unknown arrangement.");}
            }arranged.put(e.id(),new DisplayElements.Rect(x,y,b.width(),b.height()));}
        }
        List<DisplayElements.Spec> result=before.elements.stream().map(e->selected.contains(e.id())?e.bounds(arranged.get(e.id())):e).toList();
        if(result.equals(before.elements))return fail(before,"Selection is already arranged.");
        return new Result(true,new State(result,DisplayElements.Mode.CUSTOM,before.page,before.revision+1),"");
    }
    public static Result applyMove(State before,long expected,List<UUID> ids,int dx,int dy,int width,int height){
        if(expected!=before.revision)return fail(before,"Screen changed; review the current layout and retry.");
        if(before.revision==Long.MAX_VALUE)return fail(before,"Layout revision is exhausted.");
        if(ids.isEmpty()||ids.size()>DisplayElements.MAX_ELEMENTS||new HashSet<>(ids).size()!=ids.size())return fail(before,"Select distinct elements on this page.");
        if(Math.abs((long)dx)>DisplayElements.MAX_CANVAS||Math.abs((long)dy)>DisplayElements.MAX_CANVAS)return fail(before,"Invalid movement.");
        Set<UUID> selected=new HashSet<>(ids);var picked=before.elements.stream().filter(e->selected.contains(e.id())).toList();
        if(picked.size()!=ids.size()||picked.stream().anyMatch(e->e.page()!=before.page))return fail(before,"Selection changed; select elements on this page.");
        try{
            var moved=EditorSelection.move(picked,dx,dy,width,height);
            Map<UUID,DisplayElements.Spec> edits=new HashMap<>();for(var e:moved)edits.put(e.id(),e);
            var result=before.elements.stream().map(e->edits.getOrDefault(e.id(),e)).toList();
            if(result.equals(before.elements))return fail(before,"Selection has not moved.");
            return new Result(true,new State(result,DisplayElements.Mode.CUSTOM,before.page,before.revision+1),"");
        }catch(IllegalArgumentException ex){return fail(before,ex.getMessage());}
    }
    public static Result applyPaste(State before,long expected,List<DisplayElements.Spec> copies,int width,int height){
        if(copies.isEmpty()||copies.stream().anyMatch(e->e.page()!=before.page||e.bounds().right()>width||e.bounds().bottom()>height))return fail(before,"Paste must fit the current page and canvas.");
        if(width<8||height<9||width>DisplayElements.MAX_CANVAS||height>DisplayElements.MAX_CANVAS)return fail(before,"Invalid canvas dimensions.");
        var all=new ArrayList<>(before.elements);all.addAll(copies);
        return applyReplace(before,expected,all);
    }
    public static boolean layerAction(String action){return switch(action){
        case "layer_front","layer_back","layer_forward","layer_backward" -> true;default -> false;
    };}
    /** Stable page-local layer changes; other pages retain their exact global list slots. */
    public static Result applyLayers(State before,long expected,String action,List<UUID> ids){
        if(expected!=before.revision)return fail(before,"Screen changed; review the current layout and retry.");
        if(before.revision==Long.MAX_VALUE)return fail(before,"Layout revision is exhausted.");
        if(!layerAction(action))return fail(before,"Unknown layer action.");
        if(ids.isEmpty()||ids.size()>DisplayElements.MAX_ELEMENTS||new HashSet<>(ids).size()!=ids.size())return fail(before,"Select distinct elements on this page.");
        Set<UUID> selected=new HashSet<>(ids);
        var page=new ArrayList<>(before.elements.stream().filter(e->e.page()==before.page).toList());
        if(page.stream().filter(e->selected.contains(e.id())).count()!=ids.size())return fail(before,"Selection changed; select elements on this page.");
        switch(action){
            case "layer_front","layer_back" -> {
                boolean front=action.equals("layer_front");var ordered=new ArrayList<DisplayElements.Spec>();
                for(var e:page)if(selected.contains(e.id())!=front)ordered.add(e);
                for(var e:page)if(selected.contains(e.id())==front)ordered.add(e);
                page=ordered;
            }
            case "layer_forward" -> {
                for(int i=page.size()-2;i>=0;i--)if(selected.contains(page.get(i).id())&&!selected.contains(page.get(i+1).id()))Collections.swap(page,i,i+1);
            }
            case "layer_backward" -> {
                for(int i=1;i<page.size();i++)if(selected.contains(page.get(i).id())&&!selected.contains(page.get(i-1).id()))Collections.swap(page,i,i-1);
            }
            default -> {return fail(before,"Unknown layer action.");}
        }
        var result=new ArrayList<>(before.elements);int next=0;
        for(int i=0;i<result.size();i++)if(result.get(i).page()==before.page)result.set(i,page.get(next++));
        if(result.equals(before.elements))return fail(before,"Selection is already at that layer.");
        return new Result(true,new State(result,DisplayElements.Mode.CUSTOM,before.page,before.revision+1),"");
    }
    private static Result fail(State s,String reason){return new Result(false,s,reason);}
    private LayoutTransactions(){}
}
