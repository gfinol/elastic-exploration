# we'll use Amazon Linux 2 + Corretto 11 as our base
FROM maven:3.8.4-jdk-11 as base

# configure the build environment
FROM base as build
WORKDIR /src

# cache and copy dependencies
ADD pom.xml .
RUN mvn dependency:go-offline dependency:copy-dependencies

# compile the function
ADD . .
RUN mvn package

# copy the function artifact and dependencies onto a clean base
FROM base
WORKDIR /function

COPY --from=build /src/target/dependency/*.jar ./
COPY --from=build /src/target/*.jar ./

#RUN sysctl kernel.perf_event_paranoid=1
#RUN sysctl kernel.kptr_restrict=0

RUN apt update && \
    apt -y install openjdk-11-dbg
ADD https://github.com/jvm-profiling-tools/async-profiler/releases/download/v2.6/async-profiler-2.6-linux-x64.tar.gz .
RUN tar xvzf async-profiler-2.6-linux-x64.tar.gz

# configure the runtime startup as main
ENTRYPOINT [ "/usr/bin/java", "-agentpath:/function/async-profiler-2.6-linux-x64/build/libasyncProfiler.so=start,event=cache-misses,summary,fdtransfer,file=/temp/profile.txt", "-cp", "./*", "com.amazonaws.services.lambda.runtime.api.client.AWSLambda" ]
# pass the name of the function handler as an argument to the runtime
CMD [ "crucial.execution.aws.AWSLambdaHandler::handleRequest" ]