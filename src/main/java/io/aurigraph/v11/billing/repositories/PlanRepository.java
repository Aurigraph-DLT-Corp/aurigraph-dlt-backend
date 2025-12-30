package io.aurigraph.v11.billing.repositories;

import io.aurigraph.v11.billing.models.Plan;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PlanRepository implements PanacheRepository<Plan> {

    public Optional<Plan> findByName(String name) {
        return find("name", name).firstResultOptional();
    }

    public Optional<Plan> findByTierType(Plan.TierType tierType) {
        return find("tierType", tierType).firstResultOptional();
    }

    public List<Plan> findActivePlans() {
        return list("isActive", true);
    }

    public List<Plan> findByTierTypeAndActive(Plan.TierType tierType, boolean isActive) {
        return list("tierType = ?1 and isActive = ?2", tierType, isActive);
    }

    public List<Plan> findAllOrderByPrice() {
        return listAll(io.quarkus.panache.common.Sort.by("monthlyPrice"));
    }

    public long countActivePlans() {
        return count("isActive", true);
    }

    public boolean existsByName(String name) {
        return count("name", name) > 0;
    }

    public List<Plan> searchByNameOrDescription(String query) {
        return find("name like ?1 or description like ?1", "%" + query + "%").list();
    }
}
