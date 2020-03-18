package com.sample.app.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.serviceUnavailable;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.sample.app.exception.EmployeeApiException;
import com.sample.app.model.Employee;
import com.sample.app.service.impl.EmployeeRestClient;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

public class EmployeeRestClientTest {
	private static final int HTTP_SERVICE_PORT = 8888;
	private static final int HTTPS_SERVICE_PORT = 9999;

	private List<Employee> ALL_EMPLOYEES = new ArrayList<>();

	private EmployeeRestClient empRestClient;
	private String baseURI;

	private static int idCounter = 1;

	private static Employee buildEmployee(String firstName, String lastName) {
		Employee emp = new Employee();
		emp.setId(idCounter);
		emp.setFirstName(firstName);
		emp.setLastName(lastName);

		idCounter++;

		return emp;
	}

	@Before
	public void initializeEmployees() {
		Employee emp1 = buildEmployee("Deepak", "Moud");
		Employee emp2 = buildEmployee("Srinivasa Rao", "Gumma");
		Employee emp3 = buildEmployee("Purna Chandra", "Rao");
		Employee emp4 = buildEmployee("Madhavi Latha", "Gumma");
		Employee emp5 = buildEmployee("Raghava", "Reddy");
		Employee emp6 = buildEmployee("Ramesh Chandra", "Dokku");

		ALL_EMPLOYEES.addAll(Arrays.asList(emp1, emp2, emp3, emp4, emp5, emp6));

	}

	@Before
	public void setup() {
		baseURI = "http://localhost:" + wireMockRule.port() + "/";

		TcpClient tcpClient = TcpClient.create().option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
				.doOnConnected(conn -> {
					conn.addHandlerLast(new ReadTimeoutHandler(3)).addHandlerLast(new WriteTimeoutHandler(3));
				});

		WebClient webClient = WebClient.builder()
				.clientConnector(new ReactorClientHttpConnector(HttpClient.from(tcpClient))).baseUrl(baseURI).build();

		empRestClient = new EmployeeRestClient(webClient);
	}

	@Rule
	public WireMockRule wireMockRule = new WireMockRule(
			WireMockConfiguration.options().port(HTTP_SERVICE_PORT).httpsPort(HTTPS_SERVICE_PORT)
					.notifier(new ConsoleNotifier(true)).extensions(new ResponseTemplateTransformer(true)));

	@Test
	public void allEmployeeForAnyURL() {

		wireMockRule.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200)
				.withHeader("Content-Type", "application/json").withBody(Json.write(ALL_EMPLOYEES))));

		List<Employee> resultFromService = empRestClient.emps();

		assertEquals(resultFromService.size(), 6);

	}

	@Test
	public void allEmployeeForEactURLPath() {

		wireMockRule.stubFor(get(urlPathEqualTo("/api/v1/employees")).willReturn(aResponse().withStatus(200)
				.withHeader("Content-Type", "application/json").withBody(Json.write(ALL_EMPLOYEES))));

		List<Employee> resultFromService = empRestClient.emps();

		assertEquals(resultFromService.size(), 6);
		verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/v1/employees")));
	}

	@Test
	public void getEmployeeById() {

		Employee emp = new Employee();
		emp.setId(Integer.MAX_VALUE);
		emp.setFirstName("Chamu");
		emp.setLastName("Gurram");

		wireMockRule.stubFor(get(urlPathMatching("/api/v1/employees/[1-5]")).willReturn(
				aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(Json.write(emp))));

		for (int i = 1; i < 6; i++) {

			Employee resultEmp = empRestClient.byId(1);

			assertNotNull(resultEmp);
			assertEquals(Integer.MAX_VALUE, resultEmp.getId());
			assertEquals("Chamu", resultEmp.getFirstName());
			assertEquals("Gurram", resultEmp.getLastName());

		}

	}

	@Test
	public void allEmployeeFromFile() {

		wireMockRule.stubFor(get(urlPathEqualTo("/api/v1/employees")).willReturn(aResponse().withStatus(200)
				.withHeader("Content-Type", "application/json").withBodyFile("allEmployees.json")));

		List<Employee> resultFromService = empRestClient.emps();

		assertEquals(resultFromService.size(), 2);

		Employee emp = resultFromService.get(0);

		if (emp.getId() == 1) {
			assertEquals("Ram", emp.getFirstName());
			assertEquals("Ponnam", emp.getLastName());
		} else if (emp.getId() == 2) {
			assertEquals("Lakshman", emp.getFirstName());
			assertEquals("Gurram", emp.getLastName());
		}

		verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/v1/employees")));
	}

	@Test
	public void getEmployeeByIdUsingResponseTemplate() {

		wireMockRule.stubFor(get(urlPathMatching("/api/v1/employees/3")).willReturn(aResponse().withStatus(200)
				.withHeader("Content-Type", "application/json").withBodyFile("employeeByIdTemplate.json")));

		Employee resultEmp = empRestClient.byId(3);

		assertNotNull(resultEmp);
		assertEquals(3, resultEmp.getId());
		assertEquals("Ram", resultEmp.getFirstName());
		assertEquals("Ponnam", resultEmp.getLastName());

		verify(exactly(1), getRequestedFor(urlPathMatching("/api/v1/employees/3")));

	}

	@Test(expected = EmployeeApiException.class)
	public void getEmployeeByIdNotFound() {

		int empId = 12345;

		wireMockRule.stubFor(get(urlPathMatching("/api/v1/employees/" + empId))
				.willReturn(aResponse().withStatus(404).withHeader("Content-Type", "application/json")));

		empRestClient.byId(12345);

		verify(exactly(1), getRequestedFor(urlPathMatching("/api/v1/employees/" + empId)));

	}

	@Test(expected = EmployeeApiException.class)
	public void getEmployeeByIdNotFoundReturnErrorPayloadFromFile() {

		int empId = 12345;

		wireMockRule.stubFor(get(urlPathMatching("/api/v1/employees/" + empId)).willReturn(
				aResponse().withStatus(404).withHeader("Content-Type", "application/json").withBodyFile("404.json")));

		empRestClient.byId(12345);

		verify(exactly(1), getRequestedFor(urlPathMatching("/api/v1/employees/" + empId)));

	}

	@Test
	public void getEmployeesContainsNameViaQueryParam() {
		List<Employee> employeesContainNameGurram = new ArrayList<>();

		employeesContainNameGurram.add(buildEmployee("Bala Krishna", "Gurram"));
		employeesContainNameGurram.add(buildEmployee("Gurram", "Srinu"));

		String nameSubStr = "Gurram";

		wireMockRule.stubFor(
				get(urlPathEqualTo("/api/v1/employees/by-name/")).withQueryParam("empName", equalTo(nameSubStr))
						.willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
								.withBody(Json.write(employeesContainNameGurram))));

		List<Employee> resultFromService = empRestClient.containsName(nameSubStr);

		assertEquals(resultFromService.size(), 2);

	}

	@Test
	public void getEmployeesContainsNameViaResponseTemplate() {
		String nameSubStr = "Kumar";

		wireMockRule.stubFor(get(urlPathEqualTo("/api/v1/employees/by-name/"))
				.withQueryParam("empName", equalTo(nameSubStr)).willReturn(aResponse().withStatus(200)
						.withHeader("Content-Type", "application/json").withBodyFile("queryNameTemplate.json")));

		List<Employee> emps = empRestClient.containsName(nameSubStr);

		assertEquals(emps.size(), 2);

		for (Employee emp : emps) {
			if (emp.getId() == 1) {
				assertEquals("Kumar", emp.getFirstName());
				assertEquals("Ponnam", emp.getLastName());
			} else if (emp.getId() == 2) {
				assertEquals("Lakshman", emp.getFirstName());
				assertEquals("Kumar", emp.getLastName());
			}

		}

	}

	@Test
	public void createNewEmployee() {

		Employee emp = new Employee();
		emp.setId(123);
		emp.setFirstName("Bala");
		emp.setLastName("Gurram");

		wireMockRule.stubFor(post(urlPathEqualTo("/api/v1/employees")).willReturn(
				aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(Json.write(emp))));

		Employee newEmployee = empRestClient.addEmployee(emp);

		assertEquals(newEmployee.getId(), 123);
		assertEquals(newEmployee.getFirstName(), "Bala");
		assertEquals(newEmployee.getLastName(), "Gurram");

		verify(exactly(1), postRequestedFor(urlPathEqualTo("/api/v1/employees")));

	}

	@Test
	public void createNewEmployeeValidateRequestPayload() {

		Employee emp = new Employee();
		emp.setId(123);
		emp.setFirstName("Bala");
		emp.setLastName("Gurram");

		wireMockRule.stubFor(post(urlPathEqualTo("/api/v1/employees")).withRequestBody(matchingJsonPath("$.firstName"))
				.withRequestBody(matchingJsonPath("$.lastName")).willReturn(aResponse().withStatus(200)
						.withHeader("Content-Type", "application/json").withBody(Json.write(emp))));

		Employee empPayLoad = new Employee();
		empPayLoad.setId(123);
		// empPayLoad.setFirstName("Bala"); //uncomment this to make the test pass
		empPayLoad.setLastName("Gurram");

		Employee newEmployee = empRestClient.addEmployee(empPayLoad);

		assertEquals(newEmployee.getId(), 123);
		assertEquals(newEmployee.getFirstName(), "Bala");
		assertEquals(newEmployee.getLastName(), "Gurram");

		verify(exactly(1), postRequestedFor(urlPathEqualTo("/api/v1/employees")));

	}

	@Test
	public void createNewEmployeeValidateRequestPayloadValues() {

		Employee emp = new Employee();
		emp.setId(123);
		emp.setFirstName("Bala");
		emp.setLastName("Gurram");

		wireMockRule.stubFor(post(urlPathEqualTo("/api/v1/employees"))
				.withRequestBody(matchingJsonPath("$.firstName", equalTo("Bala")))
				.withRequestBody(matchingJsonPath("$.lastName", equalTo("Gurram"))).willReturn(aResponse()
						.withStatus(200).withHeader("Content-Type", "application/json").withBody(Json.write(emp))));

		Employee empPayLoad = new Employee();
		empPayLoad.setId(123);
		empPayLoad.setFirstName("Bala123"); // Set first name to Bala to make the test pass
		empPayLoad.setLastName("Gurram");

		Employee newEmployee = empRestClient.addEmployee(empPayLoad);

		assertEquals(newEmployee.getId(), 123);
		assertEquals(newEmployee.getFirstName(), "Bala");
		assertEquals(newEmployee.getLastName(), "Gurram");

		verify(exactly(1), postRequestedFor(urlPathEqualTo("/api/v1/employees")));
	}

	@Test
	public void createNewEmployeeViaResponseTemplate() {

		wireMockRule.stubFor(post(urlPathEqualTo("/api/v1/employees")).willReturn(aResponse().withStatus(200)
				.withHeader("Content-Type", "application/json").withBodyFile("newEmployeeTemplate.json")));

		Employee empPayLoad = new Employee();
		empPayLoad.setFirstName("Bala");
		empPayLoad.setLastName("Gurram");

		Employee newEmployee = empRestClient.addEmployee(empPayLoad);

		assertEquals(newEmployee.getFirstName(), "Bala");
		assertEquals(newEmployee.getLastName(), "Gurram");

		verify(exactly(1), postRequestedFor(urlPathEqualTo("/api/v1/employees")));

	}

	@Test
	public void updateEmployee() {

		int empId = 123;

		wireMockRule.stubFor(put(urlPathEqualTo("/api/v1/employees/" + empId)).willReturn(aResponse().withStatus(200)
				.withHeader("Content-Type", "application/json").withBodyFile("updateEmployeeTemplate.json")));

		Employee emp = new Employee();
		emp.setFirstName("Bala");
		emp.setLastName("Gurram");

		Employee newEmployee = empRestClient.updateEmployee(empId, emp);

		assertEquals(newEmployee.getId(), 123);
		assertEquals(newEmployee.getFirstName(), "Bala");
		assertEquals(newEmployee.getLastName(), "Gurram");

		verify(exactly(1), putRequestedFor(urlPathEqualTo("/api/v1/employees/" + empId)));
	}

	@Test
	public void deleteEmployee() {

		int empId = 123;

		Employee emp = new Employee();
		emp.setId(empId);
		emp.setFirstName("Bala");
		emp.setLastName("Gurram");

		wireMockRule.stubFor(delete(urlPathEqualTo("/api/v1/employees/" + empId)).willReturn(
				aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(Json.write(emp))));

		Employee deletedEmployee = empRestClient.deleteEmployee(empId);

		assertEquals(deletedEmployee.getId(), 123);
		assertEquals(deletedEmployee.getFirstName(), "Bala");
		assertEquals(deletedEmployee.getLastName(), "Gurram");

		verify(exactly(1), deleteRequestedFor(urlPathEqualTo("/api/v1/employees/" + empId)));

	}

	@Test(expected = EmployeeApiException.class)
	public void simulate_500() {

		Employee emp = new Employee();
		emp.setId(1);
		emp.setFirstName("Chamu");
		emp.setLastName("Gurram");

		wireMockRule.stubFor(get(urlPathMatching("/api/v1/employees/1")).willReturn(serverError()));

		empRestClient.byId(1);

	}

	@Test(expected = EmployeeApiException.class)
	public void simulate_serverError_500() {

		Employee emp = new Employee();
		emp.setId(1);
		emp.setFirstName("Chamu");
		emp.setLastName("Gurram");

		wireMockRule.stubFor(get(urlPathMatching("/api/v1/employees/1")).willReturn(serverError().withStatus(500)));

		empRestClient.byId(1);

	}

	@Test(expected = EmployeeApiException.class)
	public void simulate_serviceUnavailableError_503() {

		Employee emp = new Employee();
		emp.setId(1);
		emp.setFirstName("Chamu");
		emp.setLastName("Gurram");

		wireMockRule.stubFor(get(urlPathMatching("/api/v1/employees/1")).willReturn(serviceUnavailable()));

		empRestClient.byId(1);

	}

	@Test(expected = EmployeeApiException.class)
	public void simulate_fault_emptyResponse() {

		Employee emp = new Employee();
		emp.setId(1);
		emp.setFirstName("Chamu");
		emp.setLastName("Gurram");

		wireMockRule.stubFor(
				get(urlPathMatching("/api/v1/employees/1")).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));

		empRestClient.byId(1);

	}

	@Test(expected = EmployeeApiException.class)
	public void simulate_fault_connectionResetByPeer() {

		Employee emp = new Employee();
		emp.setId(1);
		emp.setFirstName("Chamu");
		emp.setLastName("Gurram");

		wireMockRule.stubFor(get(urlPathMatching("/api/v1/employees/1"))
				.willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

		empRestClient.byId(1);

	}

	@Test(expected = EmployeeApiException.class)
	public void simulate_fault_malformedResponseChunk() {

		Employee emp = new Employee();
		emp.setId(1);
		emp.setFirstName("Chamu");
		emp.setLastName("Gurram");

		wireMockRule.stubFor(get(urlPathMatching("/api/v1/employees/1"))
				.willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)));

		empRestClient.byId(1);

	}

	@Test(expected = EmployeeApiException.class)
	public void simulate_fault_randomDataThenClose() {

		Employee emp = new Employee();
		emp.setId(1);
		emp.setFirstName("Chamu");
		emp.setLastName("Gurram");

		wireMockRule.stubFor(get(urlPathMatching("/api/v1/employees/1"))
				.willReturn(aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE)));

		empRestClient.byId(1);

	}

	@Test(expected = EmployeeApiException.class)
	public void injectFixedDelay() {

		Employee emp = new Employee();
		emp.setId(1);
		emp.setFirstName("Chamu");
		emp.setLastName("Gurram");

		wireMockRule.stubFor(get(urlPathMatching("/api/v1/employees/1")).willReturn(aResponse().withFixedDelay(9000)));

		empRestClient.byId(1);
	}

	@Test(expected = EmployeeApiException.class)
	public void injectUniformRandomDelay() {

		Employee emp = new Employee();
		emp.setId(1);
		emp.setFirstName("Chamu");
		emp.setLastName("Gurram");

		wireMockRule.stubFor(
				get(urlPathMatching("/api/v1/employees/1")).willReturn(aResponse().withUniformRandomDelay(6000, 9000)));

		empRestClient.byId(1);
	}

}