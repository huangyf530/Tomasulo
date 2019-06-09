import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;


public class MainWindow extends JFrame {
    final static int ONELINEREG = 16;
    private Tomasulo tomasulo;
    private JButton start;
    private JButton next;
    private JButton initial;
    private JLabel clockLabel;
    private JTextField clockText;
    private JButton loadInst;
    private Vector<String> instructions;
    private JFileChooser fileChooser;
    private JPanel contentPanel;

    private Vector<String> inscnames;
    private Vector<Vector<String>> insdata;
    private JTable insTable;

    private Vector<String> rscnames;
    private Vector<Vector<String>> rsdata;
    private JTable rs;

    private Vector<String> lbcnames;
    private Vector<Vector<String>> lbdata;
    private JTable lb;

    private Vector<Vector<String>> regnames;
    private Vector<Vector<Vector<String>>> regdata;
    private Vector<JTable> register;

    private Status status;
    private JTextField issueText;
    private JTextArea execField;
    private JTextArea writeField;
    MainWindow(String windowName, Tomasulo tom){
        super(windowName);
        this.tomasulo = tom;
        instructions = new Vector<>();
        status = new Status();
        contentPanel = (JPanel)this.getContentPane();
        JPanel buttonPanel = initButtonPanel();
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        JPanel statusPanel = initStatusPanel();
        contentPanel.add(statusPanel, BorderLayout.CENTER);

        contentPanel.setOpaque(true);

        fileChooser = new JFileChooser();
        File current = new File("./");
        fileChooser.setCurrentDirectory(current);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        addListener();

        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(900, 700);
        setVisible(true);
    }

    private JPanel initButtonPanel(){
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        JLabel endClock = new JLabel("End Clock:");
        buttonPanel.add(endClock);
        clockText = new JTextField("0");
        clockText.setPreferredSize(new Dimension(150, 20));
        clockText.setMaximumSize(new Dimension(150, 20));
        clockText.setMinimumSize(new Dimension(100, 15));
        buttonPanel.add(clockText);
        start = new JButton("START");
        buttonPanel.add(start);
        next = new JButton("NEXT");
        buttonPanel.add(next);
        initial = new JButton("INIT");
        buttonPanel.add(initial);
        loadInst = new JButton("LOAD");
        buttonPanel.add(loadInst);
        return buttonPanel;
    }

    private JPanel initStatusPanel(){
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BorderLayout());
        // init clock panel
        JPanel clockPanel = initClockPanel();

        // init instruction table
        insdata = new Vector<>();
        inscnames = new Vector<>();
        inscnames.add("instruction");
        inscnames.add("issue");
        inscnames.add("Exec Comp");
        inscnames.add("Write Through");
        insTable = new JTable(insdata, inscnames);
        JScrollPane scrollPane = new JScrollPane(insTable);
        scrollPane.setPreferredSize(new Dimension(700, 300));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.LINE_AXIS));
        topPanel.add(clockPanel);
        topPanel.add(scrollPane);
        statusPanel.add(topPanel, BorderLayout.NORTH);

        // init reservation stations table
        JPanel rsPanel = new JPanel();
        rsPanel.setLayout(new BoxLayout(rsPanel, BoxLayout.LINE_AXIS));

        rsdata = new Vector<>();
        rscnames = new Vector<>();
        rscnames.add("Time");
        rscnames.add("Name");
        rscnames.add("Busy");
        rscnames.add("Op");
        rscnames.add("Vj");
        rscnames.add("Vk");
        rscnames.add("Qj");
        rscnames.add("Qk");
        for(int i = 0; i < Tomasulo.ADDRS + Tomasulo.MULTRS; i++){
            rsdata.add(new Vector<>());
            for(int j = 0; j < rscnames.size(); j++){
                rsdata.get(i).add("");
            }
        }
        rs = new JTable(rsdata, rscnames);
        JScrollPane RSscrollPane = new JScrollPane(rs);
        rsPanel.add(RSscrollPane);

        // init load buffer stations table
        lbdata = new Vector<>();
        lbcnames = new Vector<>();
        lbcnames.add("Time");
        lbcnames.add("Name");
        lbcnames.add("Busy");
        lbcnames.add("Immediate");
        for(int i = 0; i < Tomasulo.LOADBF; i++){
            lbdata.add(new Vector<>());
            for(int j = 0; j < lbcnames.size(); j++){
                lbdata.get(i).add("");
            }
        }
        lb = new JTable(lbdata, lbcnames);
        JScrollPane LBscrollPane = new JScrollPane(lb);
        rsPanel.add(LBscrollPane);
        rsPanel.setPreferredSize(new Dimension(1000, 300));

        statusPanel.add(rsPanel, BorderLayout.CENTER);

        // init register panel
        JPanel regiterPane = new JPanel();
        regiterPane.setLayout(new BoxLayout(regiterPane, BoxLayout.PAGE_AXIS));
        regdata = new Vector<>();
        regnames = new Vector<>();
        register = new Vector<>();
        int regtablenum = (int)Math.ceil((double)(tomasulo.REGISTER) / ONELINEREG);
        for(int i = 0; i < regtablenum; i++){
            Vector<String> oneline = new Vector<>();
            regdata.add(new Vector<>());
            Vector<Vector<String>> thisline = regdata.get(i);
            thisline.add(new Vector<>());
            for(int j = 0; j < ONELINEREG; j++) {
                if((i * ONELINEREG + j) >= tomasulo.REGISTER){
                    break;
                }
                oneline.add("F" + (i * ONELINEREG + j));
                thisline.get(0).add("0");
            }
            regnames.add(oneline);
            register.add(new JTable(regdata.get(i), regnames.get(i)));
            JScrollPane REGscrollPane = new JScrollPane(register.get(i));
            REGscrollPane.setPreferredSize(new Dimension(700, 40));
            regiterPane.add(REGscrollPane);
        }
        statusPanel.add(regiterPane, BorderLayout.SOUTH);
        return statusPanel;
    }

    private JPanel initClockPanel(){
        JPanel clockPanel = new JPanel();
        clockLabel = new JLabel("Clock: 0");
        clockPanel.setLayout(new BoxLayout(clockPanel, BoxLayout.PAGE_AXIS));
        clockPanel.add(clockLabel);
        JLabel issue = new JLabel("Issue instruction: ");
        issueText = new JTextField();
        issueText.setPreferredSize(new Dimension(300, 30));
        issueText.setMaximumSize(new Dimension(600, 30));
        issueText.setEnabled(false);
        JLabel exec = new JLabel("Exec comp: ");
        execField = new JTextArea();
        execField.setEnabled(false);
        JLabel write = new JLabel("Write Back: ");
        writeField = new JTextArea();
        writeField.setEnabled(false);
        clockPanel.add(issue);
        clockPanel.add(issueText);
        clockPanel.add(exec);
        clockPanel.add(execField);
        clockPanel.add(write);
        clockPanel.add(writeField);
        clockPanel.setPreferredSize(new Dimension(300, 300));
        return clockPanel;
    }

    private void addListener(){
        next.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(e.getSource() == next){
                    tomasulo.updateStatus(instructions,insdata, regdata, rsdata, lbdata, status);
                    updateTable();
                }
            }
        });

        start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(e.getSource() == start){
                    int endClock = Integer.parseInt(clockText.getText());
                    long startTime = System.currentTimeMillis();
                    startTomasulo(endClock);
                    long endTime   = System.currentTimeMillis();
                    System.out.println("Total time = " + (endTime - startTime));
                    updateTable();
                }
            }
        });

        initial.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(e.getSource() == initial){
                    initial();
                    updateTable();
                }
            }
        });

        loadInst.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(e.getSource() == loadInst){
                    int result = fileChooser.showOpenDialog(contentPanel);
                    if(result == JFileChooser.APPROVE_OPTION){
                        File file = fileChooser.getSelectedFile();
                        readInstruction(file);
                        setInstructions();
                        insTable.updateUI();
                    }
                }
            }
        });
    }

    private void startTomasulo(int endClock){
        int clock = 1;
        while(!tomasulo.isEnd() || clock == 1){
            tomasulo.updateStatus(instructions, insdata, regdata, rsdata, lbdata, status);
            if(clock == endClock){
                break;
            }
            clock++;
        }
    }

    private void initial(){
        tomasulo.initClock();
        issueText.setText("");
        execField.setText("");
        writeField.setText("");
        status.init();
        for(Vector<String> temp : insdata){
            for(int i = 1; i < inscnames.size(); i++){
                temp.setElementAt("", i);
            }
        }
        for(int j = 0; j < regnames.size(); j++){
            for(int i = 0; i < regnames.get(j).size(); i++){
                regdata.get(j).get(0).setElementAt("0", i);
            }
        }
        for(Vector<String> temp : rsdata){
            for(int i = 0; i < rscnames.size(); i++){
                temp.setElementAt("", i);
            }
        }
        for(Vector<String> temp : lbdata){
            for(int i = 0; i < lbcnames.size(); i++){
                temp.setElementAt("", i);
            }
        }
    }

    private void readInstruction(File f){
        BufferedReader reader = null;
        instructions.clear();
        try{
            reader = new BufferedReader(new FileReader(f));
            String tempString = null;
            int line = 1;
            // 一次读入一行，直到读入null为文件结束
            while ((tempString = reader.readLine()) != null) {
                // 显示行号
                if(tempString.equals("")){
                    continue;
                }
                instructions.add(tempString);
                line++;
            }
            reader.close();
        }
        catch (IOException e){
            e.printStackTrace();
            System.out.println(f.getAbsolutePath() + " not find!");
        }
    }

    private void setInstructions(){
        insdata.clear();
        for(String instruction: instructions){
            Vector<String> temp = new Vector<>();
            temp.add(instruction);
            for(int j = 1; j < inscnames.size(); j++){
                temp.add("");
            }
            insdata.add(temp);
        }
    }

    private void updateTable(){
        clockLabel.setText("Clock: " + tomasulo.getClock());
        issueText.setText(status.currentIssue);
        StringBuilder builder = new StringBuilder();
        for(String temp : status.currentExec){
            builder.append(temp + "\n");
        }
        execField.setText(builder.toString());
        builder.delete(0, builder.length());
        for(String temp : status.currentWrite){
            builder.append(temp + "\n");
        }
        writeField.setText(builder.toString());
        insTable.updateUI();
        rs.updateUI();
        lb.updateUI();
        for(JTable temp : register){
            temp.updateUI();
        }
    }
}

class Status{
    String currentIssue;
    Vector<String> currentExec;
    Vector<String> currentWrite;
    Status(){
        currentIssue = null;
        currentExec = new Vector<>();
        currentWrite = new Vector<>();
    }

    void init(){
        currentWrite.clear();
        currentExec.clear();
        currentIssue = null;
    }
}