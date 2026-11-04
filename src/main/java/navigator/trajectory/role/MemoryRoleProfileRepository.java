package navigator.trajectory.role;

import navigator.common.Grade;
import navigator.trajectory.api.RoleProfile;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class MemoryRoleProfileRepository implements RoleProfileRepository{
    private List<RoleProfile> profiles = List.of();

    @Override
    public Optional<RoleProfile> find(String roleId, Grade grade) {
        for (RoleProfile p : profiles) {
            if (p.roleId().equals(roleId) && p.grade() == grade) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<RoleProfile> findAll() {
        return profiles;
    }


    @Override
    public void replaceAll(List<RoleProfile> newProfiles) {
        Set<String> seen = new HashSet<>();
        for (RoleProfile p : newProfiles) {
            String key = p.roleId() + "/" + p.grade();
            if (seen.contains(key)) {
                throw new IllegalStateException("Два профиля " + p.roleId() + " " + p.grade());
            }
            seen.add(key);
        }
        profiles = List.copyOf(newProfiles);
    }
}
