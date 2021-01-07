package it.io.openliberty.guides.metrics;

import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.provider.jsrjsonp.JsrJsonpProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

@TestMethodOrder(OrderAnnotation.class)
public class MetricsIT {

	private static String httpPort;
	private static String httpsPort;
	private static String baseHttpURL;
	private static String baseHttpsURL;
	
	private List<String> metrics;
	private Client client;
	
	private final String INVENTORY_HOSTS = "inventory/systems" ;
	private final String INVENTORY_HOSTNAME = "inventory/systems/localhost" ;
	private final String METRICS_APPLICATION = "metrics/application" ;
	
	@BeforeAll
	public static void oneTimeSetup() {
		httpPort = System.getProperty("http.port");
		httpsPort = System.getProperty("https.port");
		baseHttpURL = "http://localhost:" + httpPort + "/" ;
		baseHttpsURL = "https://localhost:" + httpsPort + "/" ;
	}
	
	@BeforeEach
	public void setup() {
		client = ClientBuilder.newClient();
		client.register(JsrJsonpProvider.class);
	}
	
	@AfterEach 
	public void teardown() {
		client.close();
	}
	
	@Test
	@Order(1)
	public void testPropertiesRequestTimeMetric() {
		connectToEndpoint( baseHttpURL + INVENTORY_HOSTNAME );
		metrics = getMetrics();
		metrics.stream().forEach( metric -> {
			if( metric.startsWith("application_inventoryProcessingTime_rate_per_second")) {
				float seconds = Float.parseFloat( metric.split(" ")[1].trim());
				assertTrue( 4 > seconds );
			}
		});
	}
	
	@Test
	@Order(2)
	public void testInventoryAccessCountMetric() {
		connectToEndpoint( baseHttpURL + INVENTORY_HOSTS );
		metrics = getMetrics();
		metrics.stream().forEach( metric -> {
			if( metric.startsWith("application_inventoryAccessCount_total") ) {
				int count = Integer.parseInt( metric.split(" ")[ metric.split(" ").length - 1 ] );
				assertTrue( 1 <= count );
			}
		});
	}
	
	@Test
	@Order(3)
	public void testInventorySizeGaugeMetric() {
		metrics = getMetrics();
		metrics.stream().forEach( metric -> {
			if( metric.startsWith( "application_inventorySizeGauge" ) ) {
				int value = Character.getNumericValue( metric.charAt( metric.length() - 1 ) );
				assertTrue( 1 <= value );
			}
		});
	}
	
	@Test
	@Order(4)
	public void testPropertiesAddSimplyTimeMetric() {
		connectToEndpoint( baseHttpURL + INVENTORY_HOSTNAME );
		metrics = getMetrics();
		boolean checkMetric = metrics.stream().filter( metric -> {
			return metric.startsWith("application_inventoryAddingTime_total");
		}).count() > 0 ;
		assertTrue(checkMetric);
	}
	
	private List<String> getMetrics(){
		
		String usernameAndPassword = StringUtils.join("admin",":","adminpwd");
		String authorizationHeaderValue = StringUtils.join("Basic ", Base64.getEncoder().encodeToString(usernameAndPassword.getBytes()));
		
		Response metricsResponse = client.target( baseHttpsURL + METRICS_APPLICATION )
				.request(MediaType.TEXT_PLAIN)
				.header("Authorization", authorizationHeaderValue )
				.get();
		
		InputStream is = (InputStream)metricsResponse.getEntity();
		BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
		
		List<String> result = new ArrayList<>();
		String input = null;
		
		try {
			while( ( input = br.readLine() ) != null ) {
				result.add( input );
			}
		}catch(IOException ex) {
			ex.printStackTrace();
			fail();
		}
		
		metricsResponse.close();
		return result;
		
	}
	
	public void connectToEndpoint( String url ) {
		Response response = getResponse( url );
		assertResponse( url , response );
		response.close();
	}
	
	private Response getResponse( String url ) {
		return client.target( url ).request().get();
	}
	
	private void assertResponse( String url , Response response ) {
		assertEquals( 200 , response.getStatus() , "Incorrect response code from " + url );
	}
	
}
