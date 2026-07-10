# 멀티스테이지: 빌드(JDK) → 실행(JRE). eclipse-temurin은 linux/arm64 지원 (t4g/Graviton 대응).
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
# 의존성 레이어 분리 — 소스만 바뀌면 gradle 배포판/의존성 다운로드를 캐시에서 재사용
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon
COPY src ./src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home appuser
# plain jar는 build.gradle에서 비활성화되어 bootJar 하나만 존재
COPY --from=builder /app/build/libs/*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
