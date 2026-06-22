FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /workspace

COPY pom.xml mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY personal-blog-common/pom.xml personal-blog-common/pom.xml
COPY personal-blog-admin/pom.xml personal-blog-admin/pom.xml

RUN mvn -pl personal-blog-admin -am dependency:go-offline

COPY personal-blog-common/src personal-blog-common/src
COPY personal-blog-admin/src personal-blog-admin/src

RUN mvn -pl personal-blog-admin -am clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /workspace/personal-blog-admin/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
