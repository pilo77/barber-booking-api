package com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import com.villamil.barberbooking.application.dto.response.*;
import com.villamil.barberbooking.application.port.out.MarketplaceRepositoryPort;
import com.villamil.barberbooking.domain.exception.PublicResourceNotFoundException;
import com.villamil.barberbooking.domain.model.*;

@Component
public class MarketplacePersistenceAdapter implements MarketplaceRepositoryPort {
    private static final String BOOKABLE = """
            EXISTS (SELECT 1 FROM services s WHERE s.company_id=b.company_id AND s.active AND s.visible_for_online_booking)
            AND EXISTS (SELECT 1 FROM barbers br JOIN barber_working_hours wh ON wh.barber_id=br.id
                AND wh.company_id=br.company_id AND wh.branch_id=br.branch_id
                WHERE br.company_id=b.company_id AND br.branch_id=b.id AND br.active AND br.active_for_online_booking AND wh.active)
            """;
    private final JdbcTemplate jdbc;
    public MarketplacePersistenceAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public Optional<MarketplaceBranchProfile> find(Long companyId, Long branchId) {
        return jdbc.query("SELECT * FROM marketplace_branch_profiles WHERE company_id=? AND branch_id=?",
                this::profile, companyId, branchId).stream().findFirst();
    }

    @Override public Optional<MarketplaceBranchProfile> findPublishedCompanyProfile(Long companyId) {
        return jdbc.query("""
                SELECT p.* FROM marketplace_branch_profiles p JOIN branches b ON b.id=p.branch_id AND b.company_id=p.company_id
                JOIN companies c ON c.id=p.company_id
                WHERE p.company_id=? AND p.publication_state='PUBLISHED' AND b.active AND c.active ORDER BY p.branch_id LIMIT 1
                """, this::profile, companyId).stream().findFirst();
    }

    @Override public MarketplaceBranchProfile lockOrCreate(Long companyId, Long branchId) {
        jdbc.update("""
                INSERT INTO marketplace_branch_profiles(company_id, branch_id)
                SELECT b.company_id, b.id FROM branches b JOIN companies c ON c.id=b.company_id
                WHERE b.company_id=? AND b.id=? AND b.active AND c.active ON CONFLICT (branch_id) DO NOTHING
                """, companyId, branchId);
        return jdbc.query("SELECT * FROM marketplace_branch_profiles WHERE company_id=? AND branch_id=? FOR UPDATE",
                this::profile, companyId, branchId).stream().findFirst()
                .orElseThrow(() -> new PublicResourceNotFoundException("Publication not found"));
    }

    @Override public Optional<MarketplaceBranchProfile> lockByBranch(Long branchId) {
        return jdbc.query("SELECT * FROM marketplace_branch_profiles WHERE branch_id=? FOR UPDATE",
                this::profile, branchId).stream().findFirst();
    }

    @Override public void save(MarketplaceBranchProfile p, MarketplacePublicationState previous, Long actorId, Long actorCompanyId) {
        int changed = jdbc.update("""
                UPDATE marketplace_branch_profiles SET publication_state=?, city=?, sector=?, address=?, description=?,
                    contact_phone=?, cover_image_url=?, submitted_at=?, reviewed_at=?, reviewed_by=?, review_reason=?, updated_at=?
                WHERE company_id=? AND branch_id=?
                """, p.publicationState().name(), p.city(), p.sector(), p.address(), p.description(), p.contactPhone(),
                p.coverImageUrl(), timestamp(p.submittedAt()), timestamp(p.reviewedAt()), p.reviewedBy(), p.reviewReason(),
                timestamp(p.updatedAt()), p.companyId(), p.branchId());
        if (changed != 1) throw new PublicResourceNotFoundException("Publication not found");
        jdbc.update("""
                INSERT INTO marketplace_publication_events(company_id, branch_id, actor_id, actor_company_id, from_state, to_state, reason)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, p.companyId(), p.branchId(), actorId, actorCompanyId, previous.name(), p.publicationState().name(), p.reviewReason());
    }

    @Override public boolean hasBookableResources(Long companyId, Long branchId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM branches b JOIN companies c ON c.id=b.company_id
                WHERE b.company_id=? AND b.id=? AND b.active AND c.active AND
                """ + BOOKABLE + ")", Boolean.class, companyId, branchId));
    }

    @Override public List<MarketplaceSubmissionResponse> pending() {
        return jdbc.query("""
                SELECT p.*, c.name AS company_name, b.name AS branch_name
                FROM marketplace_branch_profiles p JOIN companies c ON c.id=p.company_id
                JOIN branches b ON b.id=p.branch_id AND b.company_id=p.company_id
                WHERE p.publication_state='PENDING_REVIEW'
                ORDER BY p.submitted_at, p.branch_id LIMIT 100
                """, (rs, row) -> new MarketplaceSubmissionResponse(rs.getLong("company_id"), rs.getLong("branch_id"),
                rs.getString("company_name"), rs.getString("branch_name"), MarketplaceProfileResponse.from(profile(rs, row))));
    }

    @Override public List<PublicMarketplaceItemResponse> search(String city, String service, String query, int offset, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT c.slug AS company_slug, b.slug AS branch_slug, c.name AS company_name, b.name AS branch_name,
                    p.city, p.sector, p.address, p.description, p.contact_phone, p.cover_image_url,
                    (SELECT min(s.price) FROM services s WHERE s.company_id=c.id AND s.active AND s.visible_for_online_booking) AS starting_price
                FROM marketplace_branch_profiles p JOIN companies c ON c.id=p.company_id
                JOIN branches b ON b.id=p.branch_id AND b.company_id=p.company_id
                WHERE p.publication_state='PUBLISHED' AND c.active AND b.active AND
                """);
        sql.append(BOOKABLE);
        List<Object> parameters = new ArrayList<>();
        if (city != null) { sql.append(" AND lower(p.city)=lower(?)"); parameters.add(city); }
        if (service != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM services sf WHERE sf.company_id=c.id AND sf.active AND sf.visible_for_online_booking AND lower(sf.name) LIKE lower(?) ESCAPE '!')");
            parameters.add(contains(service));
        }
        if (query != null) {
            sql.append(" AND lower(concat_ws(' ',c.name,b.name,p.city,p.sector,p.description)) LIKE lower(?) ESCAPE '!'");
            parameters.add(contains(query));
        }
        sql.append(" ORDER BY lower(c.name), lower(b.name), c.id, b.id LIMIT ? OFFSET ?");
        parameters.add(limit); parameters.add(offset);
        return jdbc.query(sql.toString(), (rs, row) -> new PublicMarketplaceItemResponse(
                rs.getString("company_slug"), rs.getString("branch_slug"), rs.getString("company_name"), rs.getString("branch_name"),
                rs.getString("city"), rs.getString("sector"), rs.getString("address"), rs.getString("description"),
                rs.getString("contact_phone"), rs.getString("cover_image_url"), rs.getBigDecimal("starting_price")), parameters.toArray());
    }

    @Override public boolean isBranchPublic(Long companyId, Long branchId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM branches b JOIN companies c ON c.id=b.company_id
                    LEFT JOIN marketplace_branch_profiles p ON p.branch_id=b.id AND p.company_id=b.company_id
                    WHERE b.company_id=? AND b.id=? AND b.active AND c.active
                    AND (p.branch_id IS NULL OR p.publication_state='PUBLISHED'))
                """, Boolean.class, companyId, branchId));
    }

    @Override public boolean isCompanyPublic(Long companyId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM branches b JOIN companies c ON c.id=b.company_id
                    LEFT JOIN marketplace_branch_profiles p ON p.branch_id=b.id AND p.company_id=b.company_id
                    WHERE b.company_id=? AND b.active AND c.active
                    AND (p.branch_id IS NULL OR p.publication_state='PUBLISHED'))
                """, Boolean.class, companyId));
    }

    private MarketplaceBranchProfile profile(ResultSet rs, int row) throws SQLException {
        return new MarketplaceBranchProfile(rs.getLong("company_id"), rs.getLong("branch_id"),
                MarketplacePublicationState.valueOf(rs.getString("publication_state")), rs.getString("city"), rs.getString("sector"),
                rs.getString("address"), rs.getString("description"), rs.getString("contact_phone"), rs.getString("cover_image_url"),
                instant(rs.getTimestamp("submitted_at")), instant(rs.getTimestamp("reviewed_at")),
                rs.getObject("reviewed_by", Long.class), rs.getString("review_reason"), instant(rs.getTimestamp("updated_at")));
    }
    private Timestamp timestamp(Instant value) { return value == null ? null : Timestamp.from(value); }
    private Instant instant(Timestamp value) { return value == null ? null : value.toInstant(); }
    private String contains(String value) { return "%" + value.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%"; }
}
