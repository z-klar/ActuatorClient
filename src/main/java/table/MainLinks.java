package table;

public class MainLinks {
    public String Name;
    public String Href;
    public boolean Templated;

    public MainLinks(String name, String href, boolean templ) {
        Name = name;
        Href = href;
        Templated = templ;
    }

    public Object[] toObject() {
        return new Object[]{Name, Href, Templated};
    }
}
