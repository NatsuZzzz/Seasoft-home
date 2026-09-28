package com.store.seasoft.Model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

// Cac enum cua yeu cau tu van (luu dang chuoi trong DB, co CHECK constraint o V3)
public final class ConsultationEnums {

    private ConsultationEnums() {
    }

    public enum ServiceType {
        CORPORATE_WEBSITE, LANDING_PAGE, ECOMMERCE, CUSTOM, MAINTENANCE, OTHER
    }

    public enum BudgetRange {
        UNDER_20M, FROM_20M_TO_50M, FROM_50M_TO_100M, OVER_100M, UNDECIDED
    }

    public enum Status {
        NEW, CONTACTED, QUOTED, WON, LOST;

        // Luong chuyen trang thai hop le. WON la trang thai cuoi (se tao du an).
        // LOST co the mo lai -> CONTACTED.
        private static final Map<Status, Set<Status>> NEXT = Map.of(
                NEW, EnumSet.of(CONTACTED, LOST),
                CONTACTED, EnumSet.of(QUOTED, LOST),
                QUOTED, EnumSet.of(WON, LOST, CONTACTED),
                WON, EnumSet.noneOf(Status.class),
                LOST, EnumSet.of(CONTACTED));

        public boolean canMoveTo(Status next) {
            return this == next || NEXT.get(this).contains(next);
        }
    }
}
