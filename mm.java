
import java.io.IOException;
import java.util.HashMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

public class mm {

    // MAPPER
    public static class Map
            extends Mapper<LongWritable, Text, Text, Text> {

        public void map(LongWritable key, Text value,
                        Context context)
                throws IOException, InterruptedException {

            Configuration conf = context.getConfiguration();

            int m = Integer.parseInt(conf.get("m"));
            int p = Integer.parseInt(conf.get("p"));

            String line = value.toString();

            String[] indicesAndValue = line.split(",");

            Text outputKey = new Text();
            Text outputValue = new Text();

            if (indicesAndValue[0].equals("A")) {

                for (int k = 0; k < p; k++) {

                    outputKey.set(
                            indicesAndValue[1] + "," + k);

                    outputValue.set(
                            "A," + indicesAndValue[2]
                            + "," + indicesAndValue[3]);

                    context.write(outputKey, outputValue);
                }

            } else {

                for (int i = 0; i < m; i++) {

                    outputKey.set(
                            i + "," + indicesAndValue[2]);

                    outputValue.set(
                            "B," + indicesAndValue[1]
                            + "," + indicesAndValue[3]);

                    context.write(outputKey, outputValue);
                }
            }
        }
    }

    // REDUCER
    public static class Reduce
            extends Reducer<Text, Text, Text, Text> {

        public void reduce(Text key, Iterable<Text> values,
                           Context context)
                throws IOException, InterruptedException {

            HashMap<Integer, Float> hashA =
                    new HashMap<Integer, Float>();

            HashMap<Integer, Float> hashB =
                    new HashMap<Integer, Float>();

            for (Text val : values) {

                String[] value = val.toString().split(",");

                if (value[0].equals("A")) {

                    hashA.put(
                            Integer.parseInt(value[1]),
                            Float.parseFloat(value[2]));

                } else {

                    hashB.put(
                            Integer.parseInt(value[1]),
                            Float.parseFloat(value[2]));
                }
            }

            int n = Integer.parseInt(
                    context.getConfiguration().get("n"));

            float result = 0.0f;

            for (int j = 0; j < n; j++) {

                float a_ij = hashA.containsKey(j)
                        ? hashA.get(j) : 0.0f;

                float b_jk = hashB.containsKey(j)
                        ? hashB.get(j) : 0.0f;

                result += a_ij * b_jk;
            }

            if (result != 0.0f) {

                context.write(
                        null,
                        new Text(key.toString() + ","
                                + Float.toString(result)));
            }
        }
    }

    // DRIVER
    public static void main(String[] args) throws Exception {

        Configuration conf = new Configuration();

        // A is 2 x 5
        // B is 5 x 3

        conf.set("m", "2");
        conf.set("n", "5");
        conf.set("p", "3");

        Job job = Job.getInstance(
                conf, "MatrixMatrixMultiplicationOneStep");

        job.setJarByClass(mm.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        job.setMapperClass(Map.class);
        job.setReducerClass(Reduce.class);

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

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

nano input2.txt

A,0,0,1.0
A,0,1,1.0
A,0,2,2.0
A,0,3,3.0
A,0,4,4.0
A,1,0,5.0
A,1,1,6.0
A,1,2,7.0
A,1,3,8.0
A,1,4,9.0
B,0,0,1.0
B,0,1,2.0
B,0,2,3.0
B,1,0,4.0
B,1,1,5.0
B,1,2,6.0
B,2,0,7.0
B,2,1,8.0
B,2,2,9.0
B,3,0,10.0
B,3,1,11.0
B,3,2,12.0
B,4,0,13.0
B,4,1,14.0
B,4,2,15.0



nano mm.java

javac -classpath "$(hadoop classpath)" -d . mm.java
jar -cvf mm.jar mm*.class

hdfs dfs -mkdir -p /input
hdfs dfs -put input.txt /input/
hdfs dfs -ls /input
hdfs dfs -rm -r -f /output
hadoop jar mm.jar mm /input /output
hdfs dfs -cat /output/part-r-00000
*/