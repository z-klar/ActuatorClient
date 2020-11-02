package tools;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class Tools {

    /**
     *
     */
    public static String getPadding(int level) {
        String s = "";
        for(int i=0; i<level; i++) s += "    ";
        return s;
    }

    /**
     *
     * @return
     */
    public  static Date getClassBuildTime() {
        Date d = null;
        Class<?> currentClass = new Object() {}.getClass().getEnclosingClass();
        URL resource = currentClass.getResource(currentClass.getSimpleName() + ".class");
        if (resource != null) {
            if (resource.getProtocol().equals("file")) {
                try {
                    d = new Date(new File(resource.toURI()).lastModified());
                } catch (URISyntaxException ignored) { }
            } else if (resource.getProtocol().equals("jar")) {
                String path = resource.getPath();
                d = new Date( new File(path.substring(5, path.indexOf("!"))).lastModified() );
            } else if (resource.getProtocol().equals("zip")) {
                String path = resource.getPath();
                File jarFileOnDisk = new File(path.substring(0, path.indexOf("!")));
                try(JarFile jf = new JarFile (jarFileOnDisk)) {
                    ZipEntry ze = jf.getEntry (path.substring(path.indexOf("!") + 2));//Skip the ! and the /
                    long zeTimeLong = ze.getTime ();
                    Date zeTimeDate = new Date(zeTimeLong);
                    d = zeTimeDate;
                } catch (IOException |RuntimeException ignored) { }
            }
        }
        return d;
    }

    // region POPUP handlers
    public static void PrepareLoggersPopups(JPopupMenu menu, String[] labels, Object form ) {
        // popup for LOGGER table
        for(String s : labels) {
            JMenuItem mnuPpLogOFF = new JMenuItem(s);
            mnuPpLogOFF.addActionListener((ActionListener) form);
            menu.add(mnuPpLogOFF);
        }
    }


}
