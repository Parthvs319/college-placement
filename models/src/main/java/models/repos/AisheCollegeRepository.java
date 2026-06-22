package models.repos;

import helpers.sql.SqlFinder;
import io.ebean.Expr;
import io.ebean.Expression;
import io.ebean.ExpressionList;
import models.sql.AisheCollege;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public enum AisheCollegeRepository {
    INSTANCE;

    private final SqlFinder<Long, AisheCollege> finder = new SqlFinder<>(AisheCollege.class);
    /**
     * Multi-token search: splits the query by whitespace and ORs each token as LIKE %token%.
     * E.g. "Institue Egin" → name LIKE '%Institue%' OR name LIKE '%Egin%'
     * Results where the full query is a substring are ranked first.
     * Optionally filtered by state. Returns at most {@code limit} results.
     */
    public List<AisheCollege> search(String query, String state, int limit) {
        String[] tokens = query.trim().split("\\s+");

        // Collect per-token LIKE expressions (skip single-char tokens)
        List<Expression> likes = new ArrayList<>();
        for (String token : tokens) {
            if (token.length() >= 2) {
                likes.add(Expr.like("name", "%" + token + "%"));
            }
        }
        // Fallback: if all tokens were too short, use the full query
        if (likes.isEmpty()) {
            likes.add(Expr.like("name", "%" + query.trim() + "%"));
        }

        // Combine with OR using Expr.or() (avoids Junction API version differences)
        Expression combined = likes.get(0);
        for (int i = 1; i < likes.size(); i++) {
            combined = Expr.or(combined, likes.get(i));
        }

        ExpressionList<AisheCollege> expr = finder.query().where().add(combined);

        if (state != null && !state.isBlank()) {
            expr = expr.eq("state", state);
        }

        // Fetch a larger pool, then re-rank: exact substring match of full query comes first
        List<AisheCollege> pool = expr.setMaxRows(limit * 3).findList();
        String lq = query.toLowerCase();
        pool.sort((a, b) -> {
            boolean aFull = a.getName() != null && a.getName().toLowerCase().contains(lq);
            boolean bFull = b.getName() != null && b.getName().toLowerCase().contains(lq);
            if (aFull && !bFull) return -1;
            if (!aFull && bFull) return 1;
            return a.getName() == null ? 0 : a.getName().compareToIgnoreCase(b.getName() == null ? "" : b.getName());
        });
        return pool.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * Returns all records with only the 4 fields needed for client-side fuzzy search.
     * Fetches aisheCode, name, state, district — skips heavy columns.
     */
    public List<AisheCollege> findAllSlim() {
        return finder.query()
                .select("aisheCode, name, state, district")
                .orderBy("name asc")
                .findList();
    }

    /** Exact match by AISHE code. */
    public AisheCollege findByAisheCode(String code) {
        return finder.query().where().eq("aisheCode", code).findOne();
    }

    /** Exact match by college name — used for onboarding validation. */
    public AisheCollege findByName(String name) {
        return finder.query().where().eq("name", name).findOne();
    }
}
