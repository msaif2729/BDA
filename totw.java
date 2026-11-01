
import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class totw {

    // MAPPER
    public static class TokenizerMapper
            extends Mapper<Object, Text, Text, IntWritable> {

        private final static IntWritable one =
                new IntWritable(1);

        public void map(Object key, Text value,
                        Context context)
                throws IOException, InterruptedException {

            StringTokenizer itr =
                    new StringTokenizer(value.toString());

            while (itr.hasMoreTokens()) {

                itr.nextToken();

                context.write(
                        new Text("total words"), one);
            }
        }
    }

    // REDUCER
    public static class IntSumReducer
            extends Reducer<Text, IntWritable,
                            Text, IntWritable> {

        private IntWritable result = new IntWritable();

        public void reduce(Text key,
                           Iterable<IntWritable> values,
                           Context context)
                throws IOException, InterruptedException {

            int sum = 0;

            for (IntWritable val : values) {
                sum += val.get();
            }

            result.set(sum);
            context.write(key, result);
        }
    }

    // DRIVER
    public static void main(String[] args) throws Exception {

        Configuration conf = new Configuration();

        Job job = Job.getInstance(conf, "totw");

        job.setJarByClass(totw.class);

        job.setMapperClass(TokenizerMapper.class);
        job.setCombinerClass(IntSumReducer.class);
        job.setReducerClass(IntSumReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        FileInputFormat.addInputPath(
                job, new Path(args[0]));

        FileOutputFormat.setOutputPath(
                job, new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}


/*

Steps To Execute:

java -version
hadoop version

start-dfs.sh
start-yarn.sh

jps

nano input.txt
Hadoop is a framework
Hadoop is used for big data
MapReduce is a programming model
Hadoop supports MapReduce


nano totw.java

javac -classpath "$(hadoop classpath)" -d . totw.java
jar -cvf totw.jar totw*.class

hdfs dfs -mkdir -p /input
hdfs dfs -put input.txt /input/
hdfs dfs -ls /input
hdfs dfs -rm -r -f /totaloutput
hadoop jar totw.jar totw /input /totaloutput
hdfs dfs -cat /totaloutput/part-r-00000
*/