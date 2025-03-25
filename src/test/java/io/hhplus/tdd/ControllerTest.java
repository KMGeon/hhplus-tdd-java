package io.hhplus.tdd;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hhplus.tdd.domain.UserPoint;
import io.hhplus.tdd.service.PointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class ControllerTest {
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockBean
    protected PointService pointService;


    protected static final long USER_ID = 1L;


    protected static UserPoint fixtureUser(long amount) {
        return new UserPoint(1L, amount, 99999L);
    }
}
