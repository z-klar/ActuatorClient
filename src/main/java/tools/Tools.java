package tools;

public class Tools {

    /**
     *
     */
    public static String getPadding(int level) {
        String s = "";
        for(int i=0; i<level; i++) s += "    ";
        return s;
    }

}
