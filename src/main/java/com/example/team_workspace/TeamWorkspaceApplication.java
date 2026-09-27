package com.example.team_workspace;

import com.example.team_workspace.config.DotEnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@ConfigurationPropertiesScan
@SpringBootApplication
public class TeamWorkspaceApplication {

    public static void main(String[] args) {
        DotEnvLoader.load();
        SpringApplication.run(TeamWorkspaceApplication.class, args);
    }
}
