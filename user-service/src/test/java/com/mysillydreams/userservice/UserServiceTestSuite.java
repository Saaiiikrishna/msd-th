package com.mysillydreams.userservice;

import com.mysillydreams.userservice.controller.UserControllerIntegrationTest;
import com.mysillydreams.userservice.repository.UserRepositoryTest;
import com.mysillydreams.userservice.security.SecurityTest;
import com.mysillydreams.userservice.service.UserServiceTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Comprehensive test suite for User Service.
 * Runs all unit tests, integration tests, and security tests.
 */
@Suite
@SuiteDisplayName("User Service Test Suite")
@SelectClasses({
    // Unit Tests
    UserServiceTest.class,
    
    // Repository Tests
    UserRepositoryTest.class,
    
    // Integration Tests
    UserControllerIntegrationTest.class,
    
    // Security Tests
    SecurityTest.class
})
public class UserServiceTestSuite {
    // Test suite configuration class
    // All tests are configured via annotations
}
