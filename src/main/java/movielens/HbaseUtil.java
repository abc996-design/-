package movielens;

import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.RegexStringComparator;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.spark.SparkConf;
import org.apache.spark.serializer.KryoSerializer;
import org.apache.hadoop.conf.Configuration;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public class HbaseUtil extends KryoSerializer {

    private static Connection connection = null;
    private static Admin admin = null;

    static {
        try {
            //获取配置信息
            Configuration configuration = HBaseConfiguration.create();
            configuration.set("hbase.zookeeper.quorum", "192.168.150.137:2181");
            //创建链接对象
            connection = ConnectionFactory.createConnection(configuration);
            //创建Admin的对象
            admin = connection.getAdmin();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public HbaseUtil(SparkConf conf) {
        super(conf);
    }

    //判读表是否存在

    public static boolean isTableExist(String isTable) throws IOException {

        //连接配置
//        HBaseConfiguration configuration = new HBaseConfiguration(); //过时的配置
//        Configuration configuration = HBaseConfiguration.create();
//        configuration.set("hbase.zookeeper.quorum", "hadoop102,hadoop103,hadoop104");

        /**
         * 获取管理员的对象
         * Admin 操作DDL
         * api返回Table的 是操作DML的对象
         */
//        HBaseAdmin baseAdmin = new HBaseAdmin(configuration);//过时的配置
//        Connection connection = ConnectionFactory.createConnection(configuration);
//        Admin admin = connection.getAdmin();
        boolean tableEnabled = admin.tableExists(TableName.valueOf(isTable));
//        boolean tableEnabled = admin.isTableEnabled(isTable);
        if(tableEnabled){
            System.out.println("表"+isTable+"存在");
        }else{
            System.out.println("表"+isTable+"不存在");
        }
        //关闭连接
//        admin.close();
        return tableEnabled;
    }

    //TODO、创建表
    public static void createTable(String tableName, String... cfs) throws IOException {
        //1、创建是否存在的列族信息
        if (cfs.length < 0) {
            System.out.println("请设置列族信息！");
            return;
        }

        //2、创建是否存在的表信息
        if (isTableExist(tableName)) {
            System.out.println(tableName + "表已存在！");
            return;
        }

        //3、创建表描述器
        HTableDescriptor hTableDescriptor = new HTableDescriptor(TableName.valueOf(tableName));

        //循环添加列族
        for (String cf : cfs) {
            //5、创建列族描述器
            HColumnDescriptor hColumnDescriptor = new HColumnDescriptor(cf);

            //6、添加具体的列族信息
            hTableDescriptor.addFamily(hColumnDescriptor);
        }
        //创建表
        admin.createTable(hTableDescriptor);
    }

    //TODO 删除表
    public static void dropTable(String tableName) throws IOException {

        //1、判断表是否存在
        if (!isTableExist(tableName)) {
            System.out.println(tableName + "表不存在！！");
            return;
        }

        //使表下线
        admin.disableTable(TableName.valueOf(tableName));

        //删除表
        admin.deleteTable(TableName.valueOf(tableName));
    }


    //TODO 向表中插入数据
    public static void putData(String tableName, String rowKey, String cf, String cn, String value) throws IOException {
        //1、获取表对象
        Table table = connection.getTable(TableName.valueOf(tableName));
        //2、创建put对象
        Put put = new Put(Bytes.toBytes(rowKey));
        //3、给put赋值 列族、列、值
        put.addColumn(Bytes.toBytes(cf), Bytes.toBytes(cn), Bytes.toBytes(value));

        //4、插入数据
        table.put(put);

        table.close();
    }

    //TODO 删除表数据
    public static void deleteData(String tableName, String rowKey) throws IOException {
        //1、获取表对象
        Table table = connection.getTable(TableName.valueOf(tableName));

        Delete delete = new Delete(Bytes.toBytes(rowKey));

        //3、获取对象、只传Rowkey的时候就相当于 deleteAll、删除表示为DeleteFamily
        table.delete(delete);

        table.close();
    }


    //TODO 全表扫描表数据
    public static void getCan(String tableName) throws IOException {
        //1、获取表对象
        Table table = connection.getTable(TableName.valueOf(tableName));

        //2、创建scan对象  左闭右开  右边不包括1005
        Scan scan = new Scan();

        //3、获取对象
        ResultScanner scanner = table.getScanner(scan);

        for (Result result : scanner) {
            for (Cell c : result.rawCells()) {
                //打印数据
                System.out.print("行键:" + Bytes.toString(CellUtil.cloneRow(c)) + " ");
                System.out.print("列族:" + Bytes.toString(CellUtil.cloneFamily(c)) + " ");
                System.out.print("列:" + Bytes.toString(CellUtil.cloneQualifier(c)) + " ");
                System.out.print("值:" + Bytes.toString(CellUtil.cloneValue(c)) + " ");
                System.out.println();
            }
        }


        table.close();
    }

    //TODO 查询行
    public static void getRow(String tableName, String rowKey) throws java.lang.Exception {
        //取得一个要操作的表
        Table table = connection.getTable(TableName.valueOf(tableName));

        //设置要查询的行的rowkey
        Get get = new Get(Bytes.toBytes(rowKey));

        //设置显示多少个版本的数据
        get.setMaxVersions(3);

        //取得指定时间戳的数据
        //get.setTimeStamp(1);

        //限制要显示的列族
        //get.addFamily(Bytes.toBytes("grade"));
        //限制要显示的列
        //get.addColumn(Bytes.toBytes("course"), Bytes.toBytes("yuwen"));
        Result result = table.get(get);
        List<Cell> cells = result.listCells();
        for (Cell c : cells) {
            //注意这里 CellUtil类的使用
            System.out.print("行键:" + Bytes.toString(CellUtil.cloneRow(c)) + " ");
            System.out.print("列族:" + Bytes.toString(CellUtil.cloneFamily(c)) + " ");
            System.out.print("列:" + Bytes.toString(CellUtil.cloneQualifier(c)) + " ");
            System.out.print("值:" + Bytes.toString(CellUtil.cloneValue(c)) + " ");
            System.out.println();
        }

        //关闭资源
        table.close();
        //hbaseConn.close();
    }

    /**
     * 模糊过滤查询
     * @param tableName  表名
     * @param colName  列名
     * @param colValue 列值
     * @param cf 列族名,多个
     * @throws Exception
     */
    public static void getScanColumnValueFilter(String tableName, String cf, String colName, String colValue) throws  Exception {

        Table table = connection.getTable(TableNameValueOf(tableName));

        // 创建 Scan 对象
        Scan scan = new Scan();

        // 构造正则表达式
        String regexPattern = ".*" + Pattern.quote(colValue) + ".*";

        // 创建过滤器
        SingleColumnValueFilter filter = new SingleColumnValueFilter(
                Bytes.toBytes(cf), // 列族
                Bytes.toBytes(colName), // 列名
                CompareFilter.CompareOp.EQUAL, // 比较操作符
                new RegexStringComparator(regexPattern)); // 正则表达式

        // 设置过滤器选项
        filter.setFilterIfMissing(true); // 如果没有这个列，则过滤掉该行

        // 添加过滤器到 Scan 对象
        FilterList filters = new FilterList(FilterList.Operator.MUST_PASS_ALL);
        filters.addFilter(filter);
        scan.setFilter(filters);

        // 执行扫描并获取结果
        ResultScanner scanner = table.getScanner(scan);
        for (Result result : scanner) {
            List<Cell> cells = result.listCells();
            for (Cell c : cells) {
                //注意这里 CellUtil类的使用
                System.out.print("行键:" + Bytes.toString(CellUtil.cloneRow(c)) + " ");
                System.out.print("列族:" + Bytes.toString(CellUtil.cloneFamily(c)) + " ");
                System.out.print("列:" + Bytes.toString(CellUtil.cloneQualifier(c)) + " ");
                System.out.print("值:" + Bytes.toString(CellUtil.cloneValue(c)) + " ");
                System.out.println();
            }
        }

        // 关闭资源
        scanner.close();
        table.close();

    }


    //关闭资源
    public static void close() {
        if (admin != null) {
            try {
                admin.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (connection != null) {
            try {
                connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    //类型转换
    public static TableName TableNameValueOf(String tableName) {
        return TableName.valueOf(tableName);
    }

    public static void main(String[] args) throws Exception {

        //创建表
//        createTable("test_info", "info");

        //插入数据
//        putData("test_info", "0001", "info", "user_type", "2");
//        putData("test_info", "0001", "info", "name", "hehe");
//        putData("test_info", "0001", "info", "age", "23");

//        putData("test_info", "0002", "info", "user_type", "1");
//        putData("test_info", "0002", "info", "name", "joe");
//        putData("test_info", "0002", "info", "age", "20");

//        putData("test_info", "0003", "info", "user_type", "1");
//        putData("test_info", "0003", "info", "name", "jack");
//        putData("test_info", "0003", "info", "age", "22");

        //全表扫描数据
//        getCan("test_info");

        //查询行
//        getRow("test_info", "0004");

        //列值查询
//        getScanColumnValueFilter("test_info" , "info","name", "j");

        //删除数据
//        deleteData("test_info", "0004");

        //删除表
//        dropTable("test_info");

    }


}
