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
            System.out.println("Framework   : " + runner.getLibOrFramework());
            System.out.println("Main file   : " + runner.getFile_name());
            System.out.println("Files tree  :");

            logFileTree(runner.getFiles(), "  ");
            System.out.println("=====================================");

            String previewId = UUID.randomUUID().toString();
            String projectRoot = new File(".").getCanonicalPath()
                    + "/workdir/preview-" + previewId;

            File projectDir = new File(projectRoot);
            projectDir.mkdirs();

            Path projectRootPath = projectDir.toPath();

            // Write all files & folders
            writeFileTree(runner.getFiles(), projectRootPath);
            // 🔹 Express setup
            if (runner.getLibOrFramework().equals("express")) {
                writePackageJson(projectDir, false);
            }

            // 🔹 TypeScript Express setup
            if (runner.getLibOrFramework().equals("ts-express")) {
                writePackageJson(projectDir, true);
                writeTsConfig(projectDir);
            }

            // 🔹 Bind project directory with limit
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withBinds(new Bind(projectRoot, new Volume("/app")))
                    .withMemory(256L * 1024 * 1024)          // 256 MB RAM
                    .withMemorySwap(256L * 1024 * 1024)     // no swap
                    .withCpuPeriod(100_000L)
                    .withCpuQuota(50_000L)                  // 0.5 CPU
                    .withPidsLimit(64L);                    // fork bomb protection

            System.out.println("host config is ready " + Arrays.toString(hostConfig.getBinds()));

            // 🔹 Pull image if needed
            try {
                dockerClient.inspectImageCmd(runner.getImage_name()).exec();
            } catch (Exception e) {
                dockerClient.pullImageCmd(runner.getImage_name())
                        .start().awaitCompletion();
            }

            ExposedPort exposedPort = switch (runner.getLibOrFramework()) {
                case "express", "ts-express" -> ExposedPort.tcp(3000);
                case "fastapi" -> ExposedPort.tcp(8000);
                default -> throw new RuntimeException("Unsupported framework");
            };

            CreateContainerResponse container = dockerClient.createContainerCmd(runner.getImage_name())
                    .withHostConfig(
                            hostConfig.withPortBindings(
                                    new PortBinding(Ports.Binding.empty(), exposedPort)
                            )
                    )
                    .withExposedPorts(exposedPort)
                    .exec();

            String containerId = container.getId();
            dockerClient.startContainerCmd(containerId).exec();
            streamContainerLogs(containerId);

            // 🔹 Kill default process
            exec(containerId,
                    "pkill -9 node || true; pkill -9 python || true");

            // 🔹 Run user app
            String cmd = switch (runner.getLibOrFramework()) {
                case "express" -> "ln -sf /runtime/node_modules /app/node_modules && sleep 1 && node /app/" + runner.getFile_name();
                case "ts-express" -> "tsx /app/" + runner.getFile_name();
                case "fastapi" -> "uvicorn " + runner.getFile_name().replace(".py", "")
                        + ":app --host 0.0.0.0 --port 8000";
                default -> "";
            };

            exec(containerId, cmd + " &");

            // 🔹 Get host port
            InspectContainerResponse inspect =
                    dockerClient.inspectContainerCmd(containerId).exec();

            Ports.Binding[] bindings =
                    inspect.getNetworkSettings().getPorts().getBindings().get(exposedPort);

            int hostPort = Integer.parseInt(bindings[0].getHostPortSpec());

            // 🔹 Health check
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

            return new CustomResponse(
                    data,
                    "Container started successfully",
                    200,
                    "200"
            );

        } catch (Exception e) {
            System.out.println(e);
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
