package com.bigdata.utils;

import com.bigdata.domain.MovieRating;
import com.bigdata.domain.MovieRatingCount;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.RegexStringComparator;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HbaseUtil {

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

        //关闭连接
//        admin.close();
        return tableEnabled;
    }

    //1、 TODO、创建表
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

    //2、TODO 删除表
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


    //５、TODO 向表中插入数据
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

    //7、TODO 删除表数据
    public static void deleteData(String tableName, String rowKey, String cf, String cn) throws IOException {
        //1、获取表对象
        Table table = connection.getTable(TableName.valueOf(tableName));

        Delete delete = new Delete(Bytes.toBytes(rowKey));

        //3、获取对象、只传Rowkey的时候就相当于 deleteAll、删除表示为DeleteFamily
        table.delete(delete);

        table.close();
    }


    //６、TODO 扫描表数据
    public static void getsCan(String tableName) throws IOException {
        //1、获取表对象
        Table table = connection.getTable(TableName.valueOf(tableName));

        //2、创建scan对象  左闭右开  右边不包括1005
        Scan scan = new Scan(Bytes.toBytes("1001"), Bytes.toBytes("1003"));

        //3、获取对象
        ResultScanner scanner = table.getScanner(scan);

        for (Result result : scanner) {
            for (Cell cell : result.rawCells()) {
                //打印数据
                System.out.println("Scan CF: " + Bytes.toString(CellUtil.cloneFamily(cell)));
                System.out.println("Scan CN: " + Bytes.toString(CellUtil.cloneQualifier(cell)));
                System.out.println("Scan Value: " + Bytes.toString(CellUtil.cloneValue(cell)));
            }
        }


        table.close();
    }

    //６、TODO 获取表数据
    public static List<MovieRating> getData(String tableName, String rowKey, String cf, String cn) throws IOException {
        //1、获取表对象
        Table table = connection.getTable(TableName.valueOf(tableName));

        //2、创建Get对象
        Get get = new Get(Bytes.toBytes(rowKey));

        //2.1指定获取列族
        get.addFamily(Bytes.toBytes(cf));

        //2.2设置查询的列族和列的条件
//        get.addColumn(Bytes.toBytes(cf), Bytes.toBytes(cn));

        //2.3 设置获取数据的版本
        get.setMaxVersions(5);

        //3、获取对象
        Result result = table.get(get);

        //4、解析数据, 并装载数据
        ArrayList<MovieRating> ratingList = new ArrayList<>();
        MovieRating rate = new MovieRating();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (Cell cell : result.rawCells()) {
            String cf1 = Bytes.toString(CellUtil.cloneFamily(cell));
            String cn1 = Bytes.toString(CellUtil.cloneQualifier(cell));
            String value = Bytes.toString(CellUtil.cloneValue(cell));

            if(cn1.equals("movieId")){
                rate.setMovieId(value);

            }else if(cn1.equals("avg_rating")){
                rate.setAvgRating(value);
            }else if(cn1.equals("timestamp")){
                rate.setTimestamp(dateFormat.format(new Date(Long.valueOf(value))));
            }
            //打印数据
//            System.out.println("CF: " + Bytes.toString(CellUtil.cloneFamily(cell)));
//            System.out.println("CN: " + Bytes.toString(CellUtil.cloneQualifier(cell)));
//            System.out.println("Value: " + Bytes.toString(CellUtil.cloneValue(cell)));
        }
        ratingList.add(rate);

//        System.out.println(rate.toString());
        table.close();
        return ratingList;
    }

    public static List<MovieRating> getScanSingleColumnValueFilter(String tableName, String cf, String colName, String colValue) throws  Exception {

        Table table = connection.getTable(TableName.valueOf(tableName));

        // 创建 Scan 对象
        Scan scan = new Scan();
        scan.setCaching(10); // 设置每次查询返回的记录数

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
        String cf1 = "movieinfo";
        String cf2 = "rateinfo";

        //解析数据, 并装载数据
        ArrayList<MovieRating> ratingList = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        int count = 0;
        for (Result result : scanner) {

            count += 1;
            MovieRating rate = new MovieRating();

            // 处理每一行数据
            byte[] rowKey = result.getRow();

            // 获取列值
            byte[] movieId = result.getValue(Bytes.toBytes(cf1), Bytes.toBytes("movieId"));
            byte[] title = result.getValue(Bytes.toBytes(cf1), Bytes.toBytes("title"));
            byte[] genres = result.getValue(Bytes.toBytes(cf1), Bytes.toBytes("genres"));

            byte[] maxRating = result.getValue(Bytes.toBytes(cf2), Bytes.toBytes("max_rating"));
            byte[] avgRating = result.getValue(Bytes.toBytes(cf2), Bytes.toBytes("avg_rating"));
            byte[] minRating = result.getValue(Bytes.toBytes(cf2), Bytes.toBytes("min_rating"));

            byte[] timestamp = result.getValue(Bytes.toBytes(cf2), Bytes.toBytes("timestamp"));

            rate.setMovieId(Bytes.toString(movieId));
            rate.setMovieName(Bytes.toString(title));
            rate.setMovieGenres(Bytes.toString(genres));
            rate.setMaxRating(Bytes.toString(maxRating));
            rate.setAvgRating(Bytes.toString(avgRating));
            rate.setMinRating(Bytes.toString(minRating));
            rate.setTimestamp(Bytes.toString(timestamp));

//            System.out.println(rate.toString());
            ratingList.add(rate);
            if(count == 10){break;}
        }
        //按平均分排序
        ratingList = (ArrayList<MovieRating>) ratingList.stream().sorted(Comparator.comparing(MovieRating::getAvgRating).reversed()).collect(Collectors.toList());

        // 关闭资源
        scanner.close();
        table.close();

        return ratingList;

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

    /**
     * 查询热门电影TopN - 按startTime降序和count降序排列
     * @param tableName 表名
     * @param topN 返回的记录数量
     * @return 排序后的MovieRatingCount对象列表
     */
    public static List<MovieRatingCount> getTopMoviesByTimeAndCount(String tableName, int topN) throws IOException {
        Table table = connection.getTable(TableName.valueOf(tableName));
        List<MovieRatingCount> resultList = new ArrayList<>();

        try {
            // 创建Scan对象扫描全表
            Scan scan = new Scan();
            // 指定需要查询的列，减少网络传输
            scan.addFamily(Bytes.toBytes("rateinfo"));
            scan.addColumn(Bytes.toBytes("rateinfo"), Bytes.toBytes("startTime"));
            scan.addColumn(Bytes.toBytes("rateinfo"), Bytes.toBytes("endTime"));
            scan.addColumn(Bytes.toBytes("rateinfo"), Bytes.toBytes("name"));
            scan.addColumn(Bytes.toBytes("rateinfo"), Bytes.toBytes("count"));
            scan.addColumn(Bytes.toBytes("rateinfo"), Bytes.toBytes("avgRating"));
            scan.addColumn(Bytes.toBytes("rateinfo"), Bytes.toBytes("updateTime"));

            // 设置缓存大小提高扫描性能
            scan.setCaching(1000);
            scan.setCacheBlocks(false);

            ResultScanner scanner = table.getScanner(scan);

            // 收集所有结果并转换为MovieRatingCount对象
            for (Result result : scanner) {
                if (result.isEmpty()) continue;

                MovieRatingCount movieRatingCount = convertResultToMovieRatingCount(result);
                if (movieRatingCount != null) {
                    resultList.add(movieRatingCount);
                }
            }

            scanner.close();

            // 按startTime降序、count降序排序
            Collections.sort(resultList, new Comparator<MovieRatingCount>() {
                @Override
                public int compare(MovieRatingCount m1, MovieRatingCount m2) {
                    try {
                        // 首先按startTime降序排列
                        int startTimeCompare = m2.getStartTime().compareTo(m1.getStartTime());
                        int endTimeCompare = m2.getEndTime().compareTo(m1.getEndTime());

                        if (startTimeCompare != 0) {
                            return startTimeCompare;
                        }

                        // 如果startTime相同，按count降序排列
                        return Integer.compare(m2.getCount(), m1.getCount());

                    } catch (Exception e) {
                        return 0;
                    }
                }
            });

            // 返回TopN结果
            int returnSize = Math.min(topN, resultList.size());
            return resultList.subList(0, returnSize);

        } finally {
            table.close();
        }
    }

    /**
     * 将HBase Result转换为MovieRatingCount对象
     */
    private static MovieRatingCount convertResultToMovieRatingCount(Result result) {
        if (result == null || result.isEmpty()) {
            return null;
        }

        try {
            MovieRatingCount movieRatingCount = new MovieRatingCount();

            // 解析各个列的值并设置到对象中
            byte[] startTimeBytes = result.getValue(Bytes.toBytes("rateinfo"), Bytes.toBytes("startTime"));
            byte[] endTimeBytes = result.getValue(Bytes.toBytes("rateinfo"), Bytes.toBytes("endTime"));
            byte[] nameBytes = result.getValue(Bytes.toBytes("rateinfo"), Bytes.toBytes("name"));
            byte[] countBytes = result.getValue(Bytes.toBytes("rateinfo"), Bytes.toBytes("count"));
            byte[] avgRatingBytes = result.getValue(Bytes.toBytes("rateinfo"), Bytes.toBytes("avgRating"));
            byte[] updateTimeBytes = result.getValue(Bytes.toBytes("rateinfo"), Bytes.toBytes("updateTime"));

            if (startTimeBytes != null) {
                movieRatingCount.setStartTime(Bytes.toString(startTimeBytes));
            }

            if (endTimeBytes != null) {
                movieRatingCount.setEndTime(Bytes.toString(endTimeBytes));
            }

            if (nameBytes != null) {
                movieRatingCount.setName(Bytes.toString(nameBytes));
            }

            if (countBytes != null) {
                try {
                    movieRatingCount.setCount(Integer.parseInt(Bytes.toString(countBytes)));
                } catch (NumberFormatException e) {
                    movieRatingCount.setCount(0); // 设置默认值
                }
            }

            if (avgRatingBytes != null) {
                try {
                    movieRatingCount.setAvgRating(Double.parseDouble(Bytes.toString(avgRatingBytes)));
                } catch (NumberFormatException e) {
                    movieRatingCount.setAvgRating(0.0); // 设置默认值
                }
            }

            if (updateTimeBytes != null) {
                movieRatingCount.setUpdateTime(Bytes.toString(updateTimeBytes));
            }

            return movieRatingCount;

        } catch (Exception e) {
            System.err.println("转换Result为MovieRatingCount对象失败: " + e.getMessage());
            return null;
        }
    }



    public static void main(String[] args) throws Exception {
        HbaseUtil hbaseUtil = new HbaseUtil();
        //        getData("movie_rate", "1", "rateinfo", "userId");
        List<MovieRating> scanSingleColumnValueFilter = hbaseUtil.getScanSingleColumnValueFilter("movie_rate_test", "movieinfo", "title", "father");

        for(MovieRating movieRating:scanSingleColumnValueFilter){
            System.out.println(movieRating.toString());
        }

    }

}
