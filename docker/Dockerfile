# 仓枢 WareHub - 单 JAR 部署镜像
# 构建前置：已在外部通过 mvn package 生成 warehub.jar（含三端前端静态资源）
FROM eclipse-temurin:17-jre AS runtime

WORKDIR /app

# 运行参数
ENV SPRING_PROFILES_ACTIVE=prod
ENV DB_HOST=127.0.0.1
ENV DB_PORT=3306
ENV DB_USERNAME=root
ENV DB_PASSWORD=root
ENV TZ=Asia/Shanghai

# 复制已构建的单 JAR
COPY warehub.jar /app/warehub.jar

EXPOSE 8080

# 启动：单 JAR，内置 Tomcat + 三端静态资源
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "/app/warehub.jar", "--spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]
