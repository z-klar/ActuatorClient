import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import dto.RestCallOutput;
import model.ActuatorLink;
import model.CommonOutput;
import model.LoggerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.JsonProcessing;
import service.RestCallService;
import table.*;
import tools.GlobalData;
import tools.Tools;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.*;

public class frmMain implements ActionListener {
    private final Logger log = LoggerFactory.getLogger(JsonProcessing.class);

    private JPanel mainPanel;
    private JTabbedPane tabbedPane1;
    private JTextField txCfgActuatorUri;
    private JButton btnMainGetOverview;
    private JButton btnClearLog;
    private JList lbLog;
    private JList lbPrivateLog;
    private JButton btnClearPrivateLog;
    private JButton btnDisplayMainLinks;
    private JTable tblData1;
    private JButton btnGetLoggers;
    private JButton btnDisplayLoggers;
    private JCheckBox chkLogujVerbose;
    private JLabel lbBuildInfo;
    private JButton btnGetMetrics;
    private JButton btnDisplayMetrics;

    private DefaultListModel<String> dlmLog = new DefaultListModel<>();
    private DefaultListModel<String> dlmPrivateLog = new DefaultListModel<>();

    private RestCallService restService = new RestCallService();
    private JsonProcessing jsonProcessing = new JsonProcessing();
    private GlobalData globalData = new GlobalData();

    private String currentDisplay = "";
    private int TableMouseClickRow;

    JPopupMenu popLoggers = new JPopupMenu("pop_loggers");
    private String [] popLoggersLabels = {"Set to OFF", "Set to ERROR", "Set to WARN",
                                          "Set to INFO", "Set to DEBUG", "Set to TRACE"};
    private String [] popLoggersCommands = {"OFF", "ERROR", "WARN", "INFO", "DEBUG", "TRACE"};
    JPopupMenu popMain = new JPopupMenu("pop_main");
    private String [] popMainLabels = {"Send GET Request"};
    JPopupMenu popMetrics = new JPopupMenu("pop_metrics");
    private String [] popMetricsLabels = {"GET This Metrics"};

    /**
     *
     */
    public frmMain() {
        lbLog.setModel(dlmLog);
        lbPrivateLog.setModel(dlmPrivateLog);
        Tools.PrepareLoggersPopups(popLoggers, popLoggersLabels, this);
        Tools.PrepareLoggersPopups(popMain, popMainLabels, this);
        Tools.PrepareLoggersPopups(popMetrics, popMetricsLabels, this);

        btnClearLog.addActionListener(e -> dlmLog.clear());
        btnMainGetOverview.addActionListener(e -> getAllLinks());
        btnClearPrivateLog.addActionListener(e -> dlmPrivateLog.clear());
        btnDisplayMainLinks.addActionListener(e -> DisplayMainLinks());
        btnGetLoggers.addActionListener(e -> GetLoggers());
        btnDisplayLoggers.addActionListener(e -> DisplayLoggers());
        tblData1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                int button = e.getButton();
                if (button == MouseEvent.BUTTON3) {
                    int selRow = tblData1.rowAtPoint(e.getPoint());
                    ProcessMouseClick(selRow, e.getX(), e.getY());
                }
            }
        });
        lbBuildInfo.setText("Build Date: " + Tools.getClassBuildTime().toString());
        btnGetMetrics.addActionListener(e -> GetMetrics());
        btnDisplayMetrics.addActionListener(e -> DisplayMetrics());
    }

    // region POPUP handlers

    /**
     *
     * @param e
     */
    public void actionPerformed(ActionEvent e) {
        JMenuItem source = (JMenuItem) (e.getSource());
        JPopupMenu comp = (JPopupMenu) source.getComponent().getParent();
        String mnuLabel = ((JPopupMenu) source.getComponent().getParent()).getLabel();
        log.info("Menu label: " + mnuLabel);
        String label = source.getText();
        switch (mnuLabel) {
            case "pop_loggers":
                UpdateLogLevel(popLoggersCommands[FindLabel(label, popLoggersLabels)]);
                break;
            case "pop_main":
                if (label.contains(popMainLabels[0])) {
                    SendLinkGet();
                }
                break;
            case "pop_metrics":
                if (label.contains(popMetricsLabels[0])) {
                    SendGetMetric();
                }
                break;
        }
    }
    private int FindLabel(String label, String [] labels) {
        for(int i=0; i<labels.length; i++) {
            if(labels[i].equals(label)) return(i);
        }
        return(1000);
    }

    /**
     * @param row
     */
    private void ProcessMouseClick(int row, int x, int y) {
        if (currentDisplay.contains("links")) {
            TableMouseClickRow = row;
            popMain.show(tblData1, x, y);
        } else if (currentDisplay.contains("loggers")) {
            TableMouseClickRow = row;
            popLoggers.show(tblData1, x, y);
        } else if (currentDisplay.contains("metrics")) {
            TableMouseClickRow = row;
            popMetrics.show(tblData1, x, y);
        }
    }
    // endregion

    private void SendGetMetric() {
        String url = txCfgActuatorUri.getText() + "/actuator/metrics/";
        url += globalData.getActuatorMetrics().get(TableMouseClickRow);
        Map<String, String> props = new HashMap<>();
        props.put("Accept", "*/*");
        log.info("GET single metrics:");
        log.info(url);
        RestCallOutput ro = restService.SendRestApiRequest("GET", props, null, url);
        log.info("Result code: " + ro.getResultCode());
        Loguj("Result: " + ro.getResultCode());
        if (ro.getResultCode() < 300) {
            jsonProcessing.ParseJsonToLog(ro.getDataMsg(), dlmLog);
        } else {
            Loguj(ro.getErrorMsg());
        }
    }
    /**
     *
     */
    private void DisplayMetrics() {
        Vector<Metrics> rows = new Vector<>();
        for (String m : globalData.getActuatorMetrics()) {
            rows.add(new Metrics(m));
        }
        MetricsModel model = new MetricsModel(rows);
        tblData1.setModel(model);
        currentDisplay = "metrics";
    }

    /**
     *
     */
    private void GetMetrics() {
        String url = txCfgActuatorUri.getText() + "/actuator/metrics";
        Map<String, String> props = new HashMap<>();
        props.put("Accept", "*/*");
        log.info("GET actuator metrics:");
        log.info(url);
        RestCallOutput ro = restService.SendRestApiRequest("GET", props, null, url);
        log.info("Result code: " + ro.getResultCode());
        Loguj("Result: " + ro.getResultCode());
        if (ro.getResultCode() < 300) {
            CommonOutput co = jsonProcessing.GetMetricsList(ro.getDataMsg());
            globalData.setActuatorMetrics((ArrayList<String>) co.getResult());
        } else {
            Loguj(ro.getErrorMsg());
        }
    }

    /**
     *
     */
    private void SendLinkGet() {
        String url = globalData.getActuatorLinks().get(TableMouseClickRow).getHref();
        Map<String, String> props = new HashMap<>();
        props.put("Accept", "*/*");
        log.info("GET actuator link:");
        log.info(url);
        RestCallOutput ro = restService.SendRestApiRequest("GET", props, null, url);
        log.info("Result code: " + ro.getResultCode());
        Loguj("Result: " + ro.getResultCode());
        if (ro.getResultCode() < 300) {
            jsonProcessing.ParseJsonToLog(ro.getDataMsg(), dlmLog);
        } else {
            Loguj(ro.getErrorMsg());
        }
    }

    /**
     * @param newLevel
     */
    private void UpdateLogLevel(String newLevel) {
        String url = txCfgActuatorUri.getText() + "/actuator/loggers/";
        url += globalData.getLoggerRecords().get(TableMouseClickRow).getName();
        Map<String, String> props = new HashMap<>();
        props.put("Content-Type", "application/json");
        String json = "{ \"configuredLevel\" : \"" + newLevel + "\" }";
        log.info("Update LOG level:");
        log.info(url);
        log.info(json);
        RestCallOutput ro = restService.SendRestApiRequest("POST", props, json, url);
        log.info("Result code: " + ro.getResultCode());
        if (ro.getResultCode() < 300) {
            GetLoggers();
            DisplayLoggers();
        } else {
            log.info(ro.getErrorMsg());
        }
    }

    /**
     *
     */
    private void DisplayLoggers() {
        Vector<Loggers> rows = new Vector<>();
        for (LoggerRecord lr : globalData.getLoggerRecords()) {
            rows.add(new Loggers(lr.getName(), lr.getConfiguredLevel(), lr.getEffectiveLevel()));
        }
        LoggersModel model = new LoggersModel(rows);
        tblData1.setModel(model);
        currentDisplay = "loggers";
    }

    /**
     *
     */
    private void GetLoggers() {
        String url = txCfgActuatorUri.getText() + "/actuator/loggers";
        RestCallOutput ro = restService.getAllLinks(url, true);
        dlmPrivateLog.addElement("ResultCode=" + ro.getResultCode());
        if (chkLogujVerbose.isSelected()) Loguj(ro.getDataMsg());
        CommonOutput co = jsonProcessing.GetAllLoggers(ro.getDataMsg());
        globalData.setLoggerRecords((ArrayList<LoggerRecord>) co.getResult());
    }

    /**
     *
     */
    private void DisplayMainLinks() {
        Vector<MainLinks> rows = new Vector<>();
        for (ActuatorLink link : globalData.getActuatorLinks()) {
            rows.add(new MainLinks(link.getName(), link.getHref(), link.isTemplated()));
        }
        MainLinksModel model = new MainLinksModel(rows);
        tblData1.setModel(model);
        currentDisplay = "links";
    }

    /**
     *
     */
    private void getAllLinks() {
        String url = txCfgActuatorUri.getText() + "/actuator";
        RestCallOutput ro = restService.getAllLinks(url, true);
        dlmPrivateLog.addElement("ResultCode=" + ro.getResultCode());
        if (chkLogujVerbose.isSelected()) Loguj(ro.getDataMsg());
        CommonOutput co = jsonProcessing.GetAllActuatorLinks(ro.getDataMsg());
        globalData.setActuatorLinks((ArrayList<ActuatorLink>) co.getResult());
    }

    /**
     * @param line
     */
    private void Loguj(String line) {
        final int ROW_LEN = 110;
        ArrayList<String> alBuff = new ArrayList<>();
        String spom1 = line;
        while (spom1.length() > ROW_LEN) {
            alBuff.add(spom1.substring(0, ROW_LEN));
            spom1 = spom1.substring(ROW_LEN);
        }
        alBuff.add(spom1);
        for (String s : alBuff) dlmLog.addElement(s);
    }

    /**
     * @return
     */
    private Date getBuildDate() {
        try {
            Date BuildDate = new Date(new File(getClass().getClassLoader()
                    .getResource(getClass().getCanonicalName()
                            .replace('.', '/') + ".class").toURI()).lastModified());
            return BuildDate;
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame("Spring Actuator Client");
        frame.setContentPane(new frmMain().mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
        frame.setSize(1200, 800);
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1 = new JTabbedPane();
        mainPanel.add(tabbedPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(3, 4, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Main", panel1);
        btnMainGetOverview = new JButton();
        btnMainGetOverview.setText("Get All Links");
        panel1.add(btnMainGetOverview, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1, new GridConstraints(2, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        lbPrivateLog = new JList();
        Font lbPrivateLogFont = this.$$$getFont$$$("Courier New", -1, -1, lbPrivateLog.getFont());
        if (lbPrivateLogFont != null) lbPrivateLog.setFont(lbPrivateLogFont);
        scrollPane1.setViewportView(lbPrivateLog);
        btnClearPrivateLog = new JButton();
        btnClearPrivateLog.setText("Clear Log");
        panel1.add(btnClearPrivateLog, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnGetLoggers = new JButton();
        btnGetLoggers.setText("Get Loggers");
        panel1.add(btnGetLoggers, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnGetMetrics = new JButton();
        btnGetMetrics.setText("Get Metrics");
        panel1.add(btnGetMetrics, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Data", panel2);
        btnDisplayMainLinks = new JButton();
        btnDisplayMainLinks.setText("Main Links");
        panel2.add(btnDisplayMainLinks, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel2.add(spacer2, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JScrollPane scrollPane2 = new JScrollPane();
        panel2.add(scrollPane2, new GridConstraints(1, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tblData1 = new JTable();
        scrollPane2.setViewportView(tblData1);
        btnDisplayLoggers = new JButton();
        btnDisplayLoggers.setText("Loggers");
        panel2.add(btnDisplayLoggers, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnDisplayMetrics = new JButton();
        btnDisplayMetrics.setText("Metrics");
        panel2.add(btnDisplayMetrics, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(4, 3, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Config", panel3);
        final JLabel label1 = new JLabel();
        Font label1Font = this.$$$getFont$$$(null, -1, 14, label1.getFont());
        if (label1Font != null) label1.setFont(label1Font);
        label1.setText("Actuator URI:");
        panel3.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel3.add(spacer3, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        panel3.add(spacer4, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        txCfgActuatorUri = new JTextField();
        Font txCfgActuatorUriFont = this.$$$getFont$$$(null, -1, 14, txCfgActuatorUri.getFont());
        if (txCfgActuatorUriFont != null) txCfgActuatorUri.setFont(txCfgActuatorUriFont);
        txCfgActuatorUri.setText("http://localhost:9999");
        panel3.add(txCfgActuatorUri, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        chkLogujVerbose = new JCheckBox();
        chkLogujVerbose.setText("Verbose Logging");
        panel3.add(chkLogujVerbose, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        lbBuildInfo = new JLabel();
        lbBuildInfo.setText("Label");
        panel3.add(lbBuildInfo, new GridConstraints(3, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Log", panel4);
        btnClearLog = new JButton();
        btnClearLog.setText("Clear Log");
        panel4.add(btnClearLog, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer5 = new Spacer();
        panel4.add(spacer5, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JScrollPane scrollPane3 = new JScrollPane();
        panel4.add(scrollPane3, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        lbLog = new JList();
        Font lbLogFont = this.$$$getFont$$$("Courier New", -1, 12, lbLog.getFont());
        if (lbLogFont != null) lbLog.setFont(lbLogFont);
        scrollPane3.setViewportView(lbLog);
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        return new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
