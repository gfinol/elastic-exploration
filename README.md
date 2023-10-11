# Exploiting Inherent Elasticity of Serverless in Algorithms with Unbalanced and Irregular Workloads

## Setup
There are a few things that need to be setup before running the experiments. First, you need to install the required dependencies. Then, you need to setup the AWS CLI and the AWS Lambda deployment. Finally, you need to build the project.

### Java and Maven
This project requires Java 11 and Maven. There are multiple ways to install Java 11. One way is to use [SDKMAN](https://sdkman.io/). Once SDKMAN is installed, run the following command to install Java 11:

```bash
sdk install java 11.0.20-amzn
```

You can also use SDKMAN to install Maven:

```bash
sdk install maven
```

### AWS CLI

To run the experiments you need to have an AWS account and the AWS CLI installed. To install the AWS CLI, follow the instructions [here](https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html). Once the AWS CLI is installed, you need to configure it with your AWS credentials. To do so, run the following command:

```bash
aws configure
```

### AWS Lambda deployment setup
Deployment of the functions to AWS Lambda is done using the AWS Lambda Maven Plugin, and uses a S3 bucket to store the function's code. Therefore, you need to create a S3 bucket in your AWS account.

In the [pom.xml](pom.xml) file you can find a set of empty properties that need to be filled in order to deploy the functions to AWS Lambda. The properties are:

- `lambda.awsAccountId`: the AWS account ID
- `lambda.roleName`: the name of the IAM role that will be used by the functions.
- `lambda.functionName`: the name of the function that will be deployed to AWS Lambda.
- `lambda.region`: the AWS region where the function will be deployed.
- `lambda.memorySize`: the amount of memory that will be allocated to the function.
- `lambda.timeout`: the timeout of the function.
- `lambda.s3Bucket`: the name of the S3 bucket that will be used to store the function's code.
- `lambda.functionNameSuffix`: the suffix that will be appended to the function name.

The deployment of the function to AWS Lambda is done using the following command:

```bash
mvn package shade:shade lambda:deploy-lambda -DskipTests
```

### Crucial's configuration

Fill in the [Config.java](src/main/java/eu/cloudbutton/config/Config.java) file. There are two things that need to be filled in:

- Region where the function is deployed. Example: `Regions.EU_WEST_1`
- Function name with the format: `<functionName>-<functionNameSuffix>`. 

This configuration is used to invoke the functions. 

## How to build the project
This project uses Maven as a build tool. To build the project, run the following command:

```bash
mvn clean package
```

## How to run the experiments

Once the project has been built, you will find the jar file inside the `target` folder. To easily run the experiments, you can navigate to the `target` folder:

```bash
cd target
```

You can run the experiments using the following command:

```bash
java -cp utslambda-1.0.jar <experiment.class.name> <experiment.args>
```

Where `<experiment.class.name>` is the name of the class that contains the experiment to run, and `<experiment.args>` are the arguments that will be passed to the experiment. In the following sections you can find the list of experiments that can be run, and the arguments that they require.

Note that all the algorithms have at least three different implementations: 
1. Serverless: uses AWS Lambda to run the tasks.
2. Multiprocess: uses threads to run the tasks.
3. Hybrid: uses a combination of AWS Lambda and threads to run the tasks.

### UTS

The UTS experiments uses the following arguments:


- `-help`: Prints help message and quit.
- `-workers <INT>` or `-w <INT>`: Sets the the number of max concurrent threads/functions used. If 0, uses the  default.
- `-depth <INT>` or `-d <INT>`: Sets the depth to be used
- `-warmupDepth <INT>`: Sets the depth to be used for warmup. 

There are various implementations of the UTS algorithm that can be run. All the implementations are in the `eu.cloudbutton.utslambda` package. Here is a list of the classes that contain the implementations of the UTS algorithm:

- `multithread.MultithreadUTS`: contains the implementation of the UTS algorithm using threads.
- `serverless.taskmanager.TMServerlessUT`: contains the implementation of the UTS algorithm using AWS Lambda.
- `serverless.taskmanager.TMServerlessHybridUTS`: contains the implementation of the UTS algorithm using a combination of threads and AWS Lambda.


So, for example. To run the Serverless implementation with a depth of 17, a maximum concurrency of 100 and a warmup phase with depth 15, you can use the following command:

```bash
java -cp utslambda-1.0.jar eu.cloudbutton.utslambda.serverless.taskmanager.TMServerlessUTS -depth 17 -warmupDepth 15 -workers 100
```

### Mandelbrot with Mariani Silver

The Mandelbrot with Mariani Silver experiments uses the following arguments:

- `-help`: Prints help message and quit.
- `-workers <INT>` or `-w <INT>`: Sets the the max number of concurrent threads/functions used. If 0, uses the  default.
- `-width <INT>`: Sets the width of the image.
- `-height <INT>`: Sets the height of the image.

There are various implementations of the Mandelbrot with Mariani Silver algorithm that can be run. All the implementations are in the `eu.cloudbutton.mandelbrot` package. Here is a list of the classes that contain the implementations of the Mandelbrot with Mariani Silver algorithm:

- `MarianiSilverParallel`: contains the implementation of the Mandelbrot with Mariani Silver algorithm using threads. Threads use shared data structures.
- `parallelns.MarianiSilverParallelNS`: contains the implementation of the Mandelbrot with Mariani Silver algorithm using threads. Threads do not use shared data structures.
- `serverless.MarianiSilverServerless`: contains the implementation of the Mandelbrot with Mariani Silver algorithm using AWS Lambda.
- `serverless.MarianiSilverServerlessHybrid`: contains the implementation of the Mandelbrot with Mariani Silver algorithm using a combination of threads and AWS Lambda.

So, for example. To run the Serverless implementation with an image of 1024x1024 pixels, a maximum concurrency of 100, you can use the following command:

```bash
java -cp utslambda-1.0.jar eu.cloudbutton.mandelbrot.serverless.MarianiSilverServerless -width 1024 -height 1024 -workers 100
```


### Betweenness Centrality

The Betweenness Centrality experiments uses the following arguments:

- `-v <INT>`: Set the verbosity level to be used.
- `-s <LONG>`: Set the the seed for the random number.
- `-n <INT>`: Set the number of vertices = $2^n$.
- `-w <INT>`: Sets the the max number of concurrent threads/functions used.
- `-g <INT>`: Set the number of vertices per task.
- `-a <DOUBLE>`: Set the probability a. Default is $0.55$.
- `-b <DOUBLE>`: Set the probability b. Default is $0.1$.
- `-c <DOUBLE>`: Set the probability c. Default is $0.1$.
- `-d <DOUBLE>`: Set the probability d. Default is $0.25$.
- `-p <INT>`: Set the permutation to be used.
- `-help`: Prints help message and quit.

There are various implementations of the Betweenness Centrality algorithm that can be run. All the implementations are in the `eu.cloudbutton.bc` package. Here is a list of the classes that contain the implementations of the Betweenness Centrality algorithm:

- `multithread.MultithreadBC`: contains the implementation of the Betweenness Centrality algorithm using threads.
- `serverless.ServerlessBC`: contains the implementation of the Betweenness Centrality algorithm using AWS Lambda.
- `hybrid.HybridBC`: contains the implementation of the Betweenness Centrality algorithm using a combination of threads and AWS Lambda.

So, for example. To run the Serverless implementation with $2^{17}$ vertices, a maximum concurrency of 100 and 64 vertices per task, you can use the following command:

```bash
java -cp utslambda-1.0.jar eu.cloudbutton.bc.serverless.ServerlessBC -n 17 -w 100 -g 64
```