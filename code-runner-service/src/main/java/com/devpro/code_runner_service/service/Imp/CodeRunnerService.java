package com.devpro.code_runner_service.service.Imp;

import com.devpro.code_runner_service.DTO.CustomResponse;
import com.devpro.code_runner_service.DTO.DockerRunner;
import com.devpro.code_runner_service.DTO.PreviewURL;
import com.devpro.code_runner_service.helper.TestCaseHelper;
import com.devpro.code_runner_service.service.ICodeRunner;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CodeRunnerService implements ICodeRunner {

    private final DockerService dockerService;
    private final TestCaseHelper helper;

    public CodeRunnerService(DockerService dockerService, TestCaseHelper helper) {
        this.dockerService = dockerService;
        this.helper = helper;
    }

    @Override
    public CustomResponse runCode(String uuid, DockerRunner dockerRunner) {

        String cid = null;
        String fileId = null;
        String fileName = null;

        try {
            // 1️⃣ Start container
            CustomResponse response = dockerService.getPreviewURL(dockerRunner);

            if (response == null || response.getData() == null) {
                return new CustomResponse(null, "Docker failed to start", 500, null);
            }

            Map<String, Object> data = response.getData();

            cid = data.get("containerId").toString();
            fileId = data.get("fileId").toString();
            fileName = data.get("fileName").toString();
            PreviewURL url = (PreviewURL) data.get("url");

            // 2️⃣ Run sample testcases
            return helper.codeRun(uuid, url);

        } finally {
            // 3️⃣ ALWAYS cleanup
            if (cid != null && fileId != null) {
                dockerService.deleteContainer(cid, fileId, fileName);
            }
        }
    }

    @Override
    public CustomResponse submitCode(String uuid, DockerRunner dockerRunner) {

        String cid = null;
        String fileId = null;
        String fileName = null;

        try {
            // 1️⃣ Start container
            CustomResponse response = dockerService.getPreviewURL(dockerRunner);

            if (response == null || response.getData() == null) {
                return new CustomResponse(null, "Docker failed to start", 500, null);
            }

            Map<String, Object> data = response.getData();

            cid = data.get("containerId").toString();
            fileId = data.get("fileId").toString();
            fileName = data.get("fileName").toString();
            PreviewURL url = (PreviewURL) data.get("url");

            // 2️⃣ Run sample + hidden testcases
            return helper.codeSubmit(uuid, url);

        } finally {
            // 3️⃣ ALWAYS cleanup
            if (cid != null && fileId != null) {
                dockerService.deleteContainer(cid, fileId, fileName);
            }
        }
    }
}
