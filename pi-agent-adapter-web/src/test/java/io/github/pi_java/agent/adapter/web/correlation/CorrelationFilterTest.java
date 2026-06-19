package io.github.pi_java.agent.adapter.web.correlation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationFilterTest {

    @Test
    void generatedTraceIdIsW3cShapeAndCorrelationHeaderIsPreserved() throws ServletException, IOException {
        CorrelationFilter filter = new CorrelationFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationFilter.CORRELATION_HEADER, "operator-correlation-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(request.getAttribute(CorrelationFilter.TRACE_ATTRIBUTE)).asString().matches("[0-9a-f]{32}");
        assertThat(request.getAttribute(CorrelationFilter.CORRELATION_ATTRIBUTE)).isEqualTo("operator-correlation-1");
        assertThat(response.getHeader(CorrelationFilter.CORRELATION_HEADER)).isEqualTo("operator-correlation-1");
    }
}
