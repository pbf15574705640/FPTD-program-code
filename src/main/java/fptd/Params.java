package fptd;

import java.math.BigInteger;

/**
 * 参数配置类，用于存储系统中使用的各种常量和配置参数
 */
public class Params {

    // 是否打印执行信息的开关
    public static final boolean IS_PRINT_EXE_INFO = true;



    // 服务器数量相关参数
    public static final int NUM_SERVER = 7;    // 服务器总数
    public static final int N = NUM_SERVER;     // 服务器总数的别名
    public static final int T = (N/2) + 1;     // 阈值，用于多数决定
    public static int ITER_TD = 3;             // TD算法的迭代次数

    // 大素数P，用于加密计算
    public static final BigInteger P = new BigInteger("3351951982485649274893506249551461531869841455148098344430890360930441007518386744200468574541725856922507964546621512713438470702986642486608412251521039");



    // 主节点的IP和端口配置
    public static final String IP_King = "127.0.0.1";    // 主节点IP地址
    public static final int Port_King = 8874;           // 主节点端口号



    // 精度相关常量
    public static final long PRECISE_ROUND = (long)Math.pow(10, 5);  // 精度舍入的基数

    // 离线数据存储目录
    public static final String FAKE_OFFLINE_DIR = "./offline_data/";



    // 对数计算相关常量
    private static final int exp = 13;  // 指数值
    public static final BigInteger CONSTANT_FOR_LOG = BigInteger.valueOf(10).pow(exp);  // 对数计算常量
    public static final BigInteger FIXED_DIVISOR_FOR_LOG = BigInteger.valueOf(10).pow(exp - 1);  // 对数计算固定除数

    // 注释的数据集配置（Duck Identification）
//    public final static String sensingDataFile = "datasets/d_Duck_Identification/answer.csv";
//    public final static String truthFile = "datasets/d_Duck_Identification/truth.csv";
//    public final static boolean isCategoricalData = true;

    // 注释的数据集配置（Dog Data）
//        public final static String sensingDataFile = "datasets/s4_Dog_data/answer.csv";
//        public final static String truthFile = "datasets/s4_Dog_data/truth.csv";
//        public final static boolean isCategoricalData = true;



    // 当前使用的数据集配置（Weather Data）
        public final static String sensingDataFile = "datasets/weather/answer.csv";    // 传感器数据文件路径
        public final static String truthFile = "datasets/weather/truth.csv";           // 真实数据文件路径
        public final static boolean isCategoricalData = false;                         // 是否为分类数据标志
}






