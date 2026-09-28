package com.store.seasoft.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Gioi han so lan goi theo khoa (vd: IP) trong 1 cua so thoi gian truot.
// Luu trong RAM -> du cho 1 instance; chay nhieu instance thi can Redis.
public class RateLimiter {

    private final int maxRequests;
    private final Duration window;
    private final Clock clock;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public RateLimiter(int maxRequests, Duration window, Clock clock) {
        this.maxRequests = maxRequests;
        this.window = window;
        this.clock = clock;
    }

    // true = cho phep va ghi nhan 1 lan goi
    public boolean tryAcquire(String key) {
        long now = clock.millis();
        long from = now - window.toMillis();
        Deque<Long> q = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && q.peekFirst() <= from) {
                q.pollFirst();
            }
            if (q.size() >= maxRequests) {
                return false;
            }
            q.addLast(now);
        }
        // Don khoa rong de map khong phinh mai
        if (hits.size() > 10_000) {
            hits.entrySet().removeIf(e -> {
                synchronized (e.getValue()) {
                    return e.getValue().isEmpty() || e.getValue().peekLast() <= from;
                }
            });
        }
        return true;
    }
}
