package com.devpro.code_runner_service.service;

import com.devpro.code_runner_service.DTO.CustomResponse;
import com.devpro.code_runner_service.DTO.DockerRunner;
import com.devpro.code_runner_service.DTO.PreviewURL;
import com.devpro.code_runner_service.repository.IDockerRepo;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.*;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class DockerService implements IDockerRepo {
    private final DockerClient dockerClient;

    public DockerService(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    private String getSpringBootPom() {
        return """
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-parent</artifactId>
                        <version>3.2.3</version>
                    </parent>
                    <groupId>com.devpro</groupId>
                    <artifactId>run</artifactId>
                    <version>1.0.0</version>
                
                    <dependencies>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-web</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-actuator</artifactId>
                        </dependency>
                    </dependencies>
                
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.springframework.boot</groupId>
                                <artifactId>spring-boot-maven-plugin</artifactId>
                            </plugin>
                        </plugins>
                    </build>
                
                </project>
                """;
    }


    @Override
    public CustomResponse getPreviewURL(DockerRunner runner) {
        try {
            //get image
            String image = runner.getImage_name();

            //preview id
            String previewId = UUID.randomUUID().toString();

            //dir
            String baseDir = new File(".").getCanonicalPath();
            String projectRoot = baseDir + "/workdir/preview-" + previewId;

            // Create base preview folder
            File projectDir = new File(projectRoot);
            projectDir.mkdirs();

            // springboot setup
            if (runner.getLibOrFramework().equals("springboot")) {

                // Create proper structure
                String basePkg = "com/devpro";
                String javaPath = projectRoot + "/src/main/java/" + basePkg;
                String resourcesPath = projectRoot + "/src/main/resources";
                new File(javaPath).mkdirs();
                new File(resourcesPath).mkdirs();

                // Write user's Java code
                File codeFile = new File(javaPath, runner.getFile_name());
                try (FileWriter writer = new FileWriter(codeFile)) {
                    writer.write(runner.getCode());
                }

                // Auto-generate main class if user didn't write one
//                File mainClass = new File(javaPath, "Application.java");
//                if (!mainClass.exists()) {
//                    try (FileWriter writer = new FileWriter(mainClass)) {
//                        writer.write("""
//                package com.devpro;
//
//                import org.springframework.boot.SpringApplication;
//                import org.springframework.boot.autoconfigure.SpringBootApplication;
//
//                @SpringBootApplication
//                public class Application {
//                    public static void main(String[] args) {
//                        SpringApplication.run(Application.class, args);
//                    }
//                }
//            """);
//                    }
//                }

                // Add Spring Boot config
                File props = new File(resourcesPath, "application.properties");
                try (FileWriter writer = new FileWriter(props)) {
                    writer.write("server.port=8080\n");
                }

                // POM
                File pom = new File(projectRoot, "pom.xml");
                try (FileWriter pw = new FileWriter(pom)) {
                    pw.write(getSpringBootPom());
                }
            } else {
                // Write code directly to preview folder for other languages
                File codeFile = new File(projectDir, runner.getFile_name());
                try (FileWriter writer = new FileWriter(codeFile)) {
                    writer.write(runner.getCode());
                }

                if (runner.getLibOrFramework().equals("express")) {
                    // express project
                    File pkg = new File(projectDir, "package.json");
                    try (FileWriter writer = new FileWriter(pkg)) {
                        writer.write("""
                                {
                                  "type": "module",
                                  "dependencies": {
                                    "express": "^4.19.0"
                                  }
                                }
                                """);
                    }
                }

                if (runner.getLibOrFramework().equals("ts-express")) {
                    File pkg = new File(projectDir, "package.json");
                    try (FileWriter writer = new FileWriter(pkg)) {
                        writer.write("""
                                    {
                                      "type": "module",
                                      "dependencies": {
                                        "express": "^4.19.0"
                                      },
                                      "devDependencies": {
                                        "typescript": "^5.0.0",
                                        "ts-node": "^10.9.2",
                                        "@types/node": "^20.0.0",
                                        "@types/express": "^4.17.21"
                                      }
                                    }
                                """);
                    }

                    // tsconfig.json
                    File tsconfig = new File(projectDir, "tsconfig.json");
                    try (FileWriter writer = new FileWriter(tsconfig)) {
                        writer.write("""
                                    {
                                      "compilerOptions": {
                                        "target": "ES2020",
                                        "module": "ESNext",
                                        "moduleResolution": "Node",
                                        "esModuleInterop": true,
                                        "strict": false,
                                        "skipLibCheck": true
                                      }
                                    }
                                """);
                    }
                }


            }


            // BIND FULL PROJECT ROOT 🔥
            HostConfig hostConfig = new HostConfig().withBinds(
                    new Bind(projectRoot, new Volume("/app")),
                    new Bind(System.getProperty("user.home") + "/.m2", new Volume("/root/.m2"))
            );


            //pull-image
            try {
                dockerClient.inspectImageCmd(image).exec();
                System.out.println("🔹 Using local image: " + image);
            } catch (Exception e) {
                System.out.println("⏬ Pulling from Docker Hub: " + image);
                dockerClient.pullImageCmd(image).start().awaitCompletion();
            }

            //set port and exposed
            ExposedPort exposedPort = switch (image) {
                case "runner-javascript-express", "runner-typescript-express" -> ExposedPort.tcp(3000);
                case "runner-python-fastapi", "runner-python-django" -> ExposedPort.tcp(8000);
                case "runner-python-flask" -> ExposedPort.tcp(5000);
                case "runner-java-springboot" -> ExposedPort.tcp(8080);
                default -> throw new Error("Unsupported runtime");
            };


            //build a container
            CreateContainerResponse containerResponse = dockerClient.createContainerCmd(image)
                    .withHostConfig(
                            new HostConfig().withBinds(hostConfig.getBinds())
                                    .withPortBindings(new PortBinding(Ports.Binding.empty(), exposedPort))
                                    .withAutoRemove(false)
                    )
                    .withExposedPorts(exposedPort)
                    .exec();

            //container id
            String containerId = containerResponse.getId();
            System.out.println(containerId);

            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onNext(Frame frame) {
                            String log = new String(frame.getPayload());

                            // Print to Spring Boot terminal
                            System.out.print(log);
                        }
                    });


            //start container
            dockerClient.startContainerCmd(containerId).exec();

            // Kill base server inside container
            String killExecId = dockerClient.execCreateCmd(containerId)
                    .withCmd("sh", "-c",
                            // Kill node and all known runtimes
                            "pkill -9 node || true; " +
                                    "pkill -9 npm || true; " +
                                    "pkill -9 python || true; " +
                                    "pkill -9 python3 || true; " +
                                    "pkill -9 php || true; " +
                                    "pkill -9 ruby || true; " +
                                    "pkill -9 go || true; " +

                                    // 🔥 KILL ANY PROCESS HOLDING PORT 3000
                                    "for pid in $(lsof -ti :3000); do kill -9 $pid || true; done; " +

                                    // 🔥 KILL ANY PROCESS HOLDING ANY TCP PORT
                                    "for pid in $(netstat -tlnp 2>/dev/null | awk 'NR>2 {print $NF}' | sed 's/.*\\///'); do kill -9 $pid || true; done; "
                    )
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec()
                    .getId();


            dockerClient.execStartCmd(killExecId).start().awaitCompletion();
            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onNext(Frame frame) {
                            String log = new String(frame.getPayload());

                            // Print to Spring Boot terminal
                            System.out.print(log);
                        }
                    });


            // Start user's file
            String cmd = switch (runner.getLibOrFramework()) {
                case "express" ->
                        "ln -sf /runtime/node_modules /app/node_modules && sleep 1 && node /app/" + runner.getFile_name();

                case "ts-express" -> "tsx /app/" + runner.getFile_name();

                case "fastapi", "flask", "django" -> "python " + runner.getFile_name();
                case "springboot" -> "";
                case "laravel" -> "php " + runner.getFile_name();
                case "ruby" -> "ruby " + runner.getFile_name();
                case "rust" -> "cargo run --release " + runner.getFile_name();
                case "go" -> "go run " + runner.getFile_name();
                default -> throw new Error("Unsupported language");
            };

            //if springboot build and run code
            if (runner.getLibOrFramework().equals("springboot")) {

                String build = dockerClient.execCreateCmd(containerId)
                        .withCmd("sh", "-c", "/usr/share/maven/bin/mvn -q -DskipTests package")
                        .withAttachStdout(true)
                        .withAttachStderr(true)
                        .exec().getId();

                dockerClient.execStartCmd(build)
                        .start()
                        .awaitCompletion();


                String run = dockerClient.execCreateCmd(containerId)
                        .withCmd("sh", "-c", "java -jar target/*.jar &")
                        .exec().getId();

                dockerClient.execStartCmd(run).start().awaitCompletion();


            } else {
                String execId = dockerClient.execCreateCmd(containerId)
                        .withCmd("sh", "-c", cmd + " &")
                        .exec()
                        .getId();
                dockerClient.execStartCmd(execId).start().awaitCompletion();
            }

            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onNext(Frame frame) {
                            String log = new String(frame.getPayload());

                            // Print to Spring Boot terminal
                            System.out.print(log);
                        }
                    });

            // Wait server startup
            InspectContainerResponse inspect = dockerClient.inspectContainerCmd(containerId).exec();
            Ports ports = inspect.getNetworkSettings().getPorts();
            Ports.Binding[] bindings = ports.getBindings().get(exposedPort);

            if (bindings == null || bindings.length == 0) {
                throw new RuntimeException("No port mapping found! Server may have crashed.");
            }

            int hostPort = Integer.parseInt(bindings[0].getHostPortSpec());

            // Only health-check for springboot
            if (runner.getLibOrFramework().equals("springboot")) {
                int retries = 30;
                boolean started = false;
                while (retries-- > 0) {
                    try {
                        URL url = new URL("http://localhost:" + hostPort);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setConnectTimeout(2000);
                        connection.setReadTimeout(2000);
                        int code = connection.getResponseCode();
                        if (code == 200) {
                            started = true;
                            break;
                        }
                    } catch (Exception ignored) {
                    }

                    Thread.sleep(1000);
                }

                if (!started) throw new RuntimeException("Spring Boot failed to start!");
            }

            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withFollowStream(false)
                    .exec(new ResultCallback.Adapter<>() {
                        @Override
                        public void onNext(Frame frame) {
                            System.out.println(new String(frame.getPayload()));
                        }
                    });

            PreviewURL url = new PreviewURL("http://localhost:" + hostPort, containerId, hostPort);

            List<Map<String, Object>> DATA = new ArrayList<>();
            DATA.add(Map.of("message", "Container created successfully"));
            DATA.add(Map.of("PreviewURL", url));
            DATA.add(Map.of("fileId", previewId));
            return new CustomResponse(DATA, "Success", 200, null);
        } catch (Exception e) {
            System.out.println(e);
            return new CustomResponse(null, e.getMessage(), 400, null);
        }
    }


    private void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) return;

        Files.walk(path)
                .sorted(Comparator.reverseOrder()) // delete files first
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to delete " + p, e);
                    }
                });
    }


    @Override
    public CustomResponse deleteContainer(String containerId, String fileId, String fileName) {
        try {
            // remove Docker container
            try {
                dockerClient.removeContainerCmd(containerId)
                        .withForce(true)
                        .exec();
            } catch (Exception ignored) {
                // container might already be removed
                return new CustomResponse(null, ignored.getMessage(), 500, null);
            }

            // Build workdir path (preview folder)
            Path workdirPath = Paths.get(
                    new File(".").getCanonicalPath(),
                    "workdir",
                    "preview-" + fileId
            );

            // Delete workdir recursively
            deleteRecursively(workdirPath);

            // Response
            List<Map<String, Object>> DATA = new ArrayList<>();
            DATA.add(Map.of("message", "Container and workdir deleted successfully"));

            return new CustomResponse(DATA, "Success", 200, null);

        } catch (Exception e) {
            return new CustomResponse(null, e.getMessage(), 400, null);
        }
    }

}
