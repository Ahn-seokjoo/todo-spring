# Gradle 8.12.1 + JDK 17 베이스 이미지 사용
FROM gradle:8.12.1-jdk17 AS build
WORKDIR /app

# 1) 빌드 설정만 먼저 복사하고 의존성을 받아 둔다
#    → build.gradle.kts가 바뀔 때만 다시 실행되고, 코드만 바뀌면 캐시를 재사용
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon > /dev/null

# 소스는 그다음에 복사해서 빌드
COPY src ./src
RUN gradle bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /app/build/libs/*.jar /app/todo.jar

ENV JAVA_OPTS="-Xms256m -Xmx768m"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/todo.jar"]
