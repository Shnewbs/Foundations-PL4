package net.foundations.pl4.core;

import java.util.*;

/** Bounded data-only chapter model shared by resource validation and offline tests. */
public record GuideBook(String title,String edition,List<GuideBook.Chapter> chapters) {
    public static final List<String> CATEGORIES=List.of("start","network","display","reference");
    public record Section(String heading,String body) {
        public Section { heading=text(heading,100);body=text(body,8192); }
    }
    public record Chapter(String id,String category,String title,String summary,String icon,List<Section> sections) {
        public Chapter {
            if(id==null||!id.matches("[a-z0-9_]{1,64}")||!CATEGORIES.contains(category))throw new IllegalArgumentException("Invalid chapter identity/category");
            title=text(title,100);summary=text(summary,240);
            if(icon==null||!icon.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))throw new IllegalArgumentException("Invalid icon identifier");
            sections=List.copyOf(sections);
            if(sections.isEmpty()||sections.size()>32)throw new IllegalArgumentException("Invalid section count");
        }
        public boolean matches(String query){
            String q=query==null?"":query.strip().toLowerCase(Locale.ROOT);
            if(q.isEmpty())return true;
            return (title+" "+summary+" "+id+" "+sections.stream().map(s->s.heading+" "+s.body).reduce("",(a,b)->a+" "+b)).toLowerCase(Locale.ROOT).contains(q);
        }
    }
    public GuideBook {
        title=text(title,100);edition=text(edition,100);chapters=List.copyOf(chapters);
        if(chapters.isEmpty()||chapters.size()>128)throw new IllegalArgumentException("Invalid chapter count");
        Set<String> ids=new HashSet<>();for(var c:chapters)if(!ids.add(c.id))throw new IllegalArgumentException("Duplicate chapter "+c.id);
    }
    public List<Chapter> search(String category,String query,Set<String> saved,boolean savedOnly){
        boolean searching=query!=null&&!query.isBlank();
        return chapters.stream().filter(c->(searching||c.category.equals(category))&&c.matches(query)&&(!savedOnly||saved.contains(c.id))).toList();
    }
    private static String text(String value,int max){
        if(value==null||value.length()>max)throw new IllegalArgumentException("Invalid guide text length");
        return value.replaceAll("[\\p{Cntrl}&&[^\\n\\t]]","").replace("§","");
    }
}
