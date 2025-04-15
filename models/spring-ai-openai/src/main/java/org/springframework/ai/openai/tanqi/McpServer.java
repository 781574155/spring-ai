package org.springframework.ai.openai.tanqi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpServer(@JsonProperty("name") String name, @JsonProperty("url") String url,
		@JsonProperty("tools") List<McpTool> tools) {
}
