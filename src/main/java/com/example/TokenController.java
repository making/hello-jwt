package com.example;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import com.nimbusds.jwt.JWTClaimsSet;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import static org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER;

@RestController
public class TokenController {
	private final TokenSigner tokenSigner;

	private final AuthenticationManager authenticationManager;

	private final Clock clock;

	public TokenController(TokenSigner tokenSigner, AuthenticationManager authenticationManager, Clock clock) {
		this.tokenSigner = tokenSigner;
		this.authenticationManager = authenticationManager;
		this.clock = clock;
	}

	@PostMapping(path = "/token")
	public Object issueToken(@RequestParam String username, @RequestParam String password, UriComponentsBuilder builder) {
		try {
			Authentication authenticated = authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(username, password));
			UserDetails userDetails = (UserDetails) authenticated.getPrincipal();
			String issuer = builder.path("").build().toString();
			Instant issuedAt = Instant.now(this.clock);
			Instant expiresAt = issuedAt.plus(1, ChronoUnit.HOURS);
			Set<String> scope = Set.of("message:read", "message:write");
			JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
					.issuer(issuer)
					.expirationTime(Date.from(expiresAt))
					.subject(userDetails.getUsername())
					.issueTime(Date.from(issuedAt))
					.claim("scope", scope)
					.build();
			String tokenValue = this.tokenSigner.sign(claimsSet).serialize();
			return ResponseEntity.ok(Map.of("access_token", tokenValue,
					"token_type", BEARER.getValue(),
					"expires_in", Duration.between(issuedAt, expiresAt).getSeconds(),
					"scope", scope));
		}
		catch (AuthenticationException e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("error", "unauthorized",
							"error_description", e.getMessage()));
		}
	}
}
