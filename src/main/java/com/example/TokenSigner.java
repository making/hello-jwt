package com.example;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class TokenSigner implements InitializingBean {
	private final JWSSigner signer;

	private final JWSVerifier verifier;

	public TokenSigner(JwtProperties jwtProps) {
		this.signer = new RSASSASigner(jwtProps.privateKey());
		this.verifier = new RSASSAVerifier(jwtProps.publicKey());
	}

	public SignedJWT sign(JWTClaimsSet claimsSet) {
		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
				.type(JOSEObjectType.JWT)
				.build();
		SignedJWT signedJWT = new SignedJWT(header, claimsSet);
		try {
			signedJWT.sign(this.signer);
		}
		catch (JOSEException e) {
			throw new IllegalStateException(e);
		}
		return signedJWT;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// Validate the key-pair
		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject("test").build();
		SignedJWT signedJWT = sign(claimsSet);
		if (!signedJWT.verify(this.verifier)) {
			throw new IllegalStateException("The pair of public key and private key is wrong.");
		}
	}
}
