FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY src/JvmLab.java /app/src/JvmLab.java

RUN javac -d /app/classes /app/src/JvmLab.java \
    && mkdir -p /workspace/dumps /workspace/logs /workspace/jfr
	
COPY run.sh /app/run.sh
RUN chmod +x /app/run.sh

ENTRYPOINT ["/app/run.sh"]