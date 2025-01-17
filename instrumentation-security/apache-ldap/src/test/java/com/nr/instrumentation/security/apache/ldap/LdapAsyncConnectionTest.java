package com.nr.instrumentation.security.apache.ldap;

import com.newrelic.agent.security.introspec.InstrumentationTestConfig;
import com.newrelic.agent.security.introspec.SecurityInstrumentationTestRunner;
import com.newrelic.agent.security.introspec.SecurityIntrospector;
import com.newrelic.api.agent.security.schema.AbstractOperation;
import com.newrelic.api.agent.security.schema.VulnerabilityCaseType;
import com.newrelic.api.agent.security.schema.operation.LDAPOperation;
import org.apache.directory.api.ldap.codec.decorators.SearchResultEntryDecorator;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapAsyncConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.future.SearchFuture;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.zapodot.junit.ldap.EmbeddedLdapRule;
import org.zapodot.junit.ldap.EmbeddedLdapRuleBuilder;

import java.util.List;

@RunWith(SecurityInstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.apache.directory.ldap.client.api", "com.nr.instrumentation.security.apache.ldap" })
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LdapAsyncConnectionTest {
    public static final String DOMAIN_DSN = "dc=example,dc=com";
    @ClassRule
    public static EmbeddedLdapRule embeddedLdapRule = EmbeddedLdapRuleBuilder.newInstance().usingDomainDsn(DOMAIN_DSN)
            .importingLdifs("users-import.ldif").build();

    @Test
    public void testSearch() throws LdapException, InterruptedException {
        int port = embeddedLdapRule.embeddedServerPort();
        String username = "mlakshkar";
        String password = "abc456";

        String query = String.format("(&(uid=%s)(userPassword=%s))", username, password);
        System.out.println("LDAP query: " + query);

        LdapAsyncConnection connection = new LdapNetworkConnection("localhost", port);
        SearchFuture future = connection.searchAsync(DOMAIN_DSN, query, SearchScope.SUBTREE, "");
        SearchResultEntryDecorator cursor = (SearchResultEntryDecorator) future.get();
        System.out.println(cursor.getEntry());

        SecurityIntrospector introspector = SecurityInstrumentationTestRunner.getIntrospector();
        List<AbstractOperation> operations = introspector.getOperations();
        Assert.assertTrue("No operations detected.", operations.size() > 0);
        LDAPOperation operation = (LDAPOperation) operations.get(0);
        Assert.assertEquals("Invalid executed baseDn.", DOMAIN_DSN, operation.getName());
        Assert.assertEquals("Invalid executed parameters.", query, operation.getFilter());
        Assert.assertEquals("Invalid event category.", VulnerabilityCaseType.LDAP, operation.getCaseType());
        Assert.assertEquals("Invalid executed class name.", LdapNetworkConnection.class.getName(), operation.getClassName());
        Assert.assertEquals("Invalid executed method name.", "searchAsync", operation.getMethodName());
    }

    @Test
    public void testSearch1() throws LdapException, InterruptedException {
        int port = embeddedLdapRule.embeddedServerPort();
        String username = "sclaus";

        String query = String.format("(&(uid=%s))", username);
        System.out.println("LDAP query: " + query);

        LdapAsyncConnection connection = new LdapNetworkConnection("localhost", port);
        SearchFuture future = connection.searchAsync(DOMAIN_DSN, query, SearchScope.SUBTREE, "cn");
        SearchResultEntryDecorator cursor = (SearchResultEntryDecorator) future.get();
        System.out.println(cursor.getEntry());

        SecurityIntrospector introspector = SecurityInstrumentationTestRunner.getIntrospector();
        List<AbstractOperation> operations = introspector.getOperations();
        Assert.assertTrue("No operations detected.", operations.size() > 0);
        LDAPOperation operation = (LDAPOperation) operations.get(0);
        Assert.assertEquals("Invalid executed baseDn.", DOMAIN_DSN, operation.getName());
        Assert.assertEquals("Invalid executed parameters.", query, operation.getFilter());
        Assert.assertEquals("Invalid event category.", VulnerabilityCaseType.LDAP, operation.getCaseType());
        Assert.assertEquals("Invalid executed class name.", LdapNetworkConnection.class.getName(), operation.getClassName());
        Assert.assertEquals("Invalid executed method name.", "searchAsync", operation.getMethodName());
    }

    @Test
    public void testSearch2() throws LdapException, InterruptedException {
        int port = embeddedLdapRule.embeddedServerPort();
        String username = "mlakshkar";

        String query = String.format("(&(uid=%s))", username);
        System.out.println("LDAP query: " + query);

        LdapAsyncConnection connection = new LdapNetworkConnection("localhost", port);
        SearchFuture future = connection.searchAsync(new Dn(DOMAIN_DSN), query, SearchScope.SUBTREE, "");
        SearchResultEntryDecorator cursor = (SearchResultEntryDecorator) future.get();
        System.out.println(cursor.getEntry());

        SecurityIntrospector introspector = SecurityInstrumentationTestRunner.getIntrospector();
        List<AbstractOperation> operations = introspector.getOperations();
        Assert.assertTrue("No operations detected.", operations.size() > 0);
        LDAPOperation operation = (LDAPOperation) operations.get(0);
        Assert.assertEquals("Invalid executed baseDn.", DOMAIN_DSN, operation.getName());
        Assert.assertEquals("Invalid executed parameters.", query, operation.getFilter());
        Assert.assertEquals("Invalid event category.", VulnerabilityCaseType.LDAP, operation.getCaseType());
        Assert.assertEquals("Invalid executed class name.", LdapNetworkConnection.class.getName(), operation.getClassName());
        Assert.assertEquals("Invalid executed method name.", "searchAsync", operation.getMethodName());
    }

    @Test
    public void testSearch3() throws LdapException, InterruptedException {
        int port = embeddedLdapRule.embeddedServerPort();
        String password = "123efg";

        String query = String.format("(&(userPassword=%s))", password);
        System.out.println("LDAP query: " + query);

        LdapAsyncConnection connection = new LdapNetworkConnection("localhost", port);
        SearchFuture future = connection.searchAsync(Dn.EMPTY_DN, query, SearchScope.SUBTREE, "");
        SearchResultEntryDecorator cursor = (SearchResultEntryDecorator) future.get();
        System.out.println(cursor.getEntry());

        SecurityIntrospector introspector = SecurityInstrumentationTestRunner.getIntrospector();
        List<AbstractOperation> operations = introspector.getOperations();
        Assert.assertTrue("No operations detected.", operations.size() > 0);
        LDAPOperation operation = (LDAPOperation) operations.get(0);
        Assert.assertEquals("Invalid executed baseDn.", "", operation.getName());
        Assert.assertEquals("Invalid executed parameters.", query, operation.getFilter());
        Assert.assertEquals("Invalid event category.", VulnerabilityCaseType.LDAP, operation.getCaseType());
        Assert.assertEquals("Invalid executed class name.", LdapNetworkConnection.class.getName(), operation.getClassName());
        Assert.assertEquals("Invalid executed method name.", "searchAsync", operation.getMethodName());
    }

    @Test
    public void testSearch4() throws LdapException, InterruptedException {
        int port = embeddedLdapRule.embeddedServerPort();
        String password = "abc456";

        String query = String.format("(&(userPassword=%s))", password);
        System.out.println("LDAP query: " + query);

        LdapAsyncConnection connection = new LdapNetworkConnection("localhost", port);
        SearchRequest request = new SearchRequestImpl();
        request.setBase(new Dn(DOMAIN_DSN));
        request.setFilter(query);
        request.setScope(SearchScope.SUBTREE);

        SearchFuture future = connection.searchAsync(request);
        SearchResultEntryDecorator cursor = (SearchResultEntryDecorator) future.get();
        System.out.println(cursor.getEntry());

        SecurityIntrospector introspector = SecurityInstrumentationTestRunner.getIntrospector();
        List<AbstractOperation> operations = introspector.getOperations();
        Assert.assertTrue("No operations detected.", operations.size() > 0);
        LDAPOperation operation = (LDAPOperation) operations.get(0);
        Assert.assertEquals("Invalid executed baseDn.", DOMAIN_DSN, operation.getName());
        Assert.assertEquals("Invalid executed parameters.", query, operation.getFilter());
        Assert.assertEquals("Invalid event category.", VulnerabilityCaseType.LDAP, operation.getCaseType());
        Assert.assertEquals("Invalid executed class name.", LdapNetworkConnection.class.getName(), operation.getClassName());
        Assert.assertEquals("Invalid executed method name.", "searchAsync", operation.getMethodName());
    }

    @Test
    public void testSearch5() throws LdapException, InterruptedException {
        int port = embeddedLdapRule.embeddedServerPort();
        String username = "sclaus";
        String password = "123efg";

        String query = String.format("(&(uid=%s)(userPassword=%s))", username, password);
        System.out.println("LDAP query: " + query);

        LdapAsyncConnection connection = new LdapNetworkConnection("localhost", port);
        SearchRequest request = new SearchRequestImpl();
        request.setBase(Dn.EMPTY_DN);
        request.setFilter(query);
        request.setScope(SearchScope.SUBTREE);

        SearchFuture future = connection.searchAsync(request);
        SearchResultEntryDecorator cursor = (SearchResultEntryDecorator) future.get();
        System.out.println(cursor.getEntry());

        SecurityIntrospector introspector = SecurityInstrumentationTestRunner.getIntrospector();
        List<AbstractOperation> operations = introspector.getOperations();
        Assert.assertTrue("No operations detected.", operations.size() > 0);
        LDAPOperation operation = (LDAPOperation) operations.get(0);
        Assert.assertEquals("Invalid executed baseDn.", "", operation.getName());
        Assert.assertEquals("Invalid executed parameters.", query, operation.getFilter());
        Assert.assertEquals("Invalid event category.", VulnerabilityCaseType.LDAP, operation.getCaseType());
        Assert.assertEquals("Invalid executed class name.", LdapNetworkConnection.class.getName(), operation.getClassName());
        Assert.assertEquals("Invalid executed method name.", "searchAsync", operation.getMethodName());
    }
}
