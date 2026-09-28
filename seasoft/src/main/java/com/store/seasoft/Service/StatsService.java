package com.store.seasoft.Service;

import com.store.seasoft.Dto.AdminDtos.Stats;
import com.store.seasoft.Dto.AdminDtos.WeekCount;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// So lieu tong quan cho MANAGER/ADMIN. Dem bang SQL group by cho nhanh, khong nap entity.
@Service
@RequiredArgsConstructor
public class StatsService {

    private static final int WEEKS = 8;

    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public Stats overview() {
        Map<String, Long> leadsByStatus = countBy(
                "select status, count(*) from consultation_requests group by status",
                List.of("NEW", "CONTACTED", "QUOTED", "WON", "LOST"));
        long won = leadsByStatus.get("WON");
        long lost = leadsByStatus.get("LOST");

        Map<String, Long> projectsByStatus = countBy("select status, count(*) from projects group by status",
                List.of("PLANNING", "IN_PROGRESS", "ON_HOLD", "COMPLETED", "CANCELLED"));

        return new Stats(
                leadsByStatus.values().stream().mapToLong(Long::longValue).sum(),
                one("select count(*) from consultation_requests where created_at >= now() - interval '30 days'"),
                leadsByStatus,
                countBy("select service_type, count(*) from consultation_requests group by service_type",
                        List.of("CORPORATE_WEBSITE", "LANDING_PAGE", "ECOMMERCE", "CUSTOM", "MAINTENANCE", "OTHER")),
                leadsPerWeek(),
                won + lost == 0 ? null : Math.round(won * 1000.0 / (won + lost)) / 10.0,
                projectsByStatus.values().stream().mapToLong(Long::longValue).sum(),
                projectsByStatus,
                avgProgress(),
                one("select count(*) from projects where due_date < current_date "
                        + "and status not in ('COMPLETED', 'CANCELLED')"),
                countBy("select r.code, count(u.id) from roles r left join users u on u.role_id = r.id group by r.code",
                        List.of("CUSTOMER", "STAFF", "MANAGER", "ADMIN")));
    }

    // So lead moi theo tung tuan (thu Hai dau tuan), du 8 tuan gan nhat ke ca tuan = 0
    private List<WeekCount> leadsPerWeek() {
        return jdbc.query("""
                        select w::date as week_start, count(c.id) as cnt
                        from generate_series(date_trunc('week', now()) - interval '%d weeks',
                                             date_trunc('week', now()), interval '1 week') as w
                        left join consultation_requests c
                               on date_trunc('week', c.created_at) = w
                        group by w order by w""".formatted(WEEKS - 1),
                (rs, i) -> new WeekCount(rs.getObject("week_start", LocalDate.class), rs.getLong("cnt")));
    }

    // Tien do trung binh (% giai doan DONE) cua cac du an dang chay
    private Double avgProgress() {
        return jdbc.queryForObject("""
                select round(avg(pct)::numeric, 1)::float8 from (
                  select p.id, 100.0 * count(m.id) filter (where m.status = 'DONE') / nullif(count(m.id), 0) as pct
                  from projects p left join project_milestones m on m.project_id = p.id
                  where p.status in ('PLANNING', 'IN_PROGRESS', 'ON_HOLD')
                  group by p.id) t""", Double.class);
    }

    // Tra ve map co du moi khoa (khoa khong co du lieu = 0), giu thu tu
    private Map<String, Long> countBy(String sql, List<String> keys) {
        Map<String, Long> result = new LinkedHashMap<>();
        keys.forEach(k -> result.put(k, 0L));
        jdbc.query(sql, rs -> {
            result.put(rs.getString(1), rs.getLong(2));
        });
        return result;
    }

    private long one(String sql) {
        Long v = jdbc.queryForObject(sql, Long.class);
        return v == null ? 0 : v;
    }
}
