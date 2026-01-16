package com.devpro.code_runner_service.service.Imp;

import com.devpro.code_runner_service.DTO.CustomResponse;
import com.devpro.code_runner_service.DTO.DockerRunner;
import com.devpro.code_runner_service.DTO.FileNode;
import com.devpro.code_runner_service.DTO.PreviewURL;
import com.devpro.code_runner_service.service.IDockerRepo;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.*;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class DockerService implements IDockerRepo {

    private final DockerClient dockerClient;

    private static final int TIME_LIMIT_SECONDS = 5; // ⏱️ change per problem

    public DockerService(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
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
            }
            else {
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
    private void streamContainerLogs(String containerId) {
        new Thread(() -> {
            try {
                dockerClient.logContainerCmd(containerId)
                        .withStdOut(true)
                        .withStdErr(true)
                        .withFollowStream(true)
                        .withTailAll()
                        .exec(new ResultCallback.Adapter<Frame>() {
                            @Override
                            public void onNext(Frame frame) {
                                System.out.print(
                                        "[CONTAINER " + containerId.substring(0, 6) + "] "
                                                + new String(frame.getPayload())
                                );
                            }

                            @Override
                            public void onComplete() {
                                System.out.println("[CONTAINER LOG STREAM CLOSED]");
                            }
                        });
            } catch (Exception e) {
                System.err.println("Log stream error: " + e.getMessage());
            }
        }).start();
    }


    @Override
    public CustomResponse getPreviewURL(DockerRunner runner) {
        try {
            System.out.println("========== PREVIEW REQUEST ==========");
            System.out.println("Image       : " + runner.getImage_name());
            System.out.println("Main file   : " + runner.getFile_name());
            System.out.println("Files tree  :");

            // ----------------- Determine framework and language -----------------
            String framework;
            String language;
            // ----------------- Map legacy/invalid images to allowed images -----------------
            switch (runner.getImage_name()) {
                case "express-js-core" -> runner.setImage_name("runner-javascript-express");
                case "ts-express-core" -> runner.setImage_name("runner-typescript-express");
                case "fastapi-core" -> runner.setImage_name("runner-python-fastapi");
                // else keep it as is (assume already valid)
            }

            switch (runner.getImage_name()) {
                case "runner-typescript-express" -> { framework = "express"; language = "typescript"; }
                case "runner-javascript-express" -> { framework = "express"; language = "javascript"; }
                case "runner-python-fastapi" -> { framework = "fastapi"; language = "python"; }
                default -> throw new RuntimeException("Unsupported image: " + runner.getImage_name());
            }

            // ----------------- Preprocess files: ignore DB connections -----------------
            boolean dbDetected = preprocessFiles(runner.getFiles());
            runner.setDbConnectionDetected(dbDetected);

            logFileTree(runner.getFiles(), "  ");
            System.out.println("DB connection detected: " + dbDetected);
            System.out.println("Framework: " + framework + ", Language: " + language);
            System.out.println("=====================================");

            String previewId = UUID.randomUUID().toString();
            String projectRoot = new File(".").getCanonicalPath()
                    + "/workdir/preview-" + previewId;

            File projectDir = new File(projectRoot);
            projectDir.mkdirs();
            Path projectRootPath = projectDir.toPath();

            // ----------------- Fetch files from Supabase if DB connection detected -----------------
            if (dbDetected) {
                System.out.println("Fetching input files from Supabase due to DB connection...");
                fetchFilesFromSupabase(projectRootPath, runner.getFiles());
            } else {
                writeFileTree(runner.getFiles(), projectRootPath);
            }

            // ----------------- Framework-specific setup -----------------
            if ("express".equals(framework) && "typescript".equals(language)) {
                writePackageJson(projectDir, true);
                writeTsConfig(projectDir);
            } else if ("express".equals(framework)) {
                writePackageJson(projectDir, false);
            }

            // ----------------- Docker host config -----------------
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withBinds(new Bind(projectRoot, new Volume("/app")))
                    .withMemory(256L * 1024 * 1024)
                    .withMemorySwap(256L * 1024 * 1024)
                    .withCpuPeriod(100_000L)
                    .withCpuQuota(50_000L)
                    .withPidsLimit(64L);

            // ----------------- Pull/check image -----------------
            try {
                dockerClient.inspectImageCmd(runner.getImage_name()).exec();
                System.out.println("Image found locally: " + runner.getImage_name());
            } catch (com.github.dockerjava.api.exception.NotFoundException e) {
                System.out.println("Image not found locally, trying to pull: " + runner.getImage_name());
                try {
                    dockerClient.pullImageCmd(runner.getImage_name())
                            .start()
                            .awaitCompletion();
                    System.out.println("Image pulled successfully: " + runner.getImage_name());
                } catch (Exception pullEx) {
                    return new CustomResponse(
                            null,
                            "Docker image not found or access denied: " + runner.getImage_name(),
                            404,
                            null
                    );
                }
            }

            // ----------------- Exposed port -----------------
            ExposedPort exposedPort = "express".equals(framework) ? ExposedPort.tcp(3000) : ExposedPort.tcp(8000);

            // ----------------- Create container -----------------
            CreateContainerResponse container = dockerClient.createContainerCmd(runner.getImage_name())
                    .withHostConfig(hostConfig.withPortBindings(new PortBinding(Ports.Binding.empty(), exposedPort)))
                    .withExposedPorts(exposedPort)
                    .exec();

            String containerId = container.getId();
            dockerClient.startContainerCmd(containerId).exec();
            streamContainerLogs(containerId);

            // Kill default processes
            exec(containerId, "pkill -9 node || true; pkill -9 python || true");

            // ----------------- Run user app -----------------
            String cmd;
            if ("express".equals(framework)) {
                cmd = "ln -sf /runtime/node_modules /app/node_modules && sleep 1 && node /app/" + runner.getFile_name();
            } else { // fastapi
                cmd = "uvicorn " + runner.getFile_name().replace(".py", "") + ":app --host 0.0.0.0 --port 8000";
            }
            exec(containerId, cmd + " &");

            // ----------------- Get host port -----------------
            InspectContainerResponse inspect = dockerClient.inspectContainerCmd(containerId).exec();
            Ports.Binding[] bindings = inspect.getNetworkSettings().getPorts().getBindings().get(exposedPort);
            int hostPort = Integer.parseInt(bindings[0].getHostPortSpec());

            // ----------------- Health check -----------------
            waitForServer(hostPort);

            PreviewURL url = new PreviewURL(
                    "http://localhost:" + hostPort,
                    containerId,
                    hostPort
            );

            Map<String, Object> data = new HashMap<>();
            data.put("containerId", containerId);
            data.put("fileId", previewId);
            data.put("fileName", runner.getFile_name());
            data.put("url", url);

            return new CustomResponse(data, "Container started successfully", 200, "200");

        } catch (Exception e) {
            System.out.println(e);
            return new CustomResponse(null, e.getMessage(), 500, null);
        }
    }

    // 🔹 Preprocess files: remove DB connections
    private boolean preprocessFiles(List<FileNode> files) {
        if (files == null) return false;

        // Use AtomicBoolean to allow mutation inside recursion/lambda
        AtomicBoolean dbDetected = new AtomicBoolean(false);

        for (FileNode f : files) {
            if (f.getContent() != null) {
                String original = f.getContent();
                String cleaned = original.replaceAll(
                        "(?i).*\\b(mongoose\\.connect|mysql\\.createConnection|pg\\.connect)\\b.*",
                        "// DB connection ignored"
                );
                if (!original.equals(cleaned)) dbDetected.set(true);
                f.setContent(cleaned);
            }
            // Recursive call for children
            if (f.getChildren() != null) {
                if (preprocessFiles(f.getChildren())) {
                    dbDetected.set(true);
                }
            }
        }

        // Set the flag in each FileNode
        files.forEach(f -> f.setDbConnectionDetected(dbDetected.get()));

        return dbDetected.get();
    }

    // 🔹 Fetch files from Supabase (pseudo code)
    private void fetchFilesFromSupabase(Path projectRoot, List<FileNode> files) throws Exception {
        // Implement your Supabase S3 fetch logic here
        // For example, using supabase-java client:
        // SupabaseClient storageClient = ...
        // for each file, download to projectRoot
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
            // ----------------- Stop the container first (if running) -----------------
            try {
                dockerClient.stopContainerCmd(containerId)
                        .withTimeout(5) // seconds
                        .exec();
            } catch (com.github.dockerjava.api.exception.NotFoundException ignored) {
                // Container already removed
            } catch (Exception e) {
                System.err.println("Error stopping container: " + e.getMessage());
            }

            // ----------------- Remove container -----------------
            try {
                dockerClient.removeContainerCmd(containerId)
                        .withForce(true)
                        .exec();
            } catch (com.github.dockerjava.api.exception.NotFoundException ignored) {
                // Already deleted
            } catch (Exception e) {
                return new CustomResponse(null, "Failed to remove container: " + e.getMessage(), 500, null);
            }

            // ----------------- Delete workdir files -----------------
            Path workdir = Paths.get(
                    new File(".").getCanonicalPath(),
                    "workdir",
                    "preview-" + fileId
            );

            if (Files.exists(workdir)) {
                Files.walk(workdir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (Exception ignored) {
                            }
                        });
            }

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
