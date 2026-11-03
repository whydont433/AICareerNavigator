package navigator.trajectory.role;

import navigator.common.Grade;
import navigator.trajectory.api.RoleProfile;
import navigator.trajectory.api.RoleProfileApi;
import navigator.trajectory.external.RoleCatalog;
import navigator.trajectory.external.VacancyReader;

import java.util.List;
import java.util.Optional;

/** Профили ролей (раздел 2 ТЗ): строит по вакансиям и отдаёт готовые. */
public class RoleProfileService implements RoleProfileApi {
    private final RoleProfileRepository repository;
    private final VacancyReader vacancies;
    private final RoleCatalog roles;

    public RoleProfileService(RoleProfileRepository repository, VacancyReader vacancies, RoleCatalog roles) {
        this.repository = repository;
        this.vacancies = vacancies;
        this.roles = roles;
    }

    @Override
    public Optional<RoleProfile> find(String roleId, Grade grade) {
        return repository.find(roleId, grade);
    }

    @Override
    public List<RoleProfile> getAll() {
        return repository.findAll();
    }

    /** Пересчитать профили всех ролей и грейдов. Возвращает, сколько профилей построено. */
    public int rebuild() {
        return 0; // TODO: алгоритм раздела 2
    }
}