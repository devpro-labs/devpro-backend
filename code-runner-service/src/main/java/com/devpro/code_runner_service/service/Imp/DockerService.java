package com.devpro.code_runner_service.service.Imp;

import com.devpro.code_runner_service.DTO.CustomResponse;
import com.devpro.code_runner_service.DTO.DockerRunner;
import com.devpro.code_runner_service.DTO.FileNode;
import com.devpro.code_runner_service.DTO.PreviewURL;
import com.devpro.code_runner_service.config.socket_configs.LogWebSocketHandler;
import com.devpro.code_runner_service.models.Problem;
import com.devpro.code_runner_service.models.ServiceType;
import com.devpro.code_runner_service.service.IDockerRepo;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
public class DockerService implements IDockerRepo {

    private final DockerClient dockerClient;
    private final LogWebSocketHandler logWebSocketHandler;
    private final WebClient webClient;

    @Value("${traefik.domain}")
    private String domain;

    @Value("${traefik.network}")
    private String network;

    @Value("${traefik.protocol}")
    private String protocol;


    @Value("${host_workdir}")
    private String hostWorkdir;


    private static final int TIME_LIMIT_SECONDS = 5; // ⏱️ change per problem

    public DockerService(DockerClient dockerClient, LogWebSocketHandler logWebSocketHandler, WebClient webClient) {
        this.dockerClient = dockerClient;
        this.logWebSocketHandler = logWebSocketHandler;
        this.webClient = webClient;
    }

    private void runWithTimeLimit(String containerId, String command) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        Future<?> future = executor.submit(() -> {
            try {
                exec(containerId, command);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        try {
            future.get(TIME_LIMIT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            // 🔥 TLE — kill container
            dockerClient.killContainerCmd(containerId).exec();
            throw new RuntimeException("Time Limit Exceeded");
        } finally {
            executor.shutdownNow();
        }
    }


    private void writeFileTree(List<FileNode> files, Path basePath) throws Exception {
        if (files == null) return;

        for (FileNode node : files) {
            Path currentPath = basePath.resolve(node.getName());

            boolean hasChildren = node.getChildren() != null && !node.getChildren().isEmpty();

            if (hasChildren) {
                Files.createDirectories(currentPath);

                writeFileTree(node.getChildren(), currentPath);

                if (node.getContent() != null && !node.getContent().isBlank()) {
                    Path indexFile = currentPath.resolve("index.txt");
                    Files.writeString(
                            indexFile,
                            node.getContent(),
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING
                    );
                }
            } else {
                Files.createDirectories(currentPath.getParent());
                Files.writeString(
                        currentPath,
                        node.getContent() == null ? "" : node.getContent(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING
                );
            }
        }
    }

    private void printFileNodeTree(List<FileNode> files, String indent) {
        if (files == null) return;

        for (FileNode node : files) {
            if (node.isFolder()) {
                System.out.println(indent + "📁 " + node.getName());
                printFileNodeTree(node.getChildren(), indent + "  ");
            } else {
                int size = node.getContent() == null ? 0 : node.getContent().length();
                System.out.println(indent + "📄 " + node.getName() + " (" + size + " chars)");
            }
        }
    }

    private void streamContainerLogs(String containerId, String submissionId) {

        new Thread(() -> {
            try {

                StringBuilder errorBuffer = new StringBuilder();

                dockerClient.logContainerCmd(containerId)
                        .withStdOut(true)
                        .withStdErr(true)
                        .withFollowStream(true)
                        .withTail(100)
                        .exec(new ResultCallback.Adapter<Frame>() {

                            @Override
                            public void onNext(Frame frame) {

                                String log = new String(frame.getPayload(), StandardCharsets.UTF_8);

                                // Always send raw logs
                                logWebSocketHandler.sendEvent(
                                        submissionId,
                                        "LOG",
                                        log
                                );

                                // If STDERR → treat as runtime error
                                if (frame.getStreamType() == StreamType.STDERR) {

                                    errorBuffer.append(log);

                                    logWebSocketHandler.sendEvent(
                                            submissionId,
                                            "ERROR",
                                            log
                                    );
                                }
                            }

                            @Override
                            public void onComplete() {

                                // If there was a collected error stack
                                if (!errorBuffer.isEmpty()) {
                                    logWebSocketHandler.sendEvent(
                                            submissionId,
                                            "ERROR",
                                            errorBuffer.toString()
                                    );
                                }

                                logWebSocketHandler.sendEvent(
                                        submissionId,
                                        "LOG",
                                        "Container stopped"
                                );

                                logWebSocketHandler.removeSession(submissionId);

                                System.out.println(
                                        "[CONTAINER " + containerId.substring(0, 6) + "] LOG STREAM CLOSED"
                                );
                            }

                            @Override
                            public void onError(Throwable throwable) {

                                logWebSocketHandler.sendEvent(
                                        submissionId,
                                        "ERROR",
                                        "Log stream failed: " + throwable.getMessage()
                                );

                                logWebSocketHandler.removeSession(submissionId);

                                throwable.printStackTrace();
                            }
                        });

            } catch (Exception e) {

                logWebSocketHandler.sendEvent(
                        submissionId,
                        "ERROR",
                        "Failed to stream logs: " + e.getMessage()
                );

                e.printStackTrace();
            }
        }).start();
    }


    private void execPostgres(String sql) throws Exception {

        String safeSql = sql.replace("\"", "\\\"");

        String cmd = String.format(
                "PGPASSWORD=password psql -U user -d db -c \"%s\"",
                safeSql
        );

        String execId = dockerClient.execCreateCmd("problem-postgres")
                .withCmd("sh", "-c", cmd)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .exec()
                .getId();


        dockerClient.execStartCmd(execId)
                .start()
                .awaitCompletion();
    }


    private void execMongo(String cmd) throws Exception {

        String execId = dockerClient.execCreateCmd("problem-mongodb")
                .withCmd("sh", "-c", cmd)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .exec()
                .getId();

        dockerClient.execStartCmd(execId)
                .start()
                .awaitCompletion();
    }

    private void createMongoDatabase(String previewId) throws Exception {

        String dbName = "mongo_" + previewId;
        String dbUser = "user_" + previewId;
        String dbPass = "pass_" + previewId;

        String js = String.format("""
        mongosh -u user -p password --authenticationDatabase admin --eval "
        db = db.getSiblingDB('admin');
        db.createUser({
            user: '%s',
            pwd: '%s',
            roles: [{ role: 'readWrite', db: '%s' }]
        });
        db.test.insertOne({ init: true });
        "
    """, dbUser, dbPass, dbName);

        execMongo(js);

        System.out.println("✅ MongoDB ready: " + dbName);
    }

    private void waitForMongoReady(String dbName, String user, String pass) throws Exception {

        for (int i = 0; i < 10; i++) {
            try {

                String cmd = String.format("""
                mongosh -u %s -p %s --authenticationDatabase admin --eval "db.runCommand({ ping: 1 })"
            """, user, pass);

                execMongo(cmd);

                System.out.println("✅ Mongo ready");
                return;

            } catch (Exception ignored) {}

            Thread.sleep(1000);
        }

        throw new RuntimeException("Mongo not ready");
    }

    private void createRedisUser(String previewId) throws Exception {

        String redisUser = "user_" + previewId;
        String redisPass = "pass_" + previewId;
        String prefix = "preview_" + previewId + ":";

        try {
            String command = String.format(
                    "ACL SETUSER %s on >%s ~%s* +@all",
                    redisUser,
                    redisPass,
                    prefix
            );

            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "exec", "-i", "problem-redis",
                    "redis-cli",
                    command
            );

            Process process = pb.start();
            process.waitFor();

            System.out.println("✅ Created Redis user: " + redisUser);

        } catch (Exception e) {
            System.out.println("Redis user may already exist: " + redisUser);
        }
    }

    private String buildStartCommand(String image) {
        return switch (image) {
            case "fastapi-py" -> "uvicorn index:app --host 0.0.0.0 --port 3000";
            case "express-js" -> "node src/index.js";
            case "express-ts" -> "npx tsc && node dist/index.js";
            default -> null;
        };
    }

    private void exec(String containerId, String cmd) throws Exception {
        String id = dockerClient.execCreateCmd(containerId)
                .withCmd("sh", "-c", cmd)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .exec()
                .getId();

        dockerClient.execStartCmd(id).start().awaitCompletion();
    }

    private void waitForServer(String baseUrl, String previewId) {
        int retries = 15;

        for (int i = 0; i < retries; i++) {
            try {
                ResponseEntity<Void> res = webClient.get()
                        .uri(baseUrl)
                        .exchangeToMono(response -> response.toBodilessEntity())
                        .block();

                if (res != null && (res.getStatusCode().is2xxSuccessful() || res.getStatusCode().is3xxRedirection() || res.getStatusCode().is4xxClientError())) {
                    log.info("✅ Server FULLY READY for {}", previewId);
                    return;
                }

                log.warn("⏳ Waiting... {}", i + 1);

            } catch (Exception e) {
                log.warn("⏳ Waiting... {}", i + 1);
                log.info(e.toString());
            }

            try { Thread.sleep(5000); } catch (Exception ignored) {}
        }

        throw new RuntimeException("❌ Server not ready");
    }
    private void cleanupResources(String previewId) {
        try {
            String safeId = previewId.replace("-", "_");

            String dbName = "db_" + safeId;
            String dbUser = "user_" + safeId;
            String mongoDb = "mongo_" + safeId;
            String redisUser = "user_" + safeId;

            // 🔹 POSTGRES CLEANUP
            execPostgres(String.format("DROP DATABASE IF EXISTS %s;", dbName));
            execPostgres(String.format("DROP ROLE IF EXISTS %s;", dbUser));

            // 🔹 MONGODB CLEANUP
            execMongo(String.format("""
                        db = db.getSiblingDB('%s');
                        db.dropDatabase();
                    
                        db = db.getSiblingDB('admin');
                        db.dropUser('%s');
                    """, mongoDb, dbUser));

            // 🔹 REDIS CLEANUP
//            ProcessBuilder redisPb = new ProcessBuilder(
//                    "docker", "exec", "-i", "problem-redis",
//                    "redis-cli",
//                    "ACL", "DELUSER", redisUser
//            );
//            redisPb.start().waitFor();

            log.info("✅ Cleaned resources for {}", previewId);

        } catch (Exception e) {
            log.error("Cleanup failed for {}", previewId, e);
        }
    }
    private void printFileTree(Path root) throws IOException {
        if (!Files.exists(root)) {
            System.out.println("❌ Path does not exist: " + root);
            return;
        }

        Files.walk(root)
                .forEach(path -> {
                    try {
                        String indent = "  ".repeat(root.relativize(path).getNameCount());
                        if (Files.isDirectory(path)) {
                            System.out.println(indent + "📁 " + path.getFileName());
                        } else {
                            System.out.println(indent + "📄 " + path.getFileName() +
                                    " (" + Files.size(path) + " bytes)");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    @Override
    public CustomResponse getPreviewURL(DockerRunner runner, Problem problem, String previewId) {
        try {
            System.out.println("========== PREVIEW REQUEST ==========");

            //input file
            printFileNodeTree(runner.getFiles(), "");


            String projectRoot = Paths.get(hostWorkdir, "preview-" + previewId).toString();

            //root path
            System.out.println("root path is " + projectRoot);

            File projectDir = new File(projectRoot);
            projectDir.mkdirs();


            writeFileTree(runner.getFiles(), projectDir.toPath());
            //write files into file
            System.out.println("files are written");

            //get image and container
            String image = runner.getImage_name();
            String containerName = "preview-" + previewId;

            // 🔹 Ensure image exists
            try {
                //inspect image
                dockerClient.inspectImageCmd(image).exec();
            } catch (Exception e) {
                dockerClient.pullImageCmd(image).start().awaitCompletion();
            }

            // envList
            List<String> envList = new ArrayList<>();
            envList.add("PORT=3000");

            // 🔹 Traefik labels
            Map<String, String> labels = new HashMap<>();
            String postfix_url = containerName + "." + domain;

            // Router
            labels.put("traefik.http.routers." + containerName + ".rule",
                    "Host(`" + postfix_url + "`)");
            labels.put(
                    "traefik.http.services." + containerName + ".loadbalancer.server.port",
                    "3000"
            );
            labels.put("traefik.enable", "true");
            labels.put("traefik.docker.network", network);

            System.out.println("labels are "+ labels);


            //create db
            List<ServiceType> services = problem.getServices();
            String safeId = previewId.replace("-", "_");
            if (services.contains(ServiceType.POSTGRES)) {
                //create postgres of exec-id


                String dbName = "db_" + safeId;
                String dbUser = "user_" + safeId;
                String dbPass = "pass_" + safeId;

                String createUser = String.format(
                        "CREATE USER %s WITH PASSWORD '%s';",
                        dbUser, dbPass
                );

                String createDb = String.format(
                        "CREATE DATABASE %s OWNER %s;",
                        dbName, dbUser
                );

                String alterRole = String.format(
                        "ALTER ROLE %s NOSUPERUSER NOCREATEDB NOCREATEROLE;",
                        dbUser
                );
                execPostgres(createUser);
                execPostgres(createDb);
                execPostgres(alterRole);


                envList.add("PG_HOST=problem-postgres");
                envList.add("PG_PORT=5432");
                envList.add("PG_USER=" + dbUser);
                envList.add("PG_PASSWORD=" + dbPass);
                envList.add("PG_DATABASE=" + dbName);
            }
            if (services.contains(ServiceType.MONGODB)) {
                //create mongodb
                String mongoDb = "mongo_" + safeId;
                String mongoUser = "user_" + safeId;
                String mongoPass = "pass_" + safeId;

                createMongoDatabase(safeId);
                waitForMongoReady(mongoDb, mongoUser, mongoPass);

                envList.add(
                        "MONGO_URI=mongodb://" + mongoUser + ":" + mongoPass +
                                "@problem-mongodb:27017/" + mongoDb + "?authSource=admin"
                );
            }
//            if (services.contains(ServiceType.REDIS)) {
//                String redisUser = "user_" + safeId;
//                String redisPass = "pass_" + safeId;
//                String prefix = "preview_" + safeId + ":";
//
//                createRedisUser(safeId);
//
//                envList.add("REDIS_HOST=problem-redis");
//                envList.add("REDIS_PORT=6379");
//                envList.add("REDIS_USERNAME=" + redisUser);
//                envList.add("REDIS_PASSWORD=" + redisPass);
//                envList.add("REDIS_PREFIX=" + prefix);
//            }



            // 🔹 Host config
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withBinds(
                            new Bind(
                                    projectRoot + "/src",
                                    new Volume("/app/src")
                            )
                    )
                    .withMemory(256L * 1024 * 1024)
                    .withMemorySwap(256L * 1024 * 1024)
                    .withCpuPeriod(100_000L)
                    .withCpuQuota(50_000L)
                    .withPidsLimit(64L);

            // 🔹 Create container
            CreateContainerResponse container = dockerClient.createContainerCmd(image)
                    .withName(containerName)
                    .withHostConfig(hostConfig)
                    .withWorkingDir("/app")
                    .withCmd("sh", "-c", buildStartCommand(runner.getImage_name()))
                    .withLabels(labels)
                    .withNetworkMode(network)
                    .withEnv(envList)
                    .exec();

            String containerId = container.getId();

            // 🔹 Start container
            dockerClient.startContainerCmd(containerId).exec();


            // 🔹 Logs
            streamContainerLogs(containerId, previewId);

            //send out traefik url to test out
            logWebSocketHandler.sendEvent(
                    previewId,
                    "URL",
                    protocol + "://" + postfix_url
            );

            //send an env list
            logWebSocketHandler.sendEvent(
                    previewId,
                    "ENV",
                    envList
            );
            String internalUrl = "http://" + containerName + ":3000";

            waitForServer(internalUrl, previewId);

            // 🔹 URL (Traefik)
//
            PreviewURL url = new PreviewURL(
                    postfix_url,
                    containerId,
                    0
            );

            Map<String, Object> data = new HashMap<>();
            data.put("projectId", "preview-" + previewId);
            data.put("url", url);
            data.put("fileId", previewId);
            data.put("fileName", runner.getFile_name());
            data.put("internalUrl", internalUrl);

            return new CustomResponse(data, "Preview started", 200, null);

        } catch (Exception e) {
            e.printStackTrace();
            return new CustomResponse(null, e.getMessage(), 500, null);
        }
    }


    @Override
    public CustomResponse deleteContainer(String projectId, String fileId, String fileName) {
        try {
            log.info("Deleting container for fileId: {}", fileId);
            log.info("Project ID: {}", projectId);
            dockerClient.removeContainerCmd(projectId).withForce(true).exec();

            log.info("Container deleted successfully");
            // 🔹 cleanup DB + Redis + Mongo
            cleanupResources(fileId);


            Path workdir = Paths.get(
                    new File(".").getCanonicalPath(),
                    "workdir",
                    "preview-" + fileId
            );

            Files.walk(workdir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (Exception ignored) {
                        }
                    });
            log.info("Deleted workdir: {}", workdir);

            return new CustomResponse(
                    Map.of("message", "Deleted successfully"),
                    "Success",
                    200,
                    null
            );

        } catch (Exception e) {
            return new CustomResponse(null, e.getMessage(), 500, null);
        }
    }
}
