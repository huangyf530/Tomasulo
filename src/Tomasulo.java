import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Vector;

public class Tomasulo {
    // define the cycle of each instruction
    static int LDCC = 3;
    static int JUMPCC = 1;
    static int ADDCC = 3;
    static int MULCC = 12;
    static int DIVCC = 40;
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
    // construction function
    Tomasulo(){
        loadBuffers = new Vector<>();
        for(int i = 0; i < LOADBF; i++){
            loadBuffers.add(new ReservationStation(i, "LOAD"));
        }
        addStations = new Vector<>();
        for(int i = 0; i < ADDRS; i++){
            addStations.add(new ReservationStation(i, " ADD"));
        }
        mulStations = new Vector<>();
        for(int i = 0; i < MULTRS; i++){
            mulStations.add(new ReservationStation(i, "MULT"));
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
        ReservationStation current = loadBuffers.get(index);
        current.type = Tomasulo.LOADTYPE;
        current.result = num;
        current.write_to = write_to;
        registers.get(write_to).ok = false;
        registers.get(write_to).rs = current;
        current.just_do = 1;
        return true;
    }

    private boolean Handle(int qj, int qk, int write_to, Vector<ReservationStation> test, int type, Queue<ReservationStation> queue, Vector<Device> devices){
        int index = RSBusy(test);
        if(index < 0){
            return false;
        }
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
        current.write_to = write_to;
        registers.get(write_to).ok = false;     // wait new content
        registers.get(write_to).rs = current;
        current.just_do = 1;    // just issue
        int device = getEmptyDevice(devices);
        if(device < 0){
            queue.offer(current);
        }
        else{
            assert (devices.get(index).occupy(current));
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

    boolean updateStatus(String instruction){
        /*
         * return true if the instruction issue, else return false
         */
        System.out.println(instruction);
        // (1) write through
        for(ReservationStation rs : to_update){
            rs.wakeup();
        }

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
        String[] insts = instruction.split(",");
        int write_to = Integer.parseInt(insts[1].substring(1));
        int res1;
        int res2;
        switch (insts[0]){
            case "LD":
                int num = Integer.parseInt(insts[2].substring(2), 16);
                return HandleLD(write_to, num);
            case "SUB":
                res1 = Integer.parseInt(insts[2].substring(1));
                res2 = Integer.parseInt(insts[2].substring(1));
                return Handle(res1,res2, write_to, addStations, SUBTYPE, addqueue, addDevices);
            case "ADD":
                res1 = Integer.parseInt(insts[2].substring(1));
                res2 = Integer.parseInt(insts[2].substring(1));
                return Handle(res1,res2, write_to, addStations, ADDTYPE, addqueue, addDevices);
            case "MUL":
                res1 = Integer.parseInt(insts[2].substring(1));
                res2 = Integer.parseInt(insts[2].substring(1));
                return Handle(res1,res2, write_to, mulStations, MULTTYPE, mulqueue, multDevices);
            case "DIV":
                res1 = Integer.parseInt(insts[2].substring(1));
                res2 = Integer.parseInt(insts[2].substring(1));
                return Handle(res1,res2, write_to, mulStations, DIVTYPE, mulqueue, multDevices);
            case "JUMP":
                break;
            default:
                System.out.println(insts[0] + " is not a valid instruction");
                break;
        }
        printRegiters();
        printRS();
        return true;
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
        System.out.println("Time\tName\tBusy\tOp\t\tVj\t\tVk\t\tQj\t\tQk\n");
        for(ReservationStation rs : addStations){
            System.out.print("\t\t");
            System.out.print(rs.name + "\t");
            if(rs.isBusy()){
                System.out.print("Yes\t\t");
            }
            else{
                System.out.print("No\t\t");
            }
            System.out.print("\t\t");
            if(rs.sourcej == null){
                System.out.printf("% 8d", rs.vj);
            }
            else{
                System.out.print("\t\t");
            }
            if(rs.sourcek == null){
                System.out.printf("% 8d", rs.vk);
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
}

class Device{
    int type;
    private boolean busy;
    ReservationStation rs;
    Queue<ReservationStation> waitQueue;
    Device(Queue<ReservationStation> waitQueue){
        busy = false;
        this.waitQueue = waitQueue;
    }

    boolean occupy(ReservationStation rs){
        if(busy){
            return false;
        }
        else {
            busy = true;
        }
        return true;
    }

    boolean release(){
        if(busy){
            busy = false;
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
                    }
                    else{
                        rs = null;
                        busy = false;
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
                result = vj / vk;
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
             default:
                 break;
         }
     }

     void wakeup(){
        for(ReservationStation rs : waitlist){
            if(rs.sourcek == this){
                rs.vk = this.result;
                rs.sourcek = null;
            }
            if(rs.sourcej == this){
                rs.vj = this.result;
                rs.sourcej = null;
            }
        }
        waitlist.clear();
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