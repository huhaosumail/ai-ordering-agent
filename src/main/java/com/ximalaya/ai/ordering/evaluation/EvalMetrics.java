package com.ximalaya.ai.ordering.evaluation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 检索与分类类评估指标
 */
public final class EvalMetrics {

    private EvalMetrics() {
    }

    /** Top-K 是否至少命中一个期望项 */
    public static boolean hitAtK(List<String> retrieved, List<String> expected, int k) {
        Set<String> top = new HashSet<>(retrieved.subList(0, Math.min(k, retrieved.size())));
        return expected.stream().anyMatch(top::contains);
    }

    /** 期望项在 Top-K 中的召回比例 */
    public static double recallAtK(List<String> retrieved, List<String> expected, int k) {
        if (expected.isEmpty()) {
            return 1.0;
        }
        Set<String> top = new HashSet<>(retrieved.subList(0, Math.min(k, retrieved.size())));
        long hit = expected.stream().filter(top::contains).count();
        return (double) hit / expected.size();
    }

    /** 第一个期望项出现位置的倒数（未命中为 0） */
    public static double reciprocalRank(List<String> retrieved, List<String> expected) {
        for (int i = 0; i < retrieved.size(); i++) {
            if (expected.contains(retrieved.get(i))) {
                return 1.0 / (i + 1);
            }
        }
        return 0.0;
    }
}
