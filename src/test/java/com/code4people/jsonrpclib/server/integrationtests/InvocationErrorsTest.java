package com.code4people.jsonrpclib.server.integrationtests;

import com.code4people.jsonrpclib.server.ServiceActivator;
import com.code4people.jsonrpclib.server.ServiceActivatorBuilder;
import com.code4people.jsonrpclib.binding.annotations.Bind;
import com.code4people.jsonrpclib.binding.annotations.ErrorMapping;
import com.code4people.jsonrpclib.binding.annotations.ParamsType;
import com.code4people.jsonrpclib.server.exceptions.CustomErrorException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InvocationErrorsTest {

    private ServiceActivator serviceActivator;

    @Before
    public void setUp() throws Exception {
        serviceActivator = ServiceActivatorBuilder
                .create()
                .register(Receiver.class, Receiver::new)
                .build();
    }

    @Test
    public void callWithInvalidParams_shouldReturnError() {
        String message = "{" +
                "   \"jsonrpc\": \"2.0\", " +
                "   \"method\": \"method\", " +
                "   \"params\": {\"i\":1, \"j\":2}, " +
                "   \"id\": \"1\"" +
                "}";
        String response = serviceActivator.processMessage(message).get();

        assertEquals(
                "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"error\":{\"code\":-32602,\"message\":\"Invalid params\"}}",
                response);
    }

    @Test
    public void callWithDeserializationFailure_shouldReturnError() {
        String message = "{" +
                "   \"jsonrpc\": \"2.0\", " +
                "   \"method\": \"method\", " +
                "   \"params\": [\"abc\", \"def\"], " +
                "   \"id\": \"1\"" +
                "}";
        String response = serviceActivator.processMessage(message).get();

        assertEquals(
                "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"error\":{\"code\":-32602,\"message\":\"Invalid params\"}}",
                response);
    }

    @Test
    public void callWithInvalidParamsCount_shouldReturnError() {
        String message = "{" +
                "   \"jsonrpc\": \"2.0\", " +
                "   \"method\": \"method\", " +
                "   \"params\": [1], " +
                "   \"id\": \"1\"" +
                "}";
        String response = serviceActivator.processMessage(message).get();

        assertEquals(
                "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"error\":{\"code\":-32602,\"message\":\"Invalid params\"}}",
                response);
    }

    @Test
    public void callForNonExistingMethod_shouldReturnError() {
        String message = "{" +
                "   \"jsonrpc\": \"2.0\", " +
                "   \"method\": \"foobar\", " +
                "   \"id\": \"1\"" +
                "}";
        String response = serviceActivator.processMessage(message).get();

        assertEquals(
                "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"error\":{\"code\":-32601,\"message\":\"Method not found\"}}",
                response);
    }

    @Test
    public void callWithInvalidJson_shouldReturnError() {
        String message = "{" +
                "   \"jsonrpc\": \"2.0\"," +
                "   \"method\": \"foobar, " +
                "   \"params\": \"bar\", \"baz]";
        String response = serviceActivator.processMessage(message).get();

        assertEquals(
                "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32700,\"message\":\"Parse error\"}}",
                response);
    }

    @Test
    public void callWithInvalidRequest_shouldReturnError() {
        String message = "{" +
                "   \"jsonrpc\": \"2.0\"," +
                "   \"method\": 1," +
                "   \"params\": \"bar\"" +
                "}";
        String response = serviceActivator.processMessage(message).get();

        assertEquals(
                "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32600,\"message\":\"Invalid Request\"}}",
                response);
    }

    @Test
    public void batchCallWithInvalidJson_shouldReturnError() {
        String message = "[\n" +
                "  {\"jsonrpc\": \"2.0\", \"method\": \"sum\", \"params\": [1,2,4], \"id\": \"1\"},\n" +
                "  {\"jsonrpc\": \"2.0\", \"method\"\n" +
                "]";
        String response = serviceActivator.processMessage(message).get();

        assertEquals(
                "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32700,\"message\":\"Parse error\"}}",
                response);
    }

    @Test
    public void emptyBatchCall_shouldReturnError() {
        String response = serviceActivator.processMessage("[]").get();

        assertEquals(
                "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32600,\"message\":\"Invalid Request\"}}",
                response);
    }

    @Test
    public void batchCallWithInvalidRequest_shouldReturnBatchWithError() {
        String response = serviceActivator.processMessage("[1]").get();

        assertEquals(
                "[{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32600,\"message\":\"Invalid Request\"}}]",
                response);
    }

    @Test
    public void batchCallWithInvalidRequests_shouldReturnBatchWithErrors() {
        String response = serviceActivator.processMessage("[1,2,4]").get();

        assertEquals(
                "[" +
                        "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32600,\"message\":\"Invalid Request\"}}," +
                        "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32600,\"message\":\"Invalid Request\"}}," +
                        "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32600,\"message\":\"Invalid Request\"}}" +
                        "]",
                response);
    }

    @Test
    public void callThatResultsWithCustomError_shouldReturnError() {
        String message = "{" +
                "   \"jsonrpc\": \"2.0\", " +
                "   \"method\": \"methodWithCustomError\", " +
                "   \"id\": \"1\"" +
                "}";
        String response = serviceActivator.processMessage(message).get();

        assertEquals(
                "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"error\":{\"code\":-32099,\"message\":\"This is custom error message.\",\"data\":{\"field\":\"value\"}}}",
                response);
    }

    @Test
    public void callThatResultsWithUnexpectedError_shouldReturnError() {
        String message = "{" +
                "   \"jsonrpc\": \"2.0\", " +
                "   \"method\": \"methodWithUnexpectedError\", " +
                "   \"id\": \"1\"" +
                "}";
        String response = serviceActivator.processMessage(message).get();

        assertEquals(
                "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"error\":{\"code\":-32603,\"message\":\"Internal error\"}}",
                response);
    }

    @Test
    public void callThatResultsWithMethodMappedError_shouldReturnError() {
        String message = "{" +
                "   \"jsonrpc\": \"2.0\", " +
                "   \"method\": \"methodWithMappedError1\", " +
                "   \"id\": \"1\"" +
                "}";
        String response = serviceActivator.processMessage(message).get();

        assertEquals(
                "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"error\":{\"code\":32000,\"message\":\"This is custom message\"}}",
                response);
    }

    @Test
    public void callThatResultsWithClassMappedError_shouldReturnError() {
        String message = "{" +
                "   \"jsonrpc\": \"2.0\", " +
                "   \"method\": \"methodWithMappedError2\", " +
                "   \"id\": \"1\"" +
                "}";
        String response = serviceActivator.processMessage(message).get();

        assertEquals(
                "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"error\":{\"code\":33000,\"message\":\"This is custom message\"}}",
                response);
    }

    @ErrorMapping(
            @com.code4people.jsonrpclib.binding.annotations.Error(code = 33000, exception = IllegalStateException.class, message = "This is custom message")
    )
    public static class Receiver {
        @Bind
        public String method(int i, int j) {
            return "result";
        }

        @Bind
        public String methodWithCustomError() {
            throw new CustomErrorException(
                    -32099,
                    "This is custom error message.",
                    new Object() {
                        public String field = "value";
                    });
        }

        @Bind
        public String methodWithUnexpectedError() {
            throw new RuntimeException("This is unexpected method message");
        }

        @Bind
        @ErrorMapping(
                @com.code4people.jsonrpclib.binding.annotations.Error(code = 32000, exception = RuntimeException.class, message = "This is custom message")
        )
        public String methodWithMappedError1() {
            throw new RuntimeException("This is unexpected method message");
        }

        @Bind
        public String methodWithMappedError2() {
            throw new IllegalStateException("This is unexpected method message");
        }
    }
}
