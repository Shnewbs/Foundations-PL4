package net.foundations.pl4.core;

/** Predictable category selection and quick access to the welcome/tutorial chapters. */
public final class GuideNavigation {
    public static GuideBook.Chapter first(GuideBook book,String category) {
        return book.chapters().stream().filter(c->c.category().equals(category)).findFirst().orElse(book.chapters().get(0));
    }
    public static GuideBook.Chapter byId(GuideBook book,String id) {
        return book.chapters().stream().filter(c->c.id().equals(id)).findFirst().orElse(book.chapters().get(0));
    }
    public static GuideBook.Chapter section(GuideBook book,String category,GuideBook.Chapter current) {
        return current!=null&&current.category().equals(category)?current:first(book,category);
    }
    public static String heading(String category) {
        return switch(category){case "start"->"WELCOME / TUTORIALS";case "network"->"NETWORK REFERENCE";
            case "display"->"DISPLAYS / EDITING";default->"WORKSHOP / REFERENCE";};
    }
    private GuideNavigation(){}
}
