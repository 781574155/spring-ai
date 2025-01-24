package org.springframework.ai.openai.tanqi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DocReferences(@JsonProperty("docs") List<FileDoc> docs, @JsonProperty("files") List<TanqiFile> files) {
}
