package fptd;

import fptd.protocols.Circuit;
import fptd.truthDiscovery.optimized.TDOfflineOptimal;
import fptd.truthDiscovery.optimized.TDOnlineOptimal;
import fptd.utils.DataManager;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * 主类，包含程序入口点和核心执行流程
 */
public class Main {

    /**
     * 程序入口点
     * @param args 命令行参数
     * @throws InterruptedException 线程中断异常
     */
    // 重复实验次数（论文要求10次取平均）
    private static final int REPEAT_TIMES = 10;

    public static void main(String[] args) throws InterruptedException {
        final String sensingDataFile = Params.sensingDataFile;
        final String truthFile = Params.truthFile;
        boolean isCategoricalData = Params.isCategoricalData;

        int requiredWorkerNum = -1;
        DataManager dataManager = new DataManager(sensingDataFile, truthFile, isCategoricalData, requiredWorkerNum);
        final int workerNum = dataManager.sensingDataMatrix.size();
        final int examNum = dataManager.sensingDataMatrix.get(0).size();

        final String jobName = "TD_optimal";

        System.out.println("=== 实验配置 ===");
        System.out.println("数据集: " + sensingDataFile);
        System.out.println("服务器数 N: " + Params.NUM_SERVER);
        System.out.println("迭代次数: " + Params.ITER_TD);
        System.out.println("Worker数: " + workerNum + ", Exam数: " + examNum);
        System.out.println("重复次数: " + REPEAT_TIMES);
        System.out.println("================");

        // 离线阶段只需执行一次
        runTDOffline(workerNum, examNum, jobName);

        // 在线阶段重复执行REPEAT_TIMES次，取平均时间
        long totalOnlineTime = 0;
        for (int i = 0; i < REPEAT_TIMES; i++) {
            long onlineStart = System.currentTimeMillis();
            runTDOnline(workerNum, examNum, jobName, dataManager);
            long onlineTime = System.currentTimeMillis() - onlineStart;
            totalOnlineTime += onlineTime;
            System.out.println("第" + (i + 1) + "次在线阶段耗时: " + onlineTime + " ms");
        }

        double avgOnlineTime = (double) totalOnlineTime / REPEAT_TIMES;
        System.out.println("================");
        System.out.println("在线阶段平均耗时: " + String.format("%.2f", avgOnlineTime) + " ms");
        System.out.println("在线阶段平均耗时: " + String.format("%.4f", avgOnlineTime / 1000.0) + " s");
    }

    private static void runTDOffline(int workerNum, int examNum, String jobName) {
        new TDOfflineOptimal(workerNum, examNum, jobName).runTDOffline();
    }

    private static void runTDOnline(int workerNum,
                                    int examNum,
                                    String jobName,
                                    DataManager dataManager)
            throws InterruptedException {

        List<List<BigInteger>> worker2Labels = dataManager.sensingDataMatrix;
        if(worker2Labels.size() != workerNum) {
            throw new RuntimeException("Wrong number of workers");
        }
        if(worker2Labels.getFirst().size() != examNum) {
            throw new RuntimeException("Wrong number of exams");
        }

        if(Params.IS_PRINT_EXE_INFO){
            System.out.println("Start to build TD circuits");
        }

        TDOnlineOptimal tdOnline = new TDOnlineOptimal(workerNum, examNum);
        List<Circuit> circuits = tdOnline.buildTDCircuit(worker2Labels, jobName);

        if(Params.IS_PRINT_EXE_INFO){
            System.out.println("Finish to build TD circuits");
        }

        List<Thread> threads = new ArrayList<>();
        for(int owner_idx = 0; owner_idx < Params.NUM_SERVER; owner_idx++){
            Circuit circuit = circuits.get(owner_idx);
            Thread thread = new Thread(new ServerThread(circuit, owner_idx, owner_idx == 0, dataManager));
            thread.start();
            threads.add(thread);
            if (0 == owner_idx){//if it is the king
                Thread.sleep(500);
            }
        }
        for(Thread thread : threads){
            thread.join();
        }
    }
}