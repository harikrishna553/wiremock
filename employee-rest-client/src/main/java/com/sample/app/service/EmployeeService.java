package com.sample.app.service;

import java.util.List;

import com.sample.app.model.Employee;

public interface EmployeeService {

	public List<Employee> emps();

	public Employee byId(int id);

	public List<Employee> containsName(String name);

	public Employee addEmployee(Employee emp);

	public Employee updateEmployee(int id, Employee emp);

	public Employee deleteEmployee(int id);

}
