package com.ximalaya.ai.ordering.agent;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 从用户自然语言解析下单意图（模拟模式与评估共用）
 */
@Component
public class OrderIntentParser {

    public Optional<Map<String, Object>> tryParse(String userMsg, Long userId) {
        if (!userMsg.contains("份") && !userMsg.contains("我要") && !userMsg.contains("下单")
                && !userMsg.contains("来一") && !userMsg.contains("点") && !userMsg.contains("来")) {
            return Optional.empty();
        }
        List<Map<String, Object>> items = parseItems(userMsg);
        if (items.isEmpty()) {
            return Optional.empty();
        }
        Map<String, Object> params = new HashMap<>();
        params.put("items", items);
        params.put("userId", userId != null ? userId : 1L);
        return Optional.of(params);
    }

    public List<Map<String, Object>> parseItems(String userMsg) {
        List<Map<String, Object>> items = new ArrayList<>();
        String qty = "[一二两三四五六七八九十\\d]+";

        java.util.regex.Pattern qtyFirst = java.util.regex.Pattern.compile(
                "(" + qty + ")份\\s*([^，,。！!？?\\s谢谢]+)");
        java.util.regex.Matcher m1 = qtyFirst.matcher(userMsg);
        while (m1.find()) {
            addItem(items, m1.group(2), m1.group(1));
        }

        java.util.regex.Pattern nameFirst = java.util.regex.Pattern.compile(
                "([\\u4e00-\\u9fa5A-Za-z0-9]{2,}?)\\s*(" + qty + ")份");
        java.util.regex.Matcher m2 = nameFirst.matcher(userMsg);
        while (m2.find()) {
            addItem(items, m2.group(1), m2.group(2));
        }
        return items;
    }

    private void addItem(List<Map<String, Object>> items, String rawName, String rawQty) {
        String dishName = normalizeDishName(rawName);
        if (dishName.isEmpty()) {
            return;
        }
        int quantity = parseQuantity(rawQty);
        boolean duplicate = items.stream()
                .anyMatch(i -> dishName.equals(String.valueOf(i.get("name"))));
        if (!duplicate) {
            items.add(Map.of("name", dishName, "quantity", quantity));
        }
    }

    private String normalizeDishName(String raw) {
        if (raw == null) {
            return "";
        }
        String name = raw.trim()
                .replaceAll("^(我要|来|点|订|给我|帮我|想要|需要)", "")
                .trim();
        return name.length() >= 2 ? name : "";
    }

    private int parseQuantity(String raw) {
        if (raw.matches("\\d+")) {
            return Integer.parseInt(raw);
        }
        return switch (raw) {
            case "一" -> 1;
            case "二", "两" -> 2;
            case "三" -> 3;
            case "四" -> 4;
            case "五" -> 5;
            case "六" -> 6;
            case "七" -> 7;
            case "八" -> 8;
            case "九" -> 9;
            case "十" -> 10;
            default -> 1;
        };
    }
}
