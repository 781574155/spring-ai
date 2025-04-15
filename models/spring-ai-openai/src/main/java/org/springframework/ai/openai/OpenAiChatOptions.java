/*
 * Copyright 2023-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ai.openai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest.AudioParameters;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest.StreamOptions;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest.ToolChoiceBuilder;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.ai.openai.tanqi.McpServer;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.lang.Nullable;
import org.springframework.ai.openai.tanqi.TanqiFile;
import org.springframework.util.Assert;

/**
 * Options for the OpenAI Chat API.
 *
 * @author Christian Tzolov
 * @author Mariusz Bernacki
 * @author Thomas Vitale
 * @author Ilayaperumal Gopinathan
 * @since 0.8.0
 */
@JsonInclude(Include.NON_NULL)
public class OpenAiChatOptions implements ToolCallingChatOptions {

	// @formatter:off
	/**
	 * ID of the model to use.
	 */
	private @JsonProperty("model") String model;
	/**
	 * Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing
	 * frequency in the text so far, decreasing the model's likelihood to repeat the same line verbatim.
	 */
	private @JsonProperty("frequency_penalty") Double frequencyPenalty;
	/**
	 * Modify the likelihood of specified tokens appearing in the completion. Accepts a JSON object
	 * that maps tokens (specified by their token ID in the tokenizer) to an associated bias value from -100 to 100.
	 * Mathematically, the bias is added to the logits generated by the model prior to sampling. The exact effect will
	 * vary per model, but values between -1 and 1 should decrease or increase likelihood of selection; values like -100
	 * or 100 should result in a ban or exclusive selection of the relevant token.
	 */
	private @JsonProperty("logit_bias") Map<String, Integer> logitBias;
	/**
	 * Whether to return log probabilities of the output tokens or not. If true, returns the log probabilities
	 * of each output token returned in the 'content' of 'message'.
	 */
	private @JsonProperty("logprobs") Boolean logprobs;
	/**
	 * An integer between 0 and 5 specifying the number of most likely tokens to return at each token position,
	 * each with an associated log probability. 'logprobs' must be set to 'true' if this parameter is used.
	 */
	private @JsonProperty("top_logprobs") Integer topLogprobs;
	/**
	 * The maximum number of tokens to generate in the chat completion. The total length of input
	 * tokens and generated tokens is limited by the model's context length.
	 */
	private @JsonProperty("max_tokens") Integer maxTokens;
	/**
	 * An upper bound for the number of tokens that can be generated for a completion,
	 * including visible output tokens and reasoning tokens.
	 */
	private @JsonProperty("max_completion_tokens") Integer maxCompletionTokens;
	/**
	 * How many chat completion choices to generate for each input message. Note that you will be charged based
	 * on the number of generated tokens across all of the choices. Keep n as 1 to minimize costs.
	 */
	private @JsonProperty("n") Integer n;

	/**
	 * Output types that you would like the model to generate for this request.
	 * Most models are capable of generating text, which is the default.
	 * The gpt-4o-audio-preview model can also be used to generate audio.
	 * To request that this model generate both text and audio responses,
	 * you can use: ["text", "audio"].
	 * Note that the audio modality is only available for the gpt-4o-audio-preview model
	 * and is not supported for streaming completions.
	 */
	private @JsonProperty("modalities") List<String> outputModalities;

	/**
	 * Audio parameters for the audio generation. Required when audio output is requested with
	 * modalities: ["audio"]
	 * Note: that the audio modality is only available for the gpt-4o-audio-preview model
	 * and is not supported for streaming completions.

	 */
	private @JsonProperty("audio") AudioParameters outputAudio;

	/**
	 * Number between -2.0 and 2.0. Positive values penalize new tokens based on whether they
	 * appear in the text so far, increasing the model's likelihood to talk about new topics.
	 */
	private @JsonProperty("presence_penalty") Double presencePenalty;
	/**
	 * An object specifying the format that the model must output. Setting to { "type":
	 * "json_object" } enables JSON mode, which guarantees the message the model generates is valid JSON.
	 */
	private @JsonProperty("response_format") ResponseFormat responseFormat;
	/**
	 * Options for streaming response. Included in the API only if streaming-mode completion is requested.
	 */
	private @JsonProperty("stream_options") StreamOptions streamOptions;
	/**
	 * This feature is in Beta. If specified, our system will make a best effort to sample
	 * deterministically, such that repeated requests with the same seed and parameters should return the same result.
	 * Determinism is not guaranteed, and you should refer to the system_fingerprint response parameter to monitor
	 * changes in the backend.
	 */
	private @JsonProperty("seed") Integer seed;
	/**
	 * Up to 4 sequences where the API will stop generating further tokens.
	 */
	private @JsonProperty("stop") List<String> stop;
	/**
	 * What sampling temperature to use, between 0 and 1. Higher values like 0.8 will make the output
	 * more random, while lower values like 0.2 will make it more focused and deterministic. We generally recommend
	 * altering this or top_p but not both.
	 */
	private @JsonProperty("temperature") Double temperature;
	/**
	 * An alternative to sampling with temperature, called nucleus sampling, where the model considers the
	 * results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10%
	 * probability mass are considered. We generally recommend altering this or temperature but not both.
	 */
	private @JsonProperty("top_p") Double topP;
	/**
	 * A list of tools the model may call. Currently, only functions are supported as a tool. Use this to
	 * provide a list of functions the model may generate JSON inputs for.
	 */
	private @JsonProperty("tools") List<OpenAiApi.FunctionTool> tools;
	/**
	 * Controls which (if any) function is called by the model. none means the model will not call a
	 * function and instead generates a message. auto means the model can pick between generating a message or calling a
	 * function. Specifying a particular function via {"type: "function", "function": {"name": "my_function"}} forces
	 * the model to call that function. none is the default when no functions are present. auto is the default if
	 * functions are present. Use the {@link ToolChoiceBuilder} to create a tool choice object.
	 */
	private @JsonProperty("tool_choice") Object toolChoice;
	/**
	 * A unique identifier representing your end-user, which can help OpenAI to monitor and detect abuse.
	 */
	private @JsonProperty("user") String user;
	/**
	 * Whether to enable <a href="https://platform.openai.com/docs/guides/function-calling/parallel-function-calling">parallel function calling</a> during tool use.
	 * Defaults to true.
	 */
	private @JsonProperty("parallel_tool_calls") Boolean parallelToolCalls;
	/**
	 * Whether to store the output of this chat completion request for use in our model <a href="https://platform.openai.com/docs/guides/distillation">distillation</a> or <a href="https://platform.openai.com/docs/guides/evals">evals</a> products.
	 */
	private @JsonProperty("store") Boolean store;
	/**
	 * Developer-defined tags and values used for filtering completions in the <a href="https://platform.openai.com/chat-completions">dashboard</a>.
	 */
	private @JsonProperty("metadata") Map<String, String> metadata;

	/**
	 * Constrains effort on reasoning for reasoning models. Currently supported values are low, medium, and high.
	 * Reducing reasoning effort can result in faster responses and fewer tokens used on reasoning in a response.
	 * Optional. Defaults to medium.
	 * Only for 'o1' models.
	 */
	private @JsonProperty("reasoning_effort") String reasoningEffort;

	/**
	 * Collection of {@link ToolCallback}s to be used for tool calling in the chat completion requests.
	 */
	@JsonIgnore
	private List<FunctionCallback> toolCallbacks = new ArrayList<>();

	/**
	 * Collection of tool names to be resolved at runtime and used for tool calling in the chat completion requests.
	 */
	@JsonIgnore
	private Set<String> toolNames = new HashSet<>();

	/**
	 * Whether to enable the tool execution lifecycle internally in ChatModel.
	 */
	@JsonIgnore
	private Boolean internalToolExecutionEnabled;

	/**
	 * Optional HTTP headers to be added to the chat completion request.
	 */
	@JsonIgnore
	private Map<String, String> httpHeaders = new HashMap<>();

	@JsonIgnore
	private Map<String, Object> toolContext = new HashMap<>();

	// add for tanqi
	private @JsonProperty("category") String category;
	private @JsonProperty("tanqi_files") List<TanqiFile> tanqiFiles;
	private @JsonProperty("conversation_id") String conversationId;
	private @JsonProperty("req_type") String reqType;
	private @JsonProperty("mcp_servers") List<McpServer> mcpServers;

	// @formatter:on

	public static Builder builder() {
		return new Builder();
	}

	public static OpenAiChatOptions fromOptions(OpenAiChatOptions fromOptions) {
		return OpenAiChatOptions.builder()
			.model(fromOptions.getModel())
			.frequencyPenalty(fromOptions.getFrequencyPenalty())
			.logitBias(fromOptions.getLogitBias())
			.logprobs(fromOptions.getLogprobs())
			.topLogprobs(fromOptions.getTopLogprobs())
			.maxTokens(fromOptions.getMaxTokens())
			.maxCompletionTokens(fromOptions.getMaxCompletionTokens())
			.N(fromOptions.getN())
			.outputModalities(fromOptions.getOutputModalities() != null
					? new ArrayList<>(fromOptions.getOutputModalities()) : null)
			.outputAudio(fromOptions.getOutputAudio())
			.presencePenalty(fromOptions.getPresencePenalty())
			.responseFormat(fromOptions.getResponseFormat())
			.streamUsage(fromOptions.getStreamUsage())
			.seed(fromOptions.getSeed())
			.stop(fromOptions.getStop() != null ? new ArrayList<>(fromOptions.getStop()) : null)
			.temperature(fromOptions.getTemperature())
			.topP(fromOptions.getTopP())
			.tools(fromOptions.getTools())
			.toolChoice(fromOptions.getToolChoice())
			.user(fromOptions.getUser())
			.parallelToolCalls(fromOptions.getParallelToolCalls())
			.toolCallbacks(
					fromOptions.getToolCallbacks() != null ? new ArrayList<>(fromOptions.getToolCallbacks()) : null)
			.toolNames(fromOptions.getToolNames() != null ? new HashSet<>(fromOptions.getToolNames()) : null)
			.httpHeaders(fromOptions.getHttpHeaders() != null ? new HashMap<>(fromOptions.getHttpHeaders()) : null)
			.internalToolExecutionEnabled(fromOptions.isInternalToolExecutionEnabled())
			.toolContext(fromOptions.getToolContext() != null ? new HashMap<>(fromOptions.getToolContext()) : null)
			.store(fromOptions.getStore())
			.metadata(fromOptions.getMetadata())
			.reasoningEffort(fromOptions.getReasoningEffort())
			.build();
	}

	public Boolean getStreamUsage() {
		return this.streamOptions != null;
	}

	public void setStreamUsage(Boolean enableStreamUsage) {
		this.streamOptions = (enableStreamUsage) ? StreamOptions.INCLUDE_USAGE : null;
	}

	@Override
	public String getModel() {
		return this.model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	@Override
	public Double getFrequencyPenalty() {
		return this.frequencyPenalty;
	}

	public void setFrequencyPenalty(Double frequencyPenalty) {
		this.frequencyPenalty = frequencyPenalty;
	}

	public Map<String, Integer> getLogitBias() {
		return this.logitBias;
	}

	public void setLogitBias(Map<String, Integer> logitBias) {
		this.logitBias = logitBias;
	}

	public Boolean getLogprobs() {
		return this.logprobs;
	}

	public void setLogprobs(Boolean logprobs) {
		this.logprobs = logprobs;
	}

	public Integer getTopLogprobs() {
		return this.topLogprobs;
	}

	public void setTopLogprobs(Integer topLogprobs) {
		this.topLogprobs = topLogprobs;
	}

	@Override
	public Integer getMaxTokens() {
		return this.maxTokens;
	}

	public void setMaxTokens(Integer maxTokens) {
		this.maxTokens = maxTokens;
	}

	public Integer getMaxCompletionTokens() {
		return this.maxCompletionTokens;
	}

	public void setMaxCompletionTokens(Integer maxCompletionTokens) {
		this.maxCompletionTokens = maxCompletionTokens;
	}

	public Integer getN() {
		return this.n;
	}

	public void setN(Integer n) {
		this.n = n;
	}

	public List<String> getOutputModalities() {
		return this.outputModalities;
	}

	public void setOutputModalities(List<String> modalities) {
		this.outputModalities = modalities;
	}

	public AudioParameters getOutputAudio() {
		return this.outputAudio;
	}

	public void setOutputAudio(AudioParameters audio) {
		this.outputAudio = audio;
	}

	@Override
	public Double getPresencePenalty() {
		return this.presencePenalty;
	}

	public void setPresencePenalty(Double presencePenalty) {
		this.presencePenalty = presencePenalty;
	}

	public ResponseFormat getResponseFormat() {
		return this.responseFormat;
	}

	public void setResponseFormat(ResponseFormat responseFormat) {
		this.responseFormat = responseFormat;
	}

	public StreamOptions getStreamOptions() {
		return this.streamOptions;
	}

	public void setStreamOptions(StreamOptions streamOptions) {
		this.streamOptions = streamOptions;
	}

	public Integer getSeed() {
		return this.seed;
	}

	public void setSeed(Integer seed) {
		this.seed = seed;
	}

	@Override
	@JsonIgnore
	public List<String> getStopSequences() {
		return getStop();
	}

	@JsonIgnore
	public void setStopSequences(List<String> stopSequences) {
		setStop(stopSequences);
	}

	public List<String> getStop() {
		return this.stop;
	}

	public void setStop(List<String> stop) {
		this.stop = stop;
	}

	@Override
	public Double getTemperature() {
		return this.temperature;
	}

	public void setTemperature(Double temperature) {
		this.temperature = temperature;
	}

	@Override
	public Double getTopP() {
		return this.topP;
	}

	public void setTopP(Double topP) {
		this.topP = topP;
	}

	public List<OpenAiApi.FunctionTool> getTools() {
		return this.tools;
	}

	public void setTools(List<OpenAiApi.FunctionTool> tools) {
		this.tools = tools;
	}

	public Object getToolChoice() {
		return this.toolChoice;
	}

	public void setToolChoice(Object toolChoice) {
		this.toolChoice = toolChoice;
	}

	@Override
	@Deprecated
	@JsonIgnore
	public Boolean getProxyToolCalls() {
		return this.internalToolExecutionEnabled != null ? !this.internalToolExecutionEnabled : null;
	}

	@Deprecated
	@JsonIgnore
	public void setProxyToolCalls(Boolean proxyToolCalls) {
		this.internalToolExecutionEnabled = proxyToolCalls != null ? !proxyToolCalls : null;
	}

	public String getUser() {
		return this.user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public Boolean getParallelToolCalls() {
		return this.parallelToolCalls;
	}

	public void setParallelToolCalls(Boolean parallelToolCalls) {
		this.parallelToolCalls = parallelToolCalls;
	}

	@Override
	@JsonIgnore
	public List<FunctionCallback> getToolCallbacks() {
		return this.toolCallbacks;
	}

	@Override
	@JsonIgnore
	public void setToolCallbacks(List<FunctionCallback> toolCallbacks) {
		Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
		Assert.noNullElements(toolCallbacks, "toolCallbacks cannot contain null elements");
		this.toolCallbacks = toolCallbacks;
	}

	@Override
	@JsonIgnore
	public Set<String> getToolNames() {
		return this.toolNames;
	}

	@Override
	@JsonIgnore
	public void setToolNames(Set<String> toolNames) {
		Assert.notNull(toolNames, "toolNames cannot be null");
		Assert.noNullElements(toolNames, "toolNames cannot contain null elements");
		toolNames.forEach(tool -> Assert.hasText(tool, "toolNames cannot contain empty elements"));
		this.toolNames = toolNames;
	}

	@Override
	@Nullable
	@JsonIgnore
	public Boolean isInternalToolExecutionEnabled() {
		return internalToolExecutionEnabled;
	}

	@Override
	@JsonIgnore
	public void setInternalToolExecutionEnabled(@Nullable Boolean internalToolExecutionEnabled) {
		this.internalToolExecutionEnabled = internalToolExecutionEnabled;
	}

	@Override
	@Deprecated
	@JsonIgnore
	public List<FunctionCallback> getFunctionCallbacks() {
		return this.getToolCallbacks();
	}

	@Override
	@Deprecated
	@JsonIgnore
	public void setFunctionCallbacks(List<FunctionCallback> functionCallbacks) {
		this.setToolCallbacks(functionCallbacks);
	}

	@Override
	@Deprecated
	@JsonIgnore
	public Set<String> getFunctions() {
		return this.getToolNames();
	}

	@Override
	@Deprecated
	@JsonIgnore
	public void setFunctions(Set<String> functionNames) {
		this.setToolNames(functionNames);
	}

	public Map<String, String> getHttpHeaders() {
		return this.httpHeaders;
	}

	public void setHttpHeaders(Map<String, String> httpHeaders) {
		this.httpHeaders = httpHeaders;
	}

	@Override
	@JsonIgnore
	public Integer getTopK() {
		return null;
	}

	@Override
	@JsonIgnore
	public Map<String, Object> getToolContext() {
		return this.toolContext;
	}

	@Override
	@JsonIgnore
	public void setToolContext(Map<String, Object> toolContext) {
		this.toolContext = toolContext;
	}

	public Boolean getStore() {
		return this.store;
	}

	public void setStore(Boolean store) {
		this.store = store;
	}

	public Map<String, String> getMetadata() {
		return this.metadata;
	}

	public void setMetadata(Map<String, String> metadata) {
		this.metadata = metadata;
	}

	public String getReasoningEffort() {
		return this.reasoningEffort;
	}

	public void setReasoningEffort(String reasoningEffort) {
		this.reasoningEffort = reasoningEffort;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public List<TanqiFile> getTanqiFiles() {
		return tanqiFiles;
	}

	public void setTanqiFiles(List<TanqiFile> tanqiFiles) {
		this.tanqiFiles = tanqiFiles;
	}

	public String getConversationId() {
		return conversationId;
	}

	public void setConversationId(String conversationId) {
		this.conversationId = conversationId;
	}

	public String getReqType() {
		return reqType;
	}

	public void setReqType(String reqType) {
		this.reqType = reqType;
	}

	public List<McpServer> getMcpServers() {
		return mcpServers;
	}

	public void setMcpServers(List<McpServer> mcpServers) {
		this.mcpServers = mcpServers;
	}

	@Override
	public OpenAiChatOptions copy() {
		return OpenAiChatOptions.fromOptions(this);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.model, this.frequencyPenalty, this.logitBias, this.logprobs, this.topLogprobs,
				this.maxTokens, this.maxCompletionTokens, this.n, this.presencePenalty, this.responseFormat,
				this.streamOptions, this.seed, this.stop, this.temperature, this.topP, this.tools, this.toolChoice,
				this.user, this.parallelToolCalls, this.toolCallbacks, this.toolNames, this.httpHeaders,
				this.internalToolExecutionEnabled, this.toolContext, this.outputModalities, this.outputAudio,
				this.store, this.metadata, this.reasoningEffort);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		OpenAiChatOptions other = (OpenAiChatOptions) o;
		return Objects.equals(this.model, other.model) && Objects.equals(this.frequencyPenalty, other.frequencyPenalty)
				&& Objects.equals(this.logitBias, other.logitBias) && Objects.equals(this.logprobs, other.logprobs)
				&& Objects.equals(this.topLogprobs, other.topLogprobs)
				&& Objects.equals(this.maxTokens, other.maxTokens)
				&& Objects.equals(this.maxCompletionTokens, other.maxCompletionTokens)
				&& Objects.equals(this.n, other.n) && Objects.equals(this.presencePenalty, other.presencePenalty)
				&& Objects.equals(this.responseFormat, other.responseFormat)
				&& Objects.equals(this.streamOptions, other.streamOptions) && Objects.equals(this.seed, other.seed)
				&& Objects.equals(this.stop, other.stop) && Objects.equals(this.temperature, other.temperature)
				&& Objects.equals(this.topP, other.topP) && Objects.equals(this.tools, other.tools)
				&& Objects.equals(this.toolChoice, other.toolChoice) && Objects.equals(this.user, other.user)
				&& Objects.equals(this.parallelToolCalls, other.parallelToolCalls)
				&& Objects.equals(this.toolCallbacks, other.toolCallbacks)
				&& Objects.equals(this.toolNames, other.toolNames)
				&& Objects.equals(this.httpHeaders, other.httpHeaders)
				&& Objects.equals(this.toolContext, other.toolContext)
				&& Objects.equals(this.internalToolExecutionEnabled, other.internalToolExecutionEnabled)
				&& Objects.equals(this.outputModalities, other.outputModalities)
				&& Objects.equals(this.outputAudio, other.outputAudio) && Objects.equals(this.store, other.store)
				&& Objects.equals(this.metadata, other.metadata)
				&& Objects.equals(this.reasoningEffort, other.reasoningEffort);
	}

	@Override
	public String toString() {
		return "OpenAiChatOptions: " + ModelOptionsUtils.toJsonString(this);
	}

	public static class Builder {

		protected OpenAiChatOptions options;

		public Builder() {
			this.options = new OpenAiChatOptions();
		}

		public Builder(OpenAiChatOptions options) {
			this.options = options;
		}

		public Builder model(String model) {
			this.options.model = model;
			return this;
		}

		public Builder model(OpenAiApi.ChatModel openAiChatModel) {
			this.options.model = openAiChatModel.getName();
			return this;
		}

		public Builder frequencyPenalty(Double frequencyPenalty) {
			this.options.frequencyPenalty = frequencyPenalty;
			return this;
		}

		public Builder logitBias(Map<String, Integer> logitBias) {
			this.options.logitBias = logitBias;
			return this;
		}

		public Builder logprobs(Boolean logprobs) {
			this.options.logprobs = logprobs;
			return this;
		}

		public Builder topLogprobs(Integer topLogprobs) {
			this.options.topLogprobs = topLogprobs;
			return this;
		}

		public Builder maxTokens(Integer maxTokens) {
			this.options.maxTokens = maxTokens;
			return this;
		}

		public Builder maxCompletionTokens(Integer maxCompletionTokens) {
			this.options.maxCompletionTokens = maxCompletionTokens;
			return this;
		}

		public Builder N(Integer n) {
			this.options.n = n;
			return this;
		}

		public Builder outputModalities(List<String> modalities) {
			this.options.outputModalities = modalities;
			return this;
		}

		public Builder outputAudio(AudioParameters audio) {
			this.options.outputAudio = audio;
			return this;
		}

		public Builder presencePenalty(Double presencePenalty) {
			this.options.presencePenalty = presencePenalty;
			return this;
		}

		public Builder responseFormat(ResponseFormat responseFormat) {
			this.options.responseFormat = responseFormat;
			return this;
		}

		public Builder streamUsage(boolean enableStreamUsage) {
			this.options.streamOptions = (enableStreamUsage) ? StreamOptions.INCLUDE_USAGE : null;
			return this;
		}

		public Builder seed(Integer seed) {
			this.options.seed = seed;
			return this;
		}

		public Builder stop(List<String> stop) {
			this.options.stop = stop;
			return this;
		}

		public Builder temperature(Double temperature) {
			this.options.temperature = temperature;
			return this;
		}

		public Builder topP(Double topP) {
			this.options.topP = topP;
			return this;
		}

		public Builder tools(List<OpenAiApi.FunctionTool> tools) {
			this.options.tools = tools;
			return this;
		}

		public Builder toolChoice(Object toolChoice) {
			this.options.toolChoice = toolChoice;
			return this;
		}

		public Builder user(String user) {
			this.options.user = user;
			return this;
		}

		public Builder parallelToolCalls(Boolean parallelToolCalls) {
			this.options.parallelToolCalls = parallelToolCalls;
			return this;
		}

		public Builder toolCallbacks(List<FunctionCallback> toolCallbacks) {
			this.options.setToolCallbacks(toolCallbacks);
			return this;
		}

		public Builder toolCallbacks(FunctionCallback... toolCallbacks) {
			Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
			this.options.toolCallbacks.addAll(Arrays.asList(toolCallbacks));
			return this;
		}

		public Builder toolNames(Set<String> toolNames) {
			Assert.notNull(toolNames, "toolNames cannot be null");
			this.options.setToolNames(toolNames);
			return this;
		}

		public Builder toolNames(String... toolNames) {
			Assert.notNull(toolNames, "toolNames cannot be null");
			this.options.toolNames.addAll(Set.of(toolNames));
			return this;
		}

		public Builder internalToolExecutionEnabled(@Nullable Boolean internalToolExecutionEnabled) {
			this.options.setInternalToolExecutionEnabled(internalToolExecutionEnabled);
			return this;
		}

		@Deprecated
		public Builder functionCallbacks(List<FunctionCallback> functionCallbacks) {
			return toolCallbacks(functionCallbacks);
		}

		@Deprecated
		public Builder functions(Set<String> functionNames) {
			return toolNames(functionNames);
		}

		@Deprecated
		public Builder function(String functionName) {
			return toolNames(functionName);
		}

		@Deprecated
		public Builder proxyToolCalls(Boolean proxyToolCalls) {
			if (proxyToolCalls != null) {
				this.options.setInternalToolExecutionEnabled(!proxyToolCalls);
			}
			return this;
		}

		public Builder httpHeaders(Map<String, String> httpHeaders) {
			this.options.httpHeaders = httpHeaders;
			return this;
		}

		public Builder toolContext(Map<String, Object> toolContext) {
			if (this.options.toolContext == null) {
				this.options.toolContext = toolContext;
			}
			else {
				this.options.toolContext.putAll(toolContext);
			}
			return this;
		}

		public Builder store(Boolean store) {
			this.options.store = store;
			return this;
		}

		public Builder metadata(Map<String, String> metadata) {
			this.options.metadata = metadata;
			return this;
		}

		public Builder reasoningEffort(String reasoningEffort) {
			this.options.reasoningEffort = reasoningEffort;
			return this;
		}

		public OpenAiChatOptions build() {
			return this.options;
		}

	}

}
