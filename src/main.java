import java.io.*;
import java.util.Vector;


public class main {
    private static String path = "./nel/test0.nel";
    public static void main(String[] args)
    {
        Vector<String> instruction = readInstruction(path);
        Tomasulo tomasulo = new Tomasulo();
        tomasulo.updateStatus(instruction.get(0));
    }

    private static Vector<String> readInstruction(String path){
        File f = new File(path);
        BufferedReader reader = null;
        Vector<String> instruction = new Vector<>();
        try{
            reader = new BufferedReader(new FileReader(f));
            String tempString = null;
            int line = 1;
            // 一次读入一行，直到读入null为文件结束
            while ((tempString = reader.readLine()) != null) {
                // 显示行号
                instruction.add(tempString);
                line++;
            }
            reader.close();
        }
        catch (IOException e){
            e.printStackTrace();
            System.out.println(path + " not find!");
        }
        return instruction;
    }
}
