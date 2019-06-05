import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Vector;

public class Tomasulo {
    // define the cycle of each instruction
    static int LDCC = 3;
    static int JUMPCC = 1;
    static int ADDCC = 3;
    static int MULCC = 4;
    static int DIVCC = 4;
    static int DIVZEROCC = 1;
    // define the fu number
    private static int ADD = 3;
    private static int MULT = 2;
    private static int LOAD = 2;
    private static int REGISTER = 20;
    // define reservation stations number
    private static int ADDRS = 6;
    private static int MULTRS = 3;
    private static int LOADBF = 3;
    // define different type device
    final static int ADDTYPE = 1;
    final static int MULTTYPE = 2;
    final static int LOADTYPE = 3;
    final static int SUBTYPE = 4;
    final static int DIVTYPE = 5;
    final static int JUMPTYPE = 6;

    Vector<ReservationStation> loadBuffers;
    Vector<ReservationStation> addStations;
    Vector<ReservationStation> mulStations;
    Vector<Device> addDevices;
    Vector<Device> multDevices;
    Vector<Device> loadDevices;
    Vector<Register> registers;
    Vector<ReservationStation> to_update;
    private Queue<ReservationStation> addqueue;
    private Queue<ReservationStation> mulqueue;
    private Queue<ReservationStation> loadqueue;
    private int clock;
    int pc;
    boolean blockByJump;
    int execInstr;     // record the instructions number is being exec
    // construction function
    Tomasulo(){
        blockByJump = false;
        clock = 0;
        pc = 0;
        execInstr = 0;
        loadBuffers = new Vector<>();
        for(int i = 0; i < LOADBF; i++){
            loadBuffers.add(new ReservationStation(i + 1, "LOAD"));
        }
        addStations = new Vector<>();
        for(int i = 0; i < ADDRS; i++){
            addStations.add(new ReservationStation(i + 1, " ADD"));
        }
        mulStations = new Vector<>();
        for(int i = 0; i < MULTRS; i++){
            mulStations.add(new ReservationStation(i + 1, "MULT"));
        }
        addDevices = new Vector<>();
        addqueue = new LinkedList<>();
        for(int i = 0; i < ADD; i++){
            addDevices.add(new AddDevice(addqueue));
        }
        multDevices = new Vector<>();
        mulqueue = new LinkedList<>();
        for(int i = 0; i < MULT; i++){
            multDevices.add(new MultDevice(mulqueue));
        }
        loadDevices = new Vector<>();
        loadqueue = new LinkedList<>();
        for(int i = 0; i < LOAD; i++){
            loadDevices.add(new LoadDevice(loadqueue));
        }
        registers = new Vector<>();
        for(int i = 0; i < REGISTER; i++){
            registers.add(new Register());
        }
        to_update = new Vector<>();
    }

    private boolean HandleLD(int write_to, int num){
        int index = RSBusy(loadBuffers);
        if(index < 0){
            return false;
        }
        execInstr++;
        ReservationStation current = loadBuffers.get(index);
        current.type = Tomasulo.LOADTYPE;
        current.result = num;
        current.write_to = write_to;
        current.busy = true;
        registers.get(write_to).ok = false;
        registers.get(write_to).rs = current;
        current.just_do = 1;
        int device = getEmptyDevice(loadDevices);
        if(device < 0){
            loadqueue.offer(current);
        }
        else {
            System.out.println(device);
            loadDevices.get(device).occupy(current);
            loadDevices.get(device).update(this);
        }
        return true;
    }

    private boolean Handle(int qj, int qk, int write_to, Vector<ReservationStation> test, int type, Queue<ReservationStation> queue, Vector<Device> devices){
        int index = RSBusy(test);
        if(index < 0){
            return false;
        }
        execInstr++;
        ReservationStation current = test.get(index);
        current.type = type;
        if(registers.get(qj).ok){
            current.vj = registers.get(qj).content;
            current.sourcej = null;
        }
        else{
            current.sourcej = registers.get(qj).rs;
            registers.get(qj).rs.waitlist.add(current);
        }
        if(registers.get(qk).ok){
            current.vk = registers.get(qk).content;
            current.sourcek = null;
        }
        else{
            current.sourcek = registers.get(qk).rs;
            registers.get(qk).rs.waitlist.add(current);
        }
        current.busy = true;
        current.write_to = write_to;
        registers.get(write_to).ok = false;     // wait new content
        registers.get(write_to).rs = current;
        current.just_do = 1;    // just issue
        int device = getEmptyDevice(devices);
        if(device < 0){
            queue.offer(current);
        }
        else{
            // System.out.println(device);
            devices.get(device).occupy(current);
            devices.get(device).update(this);
        }
        return true;
    }

    private boolean HandleJump(int vj, int qk, int offset){
        int index = RSBusy(addStations);
        if(index < 0){
            return false;
        }
        execInstr++;
        ReservationStation current = addStations.get(index);
        current.type = Tomasulo.JUMPTYPE;
        current.vj = vj;
        if(registers.get(qk).ok){
            current.vk = registers.get(qk).content;
            current.sourcek = null;
        }
        else{
            current.sourcek = registers.get(qk).rs;
            registers.get(qk).rs.waitlist.add(current);
        }
        current.just_do = 1;
        current.busy = true;
        current.write_to = offset;
        int device = getEmptyDevice(addDevices);
        if(device < 0){
            addqueue.offer(current);
        }
        else{
            addDevices.get(device).occupy(current);
            addDevices.get(device).update(this);
        }
        return true;
    }

    private int RSBusy(Vector<ReservationStation> rs){
        int k = -1;
        for(int i = 0; i < rs.size(); i++){
            if(!rs.get(i).isBusy()){
                k = i;
                return k;
            }
        }
        return k;
    }

    private int getEmptyDevice(Vector<Device> devices){
        int k = -1;
        for(int i = 0; i < devices.size(); i++){
            if(!devices.get(i).isBusy()){
                return i;
            }
        }
        return k;
    }

    boolean updateStatus(Vector<String> instructions){
        /*
         * return true if the instruction issue, else return false
         */
        clock++;
        System.out.printf("Clock : %d\n", clock);
        // (1) write through
        for(ReservationStation rs : to_update){
            System.out.print(rs.name + " ");
            rs.wakeup(this);
        }
        System.out.println();
        to_update.clear();

        // (2) update device status
        for(int i = 0; i < LOAD; i++){
            loadDevices.get(i).update(this);
        }
        for(int i = 0; i < ADD; i++){
            addDevices.get(i).update(this);
        }
        for(int i = 0; i < MULT; i++){
            multDevices.get(i).update(this);
        }

        // handle new instruction
        if(blockByJump || pc >= instructions.size()){
            printRegiters();
            printRS();
            printLoadBuffers();
            return false;
        }
        String instruction = instructions.get(pc);
        System.out.println("Issue instruction: " + instruction);
        System.out.println(instruction);
        String[] insts = instruction.split(",");
        int write_to;
        int res1;
        int res2;
        if(!insts[0].equals("JUMP")){
            write_to = Integer.parseInt(insts[1].substring(1));
        }
        else{
            blockByJump = true;
            write_to = 0;
        }
        boolean succeed = false;
        switch (insts[0]){
            case "LD":
                int num = (int)Long.parseLong(insts[2].substring(2), 16);
                succeed  = HandleLD(write_to, num);
                break;
            case "SUB":
                res1 = Integer.parseInt(insts[2].substring(1));
                res2 = Integer.parseInt(insts[3].substring(1));
                succeed  = Handle(res1,res2, write_to, addStations, SUBTYPE, addqueue, addDevices);
                break;
            case "ADD":
                res1 = Integer.parseInt(insts[2].substring(1));
                res2 = Integer.parseInt(insts[3].substring(1));
                succeed = Handle(res1,res2, write_to, addStations, ADDTYPE, addqueue, addDevices);
                break;
            case "MUL":
                res1 = Integer.parseInt(insts[2].substring(1));
                res2 = Integer.parseInt(insts[3].substring(1));
                succeed = Handle(res1,res2, write_to, mulStations, MULTTYPE, mulqueue, multDevices);
                break;
            case "DIV":
                res1 = Integer.parseInt(insts[2].substring(1));
                res2 = Integer.parseInt(insts[3].substring(1));
                succeed = Handle(res1,res2, write_to, mulStations, DIVTYPE, mulqueue, multDevices);
                break;
            case "JUMP":
                write_to = (int)Long.parseLong(insts[3].substring(2), 16);
                res1 = (int)Long.parseLong(insts[1].substring(2),16);
                res2 = Integer.parseInt(insts[2].substring(1));
                succeed = HandleJump(res1, res2, write_to);
                if(!succeed){
                    System.out.println("not succeed!");
                    blockByJump = false;
                }
                break;
            default:
                System.out.println(insts[0] + " is not a valid instruction");
                break;
        }
        printRegiters();
        printRS();
        printLoadBuffers();
        if(succeed && !blockByJump){
            pc++;
        }
        return succeed;
    }

    private void printLoadBuffers(){
        System.out.println("Time\tName\tBusy\tContent");
        for(ReservationStation rs : loadBuffers){
            if(rs.just_do == 2 && rs.count_down != 0){
                System.out.print(rs.count_down);
            }
            System.out.print("\t\t");
            System.out.print(rs.name + "\t");
            if(rs.isBusy()){
                System.out.print("Yes\t\t" + rs.result);
            }
            else{
                System.out.print("No\t\t");
            }
            System.out.print("\n");
        }
    }

    private void printRegiters(){
        for(int i = 0; i < registers.size(); i++){
            System.out.print("F" + i + "\t\t");
        }
        System.out.print("\n");
        for(int i = 0; i < registers.size(); i++){
            if(registers.get(i).ok){
                System.out.print(registers.get(i).content + "\t\t");
            }
            else{
                System.out.print(registers.get(i).rs.name + "\t");
            }
        }
        System.out.print("\n");
    }

    private void printRS(){
        System.out.println("Time\tName\tBusy\tOp\t\tVj\t\tVk\t\tQj\t\tQk");
        printTheRS(addStations);
        printTheRS(mulStations);
    }

    private void printTheRS(Vector<ReservationStation> current){
        for(ReservationStation rs : current){
            if(rs.just_do == 2){
                System.out.print(rs.count_down);
            }
            System.out.print("\t\t");
            System.out.print(rs.name + "\t");
            if(rs.isBusy()){
                System.out.print("Yes\t\t");
            }
            else{
                System.out.println("No\t\t");
                continue;
            }
            System.out.print(rs.Op() + "\t");
            if(rs.sourcej == null){
                System.out.printf("% 6d  ", rs.vj);
            }
            else{
                System.out.print("\t\t");
            }
            if(rs.sourcek == null){
                System.out.printf("% 6d  ", rs.vk);
            }
            else{
                System.out.print("\t\t");
            }
            if(rs.sourcej != null){
                System.out.print(rs.sourcej.name+"\t");
            }
            else {
                System.out.print("\t\t");
            }
            if(rs.sourcek != null){
                System.out.print(rs.sourcek.name+"\t");
            }
            else {
                System.out.print("\t\t");
            }
            System.out.print("\n");
        }
    }

    public boolean isEnd(){
        return execInstr == 0;
    }
}

class Device{
    int type;
    private boolean busy;
    ReservationStation rs;
    Queue<ReservationStation> waitQueue;
    Device(Queue<ReservationStation> waitQueue){
        busy = false;
        this.waitQueue = waitQueue;
        rs = null;
    }

    boolean occupy(ReservationStation rs){
        if(busy){
            return false;
        }
        else {
            busy = true;
            this.rs = rs;
        }
        return true;
    }

    boolean release(){
        if(busy){
            busy = false;
            rs = null;
            return true;
        }
        else {
            return false;
        }
    }

    boolean isBusy(){
        return busy;
    }

    boolean update(Tomasulo tomasulo){
        if(!busy){
            return false;
        }
        if(rs.just_do == 1){ // just issue one instruction
            if(rs.getReady()){
                rs.initCountDown();
                rs.just_do = 2;
                return true;
            }
            else{
                return false;
            }
        }
        if(rs.just_do == 2){
            if(rs.count_down != 0){
                rs.count_down--;
                if(rs.count_down == 0){  // exec over
                    tomasulo.to_update.add(rs);
                    rs.result();
                    if(waitQueue.size() > 0){
                        rs = waitQueue.poll();  // schedule to next instruction
                        update(tomasulo);
                    }
                    else{
                        release();
                    }
                }
            }
            else{
                rs.just_do = 3;
            }
            return true;
        }
        return false;
    }
}

class AddDevice extends Device{
    AddDevice(Queue<ReservationStation> queue){
        super(queue);
        super.type = Tomasulo.ADDTYPE;
    }
}


class MultDevice extends Device{
    MultDevice(Queue<ReservationStation> queue){
        super(queue);
        super.type = Tomasulo.MULTTYPE;
    }
}

class LoadDevice extends Device{
    LoadDevice(Queue<ReservationStation> queue){
        super(queue);
        super.type = Tomasulo.LOADTYPE;
    }
}

class LoadBuffer{
    private boolean busy;
    int address;
    LoadBuffer(){
        busy = false;
        address = 0;
    }
    public void setAddress(int add){
        busy = true;
        address = add;
    }

    boolean isBusy(){
        return busy;
    }
}

class ReservationStation{
    String name;
    int type;
    int num;
    int count_down;    // timer
    boolean busy;
    int address;
    int vj;
    int vk;
    int write_to;
    int result;
    ReservationStation sourcej;
    ReservationStation sourcek;
    ArrayList<ReservationStation> waitlist;
    int just_do;   // 1 issue  2 exec   3 write through
    ReservationStation(int num, String name){
        this.num = num;
        this.name = name + num;
        busy = false;
        address = 0;
        just_do = 0;
        sourcej = null;
        sourcek = null;
        waitlist = new ArrayList<>();
    }

    String Op(){
        switch (type){
            case Tomasulo.ADDTYPE:
                return " ADD";
            case Tomasulo.SUBTYPE:
                return " SUB";
            case Tomasulo.LOADTYPE:
                return "LOAD";
            case Tomasulo.MULTTYPE:
                return "MULT";
            case Tomasulo.DIVTYPE:
                return " DIV";
            case Tomasulo.JUMPTYPE:
                return "JUMP";
            default:
                return null;
        }
    }

    private void release(){
        busy = false;
        address = 0;
        just_do = 0;
        sourcek = null;
        sourcej = null;
        vj = vk = 0;
        write_to = 0;
        result = 0;
    }

     boolean isBusy(){
        return busy;
     }

     boolean getReady(){
        return (sourcej == null && sourcek == null) || (type == Tomasulo.LOADTYPE);
     }

     int result(){
        switch (type){
            case Tomasulo.ADDTYPE:
                result = vj + vk;
                break;
            case Tomasulo.MULTTYPE:
                result = vj * vk;
                break;
            case Tomasulo.LOADTYPE:
                break;
            case Tomasulo.SUBTYPE:
                result = vj - vk;
                break;
            case Tomasulo.DIVTYPE:
                result = (vk == 0) ? vj : vj / vk;
                break;
            case Tomasulo.JUMPTYPE:
                result = vj - vk;
                break;
            default:
                break;
        }
        return result;
     }

     void initCountDown(){
         switch (type){
             case Tomasulo.ADDTYPE:
                 count_down = Tomasulo.ADDCC;
                 break;
             case Tomasulo.MULTTYPE:
                 count_down = Tomasulo.MULCC;
                 break;
             case Tomasulo.LOADTYPE:
                 count_down = Tomasulo.LDCC;
                 break;
             case Tomasulo.DIVTYPE:
                 count_down = (vk == 0)? Tomasulo.DIVZEROCC : Tomasulo.DIVCC;
                 break;
             case Tomasulo.SUBTYPE:
                 count_down = Tomasulo.ADDCC;
                 break;
             case Tomasulo.JUMPTYPE:
                 count_down = Tomasulo.JUMPCC;
                 break;
             default:
                 break;
         }
     }

     void wakeup(Tomasulo tomasulo){
//        if(waitlist.size() > 0){
//            System.out.print(name + " : ");
//        }
         Vector<Register> registers = tomasulo.registers;
        for(ReservationStation rs : waitlist){
        //            System.out.print(rs.name + " ");
            if(rs.sourcek == this){
                rs.vk = this.result;
                rs.sourcek = null;
            }
            if(rs.sourcej == this){
                rs.vj = this.result;
                rs.sourcej = null;
            }
        }
        if(this.type == Tomasulo.JUMPTYPE){
            tomasulo.blockByJump = false;
            if(result == 0){
                tomasulo.pc += write_to;
            }
            else{
                tomasulo.pc++;
            }
        }
        else if(registers.get(write_to).rs == this){
            registers.get(write_to).content = result;
            registers.get(write_to).ok = true;
            registers.get(write_to).rs = null;
        }
//         if(waitlist.size() > 0){
//             System.out.print("\n");
//         }
        release();
        waitlist.clear();
        tomasulo.execInstr--;
     }
}

class InstructionStatus{
    String instruction;
    int issue;
    int execcomp;
    int write;
    InstructionStatus(String inst){
        instruction = inst;
        issue = 0;
        execcomp = 0;
        write = 0;
    }

    int Issue(int t){
        issue = t;
        return issue;
    }

    int Exec(int t){
        execcomp = t;
        return execcomp;
    }

    int Write(int t){
        write = t;
        return write;
    }

    void printStatus(){
        System.out.println(instruction + "\t" + issue + "\t" + execcomp + "\t" + write);
    }
}

class Register{
    ReservationStation rs;   // wait the result of rs
    int content;
    boolean ok;
    Register(){
        content = 0;
        rs = null;
        ok = true;
    }
}