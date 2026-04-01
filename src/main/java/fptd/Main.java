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
    public static void main(String[] args) throws InterruptedException {
        // 如果开启执行信息打印，则输出启动信息
        if(Params.IS_PRINT_EXE_INFO) {
            System.out.println("Starting optimal TD...");
        }

        
        // 从Params类获取配置参数
        final String sensingDataFile = Params.sensingDataFile;  // 感知数据文件路径
        final String truthFile = Params.truthFile;              // 真实数据文件路径
        boolean isCategoricalData = Params.isCategoricalData;  // 是否为分类数据标志

        // 初始化工作线程数为-1（表示自动确定）
        int requiredWorkerNum = -1;
        // 创建数据管理器，用于读取和管理数据
        DataManager dataManager = new DataManager(sensingDataFile, truthFile, isCategoricalData, requiredWorkerNum);
        // 获取工作线程数量和考试数量
        final int workerNum = dataManager.sensingDataMatrix.size();
        final int examNum = dataManager.sensingDataMatrix.get(0).size();

        // 打离线阶段开始信息
        if(Params.IS_PRINT_EXE_INFO){
            System.out.println("Finish to read sensing data from hard disk");
            System.out.println("Start to the offline phase");
        }

        // 定义作业名称
        final String jobName = "TD_optimal";

        // 执行离线阶段
        runTDOffline(workerNum, examNum, jobName);

        // 打印离线阶段完成信息
        if(Params.IS_PRINT_EXE_INFO){
            System.out.println("Finish to offline phase");
        }

        // 执行在线阶段
        runTDOnline(workerNum, examNum, jobName, dataManager);

        // 打印在线阶段完成信息
        if(Params.IS_PRINT_EXE_INFO){
            System.out.println("Finish to online phase");
        }
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