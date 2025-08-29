package com.sovereingschool.back_base.Configurations;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import com.sovereingschool.back_common.Utils.JwtUtil;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

@Configuration
public class WebClientConfig {

	@Value("${BACKEND_INSECURE_SSL:false}")
	private boolean insecureSsl;

	private final JwtUtil jwtUtil;

	public WebClientConfig(JwtUtil jwtUtil) {
		this.jwtUtil = jwtUtil;
	}

	// Bean global que siempre envía JWT
	@Bean
	public WebClient webClient(WebClient.Builder builder) throws Exception {
		HttpClient httpClient = createHttpClient();

		String authToken = this.jwtUtil.generateToken(null, "server", null);

		return builder
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
				.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
				.build();
	}

	// Método para crear WebClient con baseUrl y headers específicos, igual que
	// antes
	public WebClient createSecureWebClient(String baseUrl) throws Exception {
		URI uri = new URI(baseUrl);
		String host = uri.getHost();
		int port = uri.getPort();

		HttpClient httpClient = createHttpClient();

		String authToken = this.jwtUtil.generateToken(null, "server", null);

		String hostHeader = (port == 443 || port == -1)
				? host
				: host + ":" + port;

		return WebClient.builder()
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.baseUrl(baseUrl)
				.defaultHeader(HttpHeaders.HOST, hostHeader)
				.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
				.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
				.build();
	}

	// Método interno que decide si usar InsecureTrustManagerFactory o SSL normal
	private HttpClient createHttpClient() throws Exception {
		if (insecureSsl) {
			SslContext sslContext = SslContextBuilder.forClient()
					.trustManager(InsecureTrustManagerFactory.INSTANCE)
					.build();
			return HttpClient.create()
					.secure(t -> t.sslContext(sslContext))
					.wiretap("reactor.netty.http.client.HttpClient",
							io.netty.handler.logging.LogLevel.DEBUG,
							AdvancedByteBufFormat.TEXTUAL);
		} else {
			return HttpClient.create()
					.secure()
					.wiretap("reactor.netty.http.client.HttpClient",
							io.netty.handler.logging.LogLevel.INFO,
							AdvancedByteBufFormat.TEXTUAL);
		}
	}
}
