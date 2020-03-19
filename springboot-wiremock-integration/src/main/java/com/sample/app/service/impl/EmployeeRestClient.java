package com.sample.app.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.sample.app.exception.EmployeeApiException;
import com.sample.app.model.Employee;
import com.sample.app.service.EmployeeService;

@Service
public class EmployeeRestClient implements EmployeeService {

	@Autowired
	private WebClient webClient;

	@Override
	public List<Employee> emps() {

		return webClient.get().uri("api/v1/employees").retrieve().bodyToFlux(Employee.class).collectList().block();
	}

	@Override
	public Employee byId(int id) {
		try {
			return webClient.get().uri("api/v1/employees/" + id).retrieve().bodyToMono(Employee.class).block();
		} catch (WebClientResponseException e) {
			throw new EmployeeApiException(e.getMessage() + "," + e.getRawStatusCode());
		} catch (Exception e) {
			throw new EmployeeApiException(e.getMessage());
		}

	}

	@Override
	public List<Employee> containsName(String name) {
		String uriToHit = UriComponentsBuilder.fromUriString("api/v1/employees/by-name/").queryParam("empName", name)
				.buildAndExpand().toString();

		return webClient.get().uri(uriToHit).retrieve().bodyToFlux(Employee.class).collectList().block();
	}

	@Override
	public Employee addEmployee(Employee emp) {
		return webClient.post().uri("api/v1/employees").syncBody(emp).retrieve().bodyToMono(Employee.class).block();
	}

	@Override
	public Employee updateEmployee(int id, Employee emp) {
		return webClient.put().uri("api/v1/employees/" + id).syncBody(emp).retrieve().bodyToMono(Employee.class)
				.block();
	}

	@Override
	public Employee deleteEmployee(int id) {
		return webClient.delete().uri("api/v1/employees/" + id).retrieve().bodyToMono(Employee.class).block();
	}

}
