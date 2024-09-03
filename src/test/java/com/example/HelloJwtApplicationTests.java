package com.example;

import java.util.List;

import com.example.MessageController.Message;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.NoOpResponseErrorHandler;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureJsonTesters
class HelloJwtApplicationTests {
	@LocalServerPort
	int port;

	RestClient restClient;

	@Autowired
	JacksonTester<Message> messageTester;

	@Autowired
	JacksonTester<List<Message>> listTester;

	@BeforeEach
	void setUp(@Autowired RestClient.Builder restClientBuilder) {
		this.restClient = restClientBuilder
				.baseUrl("http://localhost:" + port)
				.defaultStatusHandler(new NoOpResponseErrorHandler())
				.build();
	}

	@Test
	void issueTokenUsingValidCredentialsAndAccessMessageApi() throws Exception {
		String token;
		{
			ResponseEntity<JsonNode> response = this.restClient.post()
					.uri("/token")
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body("username=bar@example.com&password=secret")
					.retrieve()
					.toEntity(JsonNode.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(response.getBody()).isNotEmpty();
			token = response.getBody().get("access_token").asText();
		}
		{
			ResponseEntity<Message> response = this.restClient.post()
					.uri("/messages")
					.contentType(MediaType.TEXT_PLAIN)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
					.body("Hello World")
					.retrieve()
					.toEntity(Message.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(response.getBody()).isNotNull();
			assertThat(this.messageTester.write(response.getBody())).isEqualToJson("""
					{
					  "username": "bar@example.com",
					  "text": "Hello World"
					}
					""");
		}
		{
			ResponseEntity<List<Message>> response = this.restClient.get()
					.uri("/messages")
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
					.retrieve()
					.toEntity(new ParameterizedTypeReference<>() {
					});
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(response.getBody()).isNotNull();
			assertThat(this.listTester.write(response.getBody())).isEqualToJson("""
					[
						{
						  "username": "bar@example.com",
						  "text": "Hello World"
						}
					]
					""");
		}
	}

	@Test
	void issueTokenUsingInvalidCredentials() {
		assertThatThrownBy(() -> this.restClient.post()
				.uri("/token")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body("username=bar@example.com&password=bar")
				.retrieve()
				.toEntity(JsonNode.class))
				.isInstanceOf(HttpClientErrorException.Unauthorized.class);
	}

}
