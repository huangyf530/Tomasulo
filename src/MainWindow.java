import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;


public class MainWindow extends JFrame {
    private Tomasulo tomasulo;
    private JButton start;
    private JButton next;
    private JButton initial;
    private Vector<String> instructions;

    private Vector<String> inscnames;
    private Vector<Vector<String>> insdata;
    private JTable insTable;

    private Vector<String> rscnames;
    private Vector<Vector<String>> rsdata;
    private JTable rs;

    private Vector<String> lbcnames;
    private Vector<Vector<String>> lbdata;
    private JTable lb;

    private Vector<String> regnames;
    private Vector<Vector<String>> regdata;
    private JTable register;
    MainWindow(String windowName, Tomasulo tom, Vector<String> ins){
        super(windowName);
        this.tomasulo = tom;
        instructions = ins;
        JPanel contentPanel = (JPanel)this.getContentPane();
        JPanel buttonPanel = initButtonPanel();
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        JPanel statusPanel = initStatusPanel();
        contentPanel.add(statusPanel, BorderLayout.CENTER);
        for(String instruction : instructions){
            Vector<String> temp = new Vector<>();
            temp.add(instruction);
            for(int i = 1; i < inscnames.size(); i++){
                temp.add("");
            }
            insdata.add(temp);
        }

        contentPanel.setOpaque(true);

        addListener();

        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(1000, 800);
        setVisible(true);
    }

    private JPanel initButtonPanel(){
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        start = new JButton("START");
        buttonPanel.add(start);
        next = new JButton("NEXT");
        buttonPanel.add(next);
        initial = new JButton("INIT");
        buttonPanel.add(initial);
        return buttonPanel;
    }

    private JPanel initStatusPanel(){
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BorderLayout());

        // init instruction table
        insdata = new Vector<>();
        inscnames = new Vector<>();
        inscnames.add("instruction");
        inscnames.add("issue");
        inscnames.add("Exec Comp");
        inscnames.add("Write Through");
        insTable = new JTable(insdata, inscnames);
        JScrollPane scrollPane = new JScrollPane(insTable);
        insTable.setBackground(Color.YELLOW);
        scrollPane.setPreferredSize(new Dimension(1000, 300));
        statusPanel.add(scrollPane, BorderLayout.NORTH);

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
        regdata = new Vector<>();
        regnames = new Vector<>();
        regdata.add(new Vector<>());
        for(int i = 0; i < tomasulo.REGISTER; i++){
            regnames.add("F" + i);
            regdata.get(0).add("");
        }
        register = new JTable(regdata, regnames);
        JScrollPane REGscrollPane = new JScrollPane(register);
        REGscrollPane.setPreferredSize(new Dimension(1000, 60));
        statusPanel.add(REGscrollPane, BorderLayout.SOUTH);
        return statusPanel;
    }

    void addListener(){
        next.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(e.getSource() == next){
                    tomasulo.updateStatus(instructions,insdata, regdata, rsdata, lbdata);
                    insTable.updateUI();
                    rs.updateUI();
                    lb.updateUI();
                    register.updateUI();
                }
            }
        });

        start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(e.getSource() == start){
                    startTomasulo(0);
                    insTable.updateUI();
                    rs.updateUI();
                    lb.updateUI();
                    register.updateUI();
                }
            }
        });

        initial.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(e.getSource() == initial){
                    initial();
                    insTable.updateUI();
                    rs.updateUI();
                    lb.updateUI();
                    register.updateUI();
                }
            }
        });
    }

    private void startTomasulo(int endClock){
        int clock = 1;
        while(!tomasulo.isEnd() || clock == 1){
            tomasulo.updateStatus(instructions, insdata, regdata, rsdata, lbdata);
            clock++;
            if(clock == endClock){
                break;
            }
        }
    }

    private void initial(){
        tomasulo.initClock();
        for(Vector<String> temp : insdata){
            for(int i = 1; i < inscnames.size(); i++){
                temp.setElementAt("", i);
            }
        }
        for(Vector<String> temp : regdata){
            for(int i = 0; i < regnames.size(); i++){
                temp.setElementAt("0", i);
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
}
