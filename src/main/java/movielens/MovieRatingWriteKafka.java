package movielens;

import com.alibaba.fastjson.JSONObject;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class MovieRatingWriteKafka {

    public static void main(String[] args) throws InterruptedException, ExecutionException {

        String kafkaAddress = "192.168.150.137:9092";
        String topicName = "movie_rate";
        String groupId = "movie_group";

        // 设置 Kafka 集群的地址
        Properties props = new Properties();
        props.put("bootstrap.servers", kafkaAddress);

        // 设置消息的序列化器
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("acks", "all");
        props.put("request.timeout.ms", "6000");
//        props.put("batch.size", "1024KB");
        // 创建 Kafka Producer 实例
        Producer<String, String> producer = new KafkaProducer<>(props);

        // 写kafka测试
//        DataWriteKafka(producer, topicName);
        // 模拟数据，并写入kafka
        readFileWriteKafka(producer, topicName);
        // 关闭生产者
        producer.close();
    }
    public static void readFileWriteKafka(Producer producer, String topic){
        Random random = new Random();
        //userId,Id,rating,timestamp
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try (BufferedReader reader = new BufferedReader(new FileReader("./data/ml-latest-small/ratings.csv"))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                count += 1;
                if(count == 1){continue;}
                //制表符\t分割记录
                String[] record = line.split(",");
                JSONObject jsonObj = new JSONObject();

                jsonObj.put("userId", Integer.valueOf(record[0]));
                jsonObj.put("movieId", Integer.valueOf(record[1]));
                jsonObj.put("rating", Double.valueOf(record[2]));
                jsonObj.put("timestamp", Long.valueOf(record[3]));
                jsonObj.put("createTime", dateFormat.format(new Date()));

                String key = String.valueOf(count);
                String value = jsonObj.toJSONString();
                producer.send(new ProducerRecord<>(topic, key, value)).get();
                System.out.println(jsonObj.toString());
                Thread.sleep(random.nextInt(50));  // 控制写入速度
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

    }




}
