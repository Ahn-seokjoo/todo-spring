# Gradle 8.12.1 + JDK 17 베이스 이미지 사용
FROM gradle:8.12.1-jdk17 AS build

WORKDIR /app

# 필요한 설정 파일 먼저 복사
COPY build.gradle.kts settings.gradle.kts ./

RUN gradle dependencies --no-daemon

# 전체 소스 복사
COPY . /app

# 빌드 실행
RUN gradle clean build --no-daemon -x test

# JAR 파일 목록 출력 (파일이 생성되었는지 확인)
RUN ls -al /app/build/libs

# 런타임 이미지는 슬림한 JDK 17 이미지 사용
# 가장 취약점 없어보이는 걸로 선택
# https://hub.docker.com/_/openjdk/tags?name=17-jdk-slim
FROM openjdk:21-ea-17-jdk-slim-buster

WORKDIR /app

ENV JAVA_OPTS="-Dspring.profiles.active=prod"
# 빌드한 JAR 복사
COPY --from=build /app/build/libs/*.jar /app/todo.jar

EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=prod"]
CMD ["-jar", "todo.jar"]
