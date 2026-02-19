package com.devpro.code_runner_service.config.redis_configs;

import com.devpro.code_runner_service.DTO.ContainerDTO;
import com.devpro.code_runner_service.service.Imp.DockerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisExpirationListener implements MessageListener {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final DockerService dockerService;

    @Override
    public void onMessage(Message message, byte[] pattern) {

        //get expireKey
        String expiredKey = message.toString();

        // We only care about ttl keys - bcz only ttl  key expires
        if (!expiredKey.startsWith("container:ttl:")) {
            return;
        }

        try {

            //convert ttl to data
            String dataKey = expiredKey.replace("ttl:", "data:");

            //get container metadata
            String json = redisTemplate.opsForValue().get(dataKey);

            if (json == null) {
                System.out.println("No container metadata found.");
                return;
            }

            //get dto data
            ContainerDTO containerDTO =
                    objectMapper.readValue(json, ContainerDTO.class);

            //delete container
            dockerService.deleteContainer(
                    containerDTO.getContainerId(),
                    containerDTO.getFileId(),
                    containerDTO.getFileName()
            );

            // Cleanup redis
            redisTemplate.delete(dataKey);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
