FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY src ./src
COPY data ./data
COPY index.html .

RUN mkdir out && javac -d out src/*.java

EXPOSE 18766

CMD ["java", "-cp", "out", "CfbWebServer"]