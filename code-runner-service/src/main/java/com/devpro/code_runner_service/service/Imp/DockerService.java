package com.devpro.code_runner_service.service.Imp;

import com.devpro.code_runner_service.DTO.CustomResponse;
import com.devpro.code_runner_service.DTO.DockerRunner;
import com.devpro.code_runner_service.DTO.FileNode;
import com.devpro.code_runner_service.DTO.PreviewURL;
import com.devpro.code_runner_service.config.socket_configs.LogWebSocketHandler;
import com.devpro.code_runner_service.helper.TestCaseHelper;
import com.devpro.code_runner_service.models.Problem;
import com.devpro.code_runner_service.service.IDockerRepo;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DockerService implements IDockerRepo {

    private final DockerClient dockerClient;
    private final TestCaseHelper helper;
    private final LogWebSocketHandler logWebSocketHandler;

    private static final int TIME_LIMIT_SECONDS = 5; // ⏱️ change per problem

    public DockerService(DockerClient dockerClient, TestCaseHelper helper, LogWebSocketHandler logWebSocketHandler) {
        this.dockerClient = dockerClient;
        this.helper = helper;
        this.logWebSocketHandler = logWebSocketHandler;
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

    private void logFileTree(List<FileNode> files, String indent) {
        if (files == null) {
            System.out.println(indent + "❌ files = null");
            return;
        }

        for (FileNode file : files) {
            if (file.isFolder()) {
                System.out.println(indent + "📁 " + file.getName());
                logFileTree(file.getChildren(), indent + "  ");
            } else {
                System.out.println(
                        indent + "📄 " + file.getName() +
                                " (content length: " +
                                (file.getContent() == null ? 0 : file.getContent().length()) + ")"
                );
            }
        }
    }

    private void streamContainerLogs(String containerId, String submissionId) {
        log.info("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                " Starting log stream for container inside {}", containerId);
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

                                // If there was collected error stack
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


    /**
     * get public url
     * download it and get content
     */
    private String getYMLContent(String publicId) {
        try {

            //get url
            String url = helper.getPublicUrl(publicId);

            //download it
            URI uri = URI.create(url);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(uri.toURL().openStream(), StandardCharsets.UTF_8))) {

                return reader.lines().collect(Collectors.joining("\n"));
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * write a compose file
     */
    private void writeComposeFile(
            Map<String, String> composeFile,
            Path projectRoot
    ) throws IOException {

        for (Map.Entry<String, String> entry : composeFile.entrySet()) {
            String fileName = entry.getKey();      // docker-compose.yml
            String publicId = entry.getValue();    // cloudinary id

            String content = getYMLContent(publicId);

            Path filePath = projectRoot.resolve(fileName);
            Files.writeString(filePath, content, StandardCharsets.UTF_8);
        }
    }

    private String getFirstRunningContainer(File projectDir) throws Exception {

        ProcessBuilder pb = new ProcessBuilder(
                "docker", "compose", "ps", "-q"
        );
        String path = projectDir.getAbsolutePath().replace("\\", "/");
        pb.environment().put("PROJECT_PATH", path);

        pb.directory(projectDir);

        Process process = pb.start();

        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(process.getInputStream()))) {

            String containerId = reader.readLine();

            if (containerId == null || containerId.isBlank()) {
                throw new RuntimeException("No running containers found");
            }

            return containerId.trim();
        }
    }


    // 🔹 docker compose up
    private String runCompose(File projectDir, String previewId) throws Exception {

        //1. run compose file
        ProcessBuilder pb = new ProcessBuilder(
                "docker", "compose", "up", "-d"
        );
        pb.directory(projectDir);
        pb.environment().put("PROJECT_PATH", projectDir.getAbsolutePath());
        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            reader.lines().forEach(System.out::println);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
//            logWebSocketHandler.sendEvent(previewId,"LOG","docker compose up failed");
            throw new RuntimeException("docker compose up failed");
        }

        // 2. fetch container ID for service
        return getFirstRunningContainer(projectDir);

    }

    /**
     * export ports
     */
    private int getComposePort(String containerId) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "docker", "port", containerId, String.valueOf(3000)
        );
        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(process.getInputStream()))) {

            String line = reader.readLine(); // 0.0.0.0:32768
            System.out.println(line);
            if (line == null || !line.contains(":")) {
                throw new RuntimeException("Unable to detect exposed port");
            }
            return Integer.parseInt(line.split(":")[1]);
        }
    }


    private List<String> getRunningContainers(File projectDir) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "docker", "compose", "ps", "-q"
        );
        pb.directory(projectDir);
        pb.environment().put("PROJECT_PATH", projectDir.getAbsolutePath());

        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {

            return reader.lines().toList();
        }
    }

    private Map<String, String> resolveComposeFile(
            DockerRunner runner,
            Problem problem
    ) {

        String framework = runner.getLibOrFramework();
        Map<String, String> composeMap = problem.getComposeFile();

        if (composeMap == null || composeMap.isEmpty()) {
            throw new RuntimeException("No compose files configured");
        }

        if (!composeMap.containsKey(framework)) {
            throw new RuntimeException(
                    "No docker-compose found for framework: " + framework
            );
        }

        // return in writeComposeFile compatible format
        return Map.of(
                "docker-compose.yml",
                composeMap.get(framework)
        );
    }


    /**
     * old one
     */
//    @Override
//    public CustomResponse getPreviewURL(DockerRunner runner, Problem problem) {
//        try {
//            System.out.println("========== PREVIEW REQUEST ==========");
//            System.out.println("Image       : " + runner.getImage_name());
//            System.out.println("Framework   : " + runner.getLibOrFramework());
//            System.out.println("Main file   : " + runner.getFile_name());
//            System.out.println("Files tree  :");
//
//            logFileTree(runner.getFiles(), "  ");
//            System.out.println("=====================================");
//
//            String previewId = UUID.randomUUID().toString();
//            String projectRoot = new File(".").getCanonicalPath()
//                    + "/workdir/preview-" + previewId;
//
//            File projectDir = new File(projectRoot);
//            projectDir.mkdirs();
//
//            Path projectRootPath = projectDir.toPath();
//
//            // Write all files & folders
//            writeFileTree(runner.getFiles(), projectRootPath);
//            // 🔹 Express setup
//            if (runner.getLibOrFramework().equals("express")) {
//                writePackageJson(projectDir, false);
//            }
//
//            // 🔹 TypeScript Express setup
//            if (runner.getLibOrFramework().equals("ts-express")) {
//                writePackageJson(projectDir, true);
//                writeTsConfig(projectDir);
//            }
//
//            // 🔹 Bind project directory with limit
//            HostConfig hostConfig = HostConfig.newHostConfig()
//                    .withBinds(new Bind(projectRoot, new Volume("/app")))
//                    .withMemory(256L * 1024 * 1024)          // 256 MB RAM
//                    .withMemorySwap(256L * 1024 * 1024)     // no swap
//                    .withCpuPeriod(100_000L)
//                    .withCpuQuota(50_000L)                  // 0.5 CPU
//                    .withPidsLimit(64L);                    // fork bomb protection
//
//            System.out.println("host config is ready " + Arrays.toString(hostConfig.getBinds()));
//
//            // 🔹 Pull image if needed
//            try {
//                dockerClient.inspectImageCmd(runner.getImage_name()).exec();
//            } catch (Exception e) {
//                dockerClient.pullImageCmd(runner.getImage_name())
//                        .start().awaitCompletion();
//            }
//
//            ExposedPort exposedPort = switch (runner.getLibOrFramework()) {
//                case "express", "ts-express" -> ExposedPort.tcp(3000);
//                case "fastapi" -> ExposedPort.tcp(8000);
//                default -> throw new RuntimeException("Unsupported framework");
//            };
//
//            CreateContainerResponse container = dockerClient.createContainerCmd(runner.getImage_name())
//                    .withHostConfig(
//                            hostConfig.withPortBindings(
//                                    new PortBinding(Ports.Binding.empty(), exposedPort)
//                            )
//                    )
//                    .withExposedPorts(exposedPort)
//                    .exec();
//
//            String containerId = container.getId();
//            dockerClient.startContainerCmd(containerId).exec();
//            streamContainerLogs(containerId);
//
//            // 🔹 Kill default process
//            exec(containerId,
//                    "pkill -9 node || true; pkill -9 python || true");
//
//            // 🔹 Run user app
//            String cmd = switch (runner.getLibOrFramework()) {
//                case "express" -> "ln -sf /runtime/node_modules /app/node_modules && sleep 1 && node /app/" + runner.getFile_name();
//                case "ts-express" -> "tsx /app/" + runner.getFile_name();
//                case "fastapi" -> "uvicorn " + runner.getFile_name().replace(".py", "")
//                        + ":app --host 0.0.0.0 --port 8000";
//                default -> "";
//            };
//
//            exec(containerId, cmd + " &");
//
//            // 🔹 Get host port
//            InspectContainerResponse inspect =
//                    dockerClient.inspectContainerCmd(containerId).exec();
//
//            Ports.Binding[] bindings =
//                    inspect.getNetworkSettings().getPorts().getBindings().get(exposedPort);
//
//            int hostPort = Integer.parseInt(bindings[0].getHostPortSpec());
//
//            // 🔹 Health check
//            waitForServer(hostPort);
//
//            PreviewURL url = new PreviewURL(
//                    "http://localhost:" + hostPort,
//                    containerId,
//                    hostPort
//            );
//
//            Map<String, Object> data = new HashMap<>();
//            data.put("containerId", containerId);
//            data.put("fileId", previewId);
//            data.put("fileName", runner.getFile_name());
//            data.put("url", url);
//
//            return new CustomResponse(
//                    data,
//                    "Container started successfully",
//                    200,
//                    "200"
//            );
//
//        } catch (Exception e) {
//            System.out.println(e);
//            return new CustomResponse(null, e.getMessage(), 500, null);
//        }
//    }
    @Override
    public CustomResponse getPreviewURL(DockerRunner runner, Problem problem, String previewId) {
        try {
            System.out.println("========== PREVIEW REQUEST ==========");

            String projectRoot = new File(".").getCanonicalPath()
                    + "/workdir/preview-" + previewId;

            // create workdir
            File projectDir = new File(projectRoot);
            projectDir.mkdirs();

            // log user files
            logFileTree(runner.getFiles(), "");

            // write user project
            writeFileTree(runner.getFiles(), projectDir.toPath());

            // write compose
            Map<String, String> composeFile =
                    resolveComposeFile(runner, problem);

            writeComposeFile(composeFile, projectDir.toPath());

            // docker compose up
            String containerId = runCompose(projectDir, previewId);
            System.out.println("RUN COMPOSE DONE, containerId = " + containerId);

            logWebSocketHandler.bindContainer(previewId, containerId);

            // stream logs
            streamContainerLogs(containerId, previewId);


            // detect port
            int hostPort = getComposePort(containerId);

            // health check
            waitForServer(hostPort);

            PreviewURL url = new PreviewURL(
                    "http://localhost:" + hostPort,
                    "compose-" + previewId,
                    hostPort
            );

            Map<String, Object> data = new HashMap<>();
            data.put("previewId", previewId);
            data.put("url", url);
            data.put("containerId", containerId);
            data.put("fileId", previewId);
            data.put("fileName", runner.getFile_name());

            return new CustomResponse(
                    data,
                    "Preview started successfully",
                    200,
                    "200"
            );

        } catch (Exception e) {
            e.printStackTrace();
            return new CustomResponse(null, e.getMessage(), 500, null);
        }
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

    private void waitForServer(int port) throws Exception {
        int retries = 25;
        while (retries-- > 0) {
            try {
                HttpURLConnection con =
                        (HttpURLConnection) new URL("http://localhost:" + port).openConnection();
                con.setConnectTimeout(1000);
                if (con.getResponseCode() < 500) return;
            } catch (Exception ignored) {
            }
            Thread.sleep(1000);
        }
        logWebSocketHandler.sendEvent("compose-preview-1","LOG","Server failed to start");
        throw new RuntimeException("Server failed to start");
    }

    private void writePackageJson(File dir, boolean ts) throws Exception {
        try (FileWriter writer = new FileWriter(new File(dir, "package.json"))) {
            writer.write(ts ? """
                        {
                          "type": "module",
                          "dependencies": { "express": "^4.19.0" },
                          "devDependencies": {
                            "typescript": "^5.0.0",
                            "tsx": "^4.7.0",
                            "@types/node": "^20.0.0",
                            "@types/express": "^4.17.21"
                          }
                        }
                    """ : """
                        {
                          "type": "module",
                          "dependencies": { "express": "^4.19.0" }
                        }
                    """);
        }
    }

    private void writeTsConfig(File dir) throws Exception {
        try (FileWriter writer = new FileWriter(new File(dir, "tsconfig.json"))) {
            writer.write("""
                        {
                          "compilerOptions": {
                            "target": "ES2020",
                            "module": "ESNext",
                            "moduleResolution": "Node",
                            "esModuleInterop": true
                          }
                        }
                    """);
        }
    }

    @Override
    public CustomResponse deleteContainer(String containerId, String fileId, String fileName) {
        try {
            dockerClient.removeContainerCmd(containerId)
                    .withForce(true).exec();

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
