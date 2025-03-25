package io.hhplus.tdd.controller.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hhplus.tdd.MockApiCaller;
import io.hhplus.tdd.domain.UserPoint;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PointMockApiCaller extends MockApiCaller {

    public PointMockApiCaller(MockMvc mockMvc, ObjectMapper objectMapper) {
        super(mockMvc, objectMapper);
    }

    public UserPoint point(long id, int expectStatus) throws Exception {
        MockHttpServletRequestBuilder builder = get("/point/{id}", id)
                .contentType(MediaType.APPLICATION_JSON);

        return objectMapper.readValue(mockMvc.perform(builder)
                .andExpect(status().is(expectStatus))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), new TypeReference<UserPoint>() {});
    }

    public UserPoint charge(long id, long amount, int expectStatus) throws Exception {
        MockHttpServletRequestBuilder builder = patch("/point/{id}/charge", id)
                .content(objectMapper.writeValueAsString(amount))
                .contentType(MediaType.APPLICATION_JSON);

        return objectMapper.readValue(mockMvc.perform(builder)
                .andExpect(status().is(expectStatus))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), new TypeReference<UserPoint>() {});
    }
}
