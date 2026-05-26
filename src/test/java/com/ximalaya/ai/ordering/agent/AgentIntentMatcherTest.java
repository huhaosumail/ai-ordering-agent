package com.ximalaya.ai.ordering.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentIntentMatcherTest {

    private AgentIntentMatcher matcher;

    @BeforeEach
    void setUp() {
        matcher = new AgentIntentMatcher(new OrderIntentParser());
    }

    @Test
    void match_salesRank() {
        assertEquals("query_dishes_sales_rank",
                matcher.match("销量最高的菜有哪些", 1L).orElseThrow().toolName());
    }

    @Test
    void match_semantic() {
        assertEquals("semantic_search_dishes",
                matcher.match("有什么辣的菜推荐", 1L).orElseThrow().toolName());
    }

    @Test
    void match_createOrder() {
        assertEquals("create_order",
                matcher.match("麻婆豆腐三份", 1L).orElseThrow().toolName());
    }
}
